package com.esde.companion.data.thor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.HandlerThread
import com.esde.companion.data.debug.DebugFileLogger
import com.esde.companion.data.thor.accessibility.CompanionAccessibilityService
import com.esde.companion.domain.thor.RefreshRateDecision
import com.esde.companion.domain.thor.RefreshRateDecisionResult
import com.esde.companion.domain.thor.RefreshRateReactorState
import com.esde.companion.domain.thor.THOR_IGNORED_SYSTEM_PACKAGES
import com.esde.companion.domain.usecase.ObserveAutoFpsEnabledUseCase
import com.esde.companion.domain.usecase.ObserveAutoFpsTriggerPackagesUseCase
import com.esde.companion.domain.usecase.SetAutoFpsEnabledUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * App-scoped reactive piece for Thor Settings > Auto FPS Mode - full port of Asgard's
 * `AutoFpsForegroundReactor` mechanism (accessibility-based foreground-app detection, not a
 * shortcut off `AppState` - see CLAUDE.md for why), with the same opt-in-gated divergence as
 * [LidWakeGuardCoordinator]: the `ACTION_SCREEN_OFF` receiver is only registered while
 * [ObserveAutoFpsEnabledUseCase] reports the setting is on. Whether a refresh-rate write
 * actually lands additionally depends on the user having granted the accessibility service (for
 * the foreground-package feed to fire at all) and [RefreshRateController.canWrite] (the
 * privileged Settings service being present on this firmware) - both fail soft.
 *
 * The accessibility listeners themselves ([CompanionAccessibilityService.addForegroundPackageListener]/
 * [CompanionAccessibilityService.addDisconnectListener]) are registered exactly once in [start],
 * not inside the enabled/disabled toggle handling - that service's listener lists have no remove
 * API, so re-registering them on every enable would accumulate duplicate listeners across
 * repeated toggles. [featureEnabled] is what actually gates whether [onForegroundPackage] does
 * anything, matching how [RefreshRateDecision.decide] already treats a disabled feature as
 * [RefreshRateDecisionResult.IGNORE].
 *
 * The Settings UI prevents *turning on* this setting while the accessibility service isn't
 * granted, but a user can revoke that grant afterward from system Settings while the feature is
 * on - the disconnect listener reacts by persisting the setting back to disabled via
 * [setAutoFpsEnabled], same auto-disable shape as [LidWakeGuardCoordinator].
 */
class AutoFpsCoordinator(
    private val observeAutoFpsEnabled: ObserveAutoFpsEnabledUseCase,
    private val setAutoFpsEnabled: SetAutoFpsEnabledUseCase,
    private val observeAutoFpsTriggerPackages: ObserveAutoFpsTriggerPackagesUseCase,
    private val debugFileLogger: DebugFileLogger,
) {
    private val workerThread = HandlerThread("AutoFpsCoordinator").apply { start() }
    private val handler = Handler(workerThread.looper)

    @Volatile
    private var featureEnabled = false

    @Volatile
    private var triggerPackages: Set<String> = emptySet()

    private var highRefreshActive = false
    private var pendingRevert: Runnable? = null
    private var screenOffReceiverRegistered = false

    private val screenOffReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) {
                if (intent.action == Intent.ACTION_SCREEN_OFF) handler.post(::onDisconnect)
            }
        }

    fun start(
        context: Context,
        applicationScope: CoroutineScope,
    ) {
        val appContext = context.applicationContext
        CompanionAccessibilityService.addForegroundPackageListener { packageName ->
            handler.post { onForegroundPackage(packageName) }
        }
        CompanionAccessibilityService.addDisconnectListener {
            handler.post {
                onDisconnect()
                if (featureEnabled) applicationScope.launch { setAutoFpsEnabled(false) }
            }
        }
        applicationScope.launch {
            observeAutoFpsTriggerPackages().collect { triggerPackages = it }
        }
        applicationScope.launch {
            observeAutoFpsEnabled().distinctUntilChanged().collect { enabled ->
                featureEnabled = enabled
                if (enabled) armScreenOffReceiver(appContext) else disarmScreenOffReceiver(appContext)
            }
        }
    }

    private fun armScreenOffReceiver(context: Context) {
        if (screenOffReceiverRegistered) return
        screenOffReceiverRegistered = true
        context.registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }

    private fun disarmScreenOffReceiver(context: Context) {
        if (!screenOffReceiverRegistered) return
        screenOffReceiverRegistered = false
        runCatching { context.unregisterReceiver(screenOffReceiver) }
        onDisconnect()
    }

    private fun onForegroundPackage(packageName: String) {
        val revertPending = pendingRevert != null
        val reactorState =
            RefreshRateReactorState(highRefreshActive = highRefreshActive, revertPending = revertPending)
        when (
            RefreshRateDecision.decide(
                packageName = packageName,
                ignoredPackages = THOR_IGNORED_SYSTEM_PACKAGES,
                featureEnabled = featureEnabled,
                triggerPackages = triggerPackages,
                state = reactorState,
            )
        ) {
            RefreshRateDecisionResult.IGNORE -> Unit

            RefreshRateDecisionResult.ENTER_HIGH_REFRESH -> {
                cancelPendingRevert()
                if (!highRefreshActive) {
                    val succeeded = RefreshRateController.setHighRefreshRate()
                    logSwitch(succeeded, entering = true, packageName)
                    highRefreshActive = true
                }
            }

            RefreshRateDecisionResult.SCHEDULE_REVERT_CHECK -> {
                val runnable = Runnable { reconcile(packageName) }
                pendingRevert = runnable
                handler.postDelayed(runnable, REVERT_DEBOUNCE_MS)
            }

            RefreshRateDecisionResult.NO_OP -> Unit
        }
    }

    private fun reconcile(lastSeenPackage: String) {
        pendingRevert = null
        val current = CompanionAccessibilityService.currentForegroundPackage()
        if (current != null && current in triggerPackages) return // false alarm - still in a trigger app
        val succeeded = RefreshRateController.setNormalRefreshRate()
        logSwitch(succeeded, entering = false, current ?: lastSeenPackage)
        highRefreshActive = false
    }

    /** Same three outcomes as Asgard's `RefreshRateAction` (`ENTERED_HIGH_REFRESH`/
     * `RETURNED_NORMAL_REFRESH`/`WRITE_FAILED`), logged via [DebugFileLogger.logInfo] instead of
     * a separate per-feature event store - see CLAUDE.md's Debug Logging note. */
    private fun logSwitch(
        succeeded: Boolean,
        entering: Boolean,
        packageName: String,
    ) {
        val outcome =
            when {
                !succeeded -> "FAILED to write refresh rate"
                entering -> "entered high refresh"
                else -> "returned to normal refresh"
            }
        debugFileLogger.logInfo(LOG_TAG, "$outcome ($packageName)")
    }

    private fun cancelPendingRevert() {
        pendingRevert?.let { handler.removeCallbacks(it) }
        pendingRevert = null
    }

    private fun onDisconnect() {
        cancelPendingRevert()
        if (highRefreshActive) {
            RefreshRateController.setNormalRefreshRate()
            highRefreshActive = false
        }
    }

    // Companion's own package is deliberately absent from THOR_IGNORED_SYSTEM_PACKAGES - see
    // ThorIgnoredSystemPackages' kdoc. It's moot for this coordinator specifically:
    // CompanionAccessibilityService now filters to Display.DEFAULT_DISPLAY only, so a
    // window-state-changed event for Companion's own package (which runs on the secondary/bottom
    // display) never reaches onForegroundPackage in the first place.
    private companion object {
        const val REVERT_DEBOUNCE_MS = 700L
        const val LOG_TAG = "AutoFPS"
    }
}

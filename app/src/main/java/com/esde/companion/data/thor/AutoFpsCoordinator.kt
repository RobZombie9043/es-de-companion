package com.esde.companion.data.thor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.HandlerThread
import com.esde.companion.data.thor.accessibility.CompanionAccessibilityService
import com.esde.companion.domain.thor.RefreshRateDecision
import com.esde.companion.domain.thor.RefreshRateDecisionResult
import com.esde.companion.domain.thor.RefreshRateReactorState
import com.esde.companion.domain.usecase.ObserveAutoFpsEnabledUseCase
import com.esde.companion.domain.usecase.ObserveAutoFpsTriggerPackagesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * App-scoped reactive piece for Thor Settings > Auto FPS Mode - full port of Asgard's
 * `AutoFpsForegroundReactor` mechanism (accessibility-based foreground-app detection, not a
 * shortcut off `AppState` - see CLAUDE.md for why), with the same opt-in-gated divergence as
 * [LidWakeGuardCoordinator]: the foreground-package listener and the `ACTION_SCREEN_OFF`
 * receiver are only installed while [ObserveAutoFpsEnabledUseCase] reports the setting is on.
 * Whether a refresh-rate write actually lands additionally depends on the user having granted
 * the accessibility service (for the foreground-package feed to fire at all) and
 * [RefreshRateController.canWrite] (the privileged Settings service being present on this
 * firmware) - both fail soft, so neither needs to gate installation itself.
 */
class AutoFpsCoordinator(
    private val observeAutoFpsEnabled: ObserveAutoFpsEnabledUseCase,
    private val observeAutoFpsTriggerPackages: ObserveAutoFpsTriggerPackagesUseCase,
) {
    private val workerThread = HandlerThread("AutoFpsCoordinator").apply { start() }
    private val handler = Handler(workerThread.looper)

    @Volatile
    private var featureEnabled = false

    @Volatile
    private var triggerPackages: Set<String> = emptySet()

    private var highRefreshActive = false
    private var pendingRevert: Runnable? = null
    private var installed = false

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
        applicationScope.launch {
            observeAutoFpsTriggerPackages().collect { triggerPackages = it }
        }
        applicationScope.launch {
            observeAutoFpsEnabled().distinctUntilChanged().collect { enabled ->
                featureEnabled = enabled
                if (enabled) install(appContext) else uninstall(appContext)
            }
        }
    }

    private fun install(context: Context) {
        if (installed) return
        installed = true
        CompanionAccessibilityService.addForegroundPackageListener { packageName ->
            handler.post { onForegroundPackage(packageName) }
        }
        CompanionAccessibilityService.addDisconnectListener {
            handler.post(::onDisconnect)
        }
        context.registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }

    private fun uninstall(context: Context) {
        if (!installed) return
        installed = false
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
                ignoredPackages = IGNORED_PACKAGES,
                featureEnabled = featureEnabled,
                triggerPackages = triggerPackages,
                state = reactorState,
            )
        ) {
            RefreshRateDecisionResult.IGNORE -> Unit

            RefreshRateDecisionResult.ENTER_HIGH_REFRESH -> {
                cancelPendingRevert()
                if (!highRefreshActive) {
                    RefreshRateController.setHighRefreshRate()
                    highRefreshActive = true
                }
            }

            RefreshRateDecisionResult.SCHEDULE_REVERT_CHECK -> {
                val runnable = Runnable { reconcile() }
                pendingRevert = runnable
                handler.postDelayed(runnable, REVERT_DEBOUNCE_MS)
            }

            RefreshRateDecisionResult.NO_OP -> Unit
        }
    }

    private fun reconcile() {
        pendingRevert = null
        val current = CompanionAccessibilityService.currentForegroundPackage()
        if (current != null && current in triggerPackages) return // false alarm - still in a trigger app
        RefreshRateController.setNormalRefreshRate()
        highRefreshActive = false
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

    private companion object {
        const val REVERT_DEBOUNCE_MS = 700L

        /**
         * Matches Asgard's `AutoFpsForegroundReactor.IGNORED_PACKAGES`. Deliberately does NOT
         * include this app's own package: since this service doesn't filter by display id (see
         * [CompanionAccessibilityService]'s kdoc), a window-state-changed event for Companion's
         * own package is the genuine "the user backed out of an App-Drawer-launched trigger app"
         * signal for that use case - ignoring it would break that revert path entirely. The
         * known tradeoff (an internal Companion dialog/overlay briefly stealing "foreground"
         * while a trigger app is still genuinely displayed on the other display) is exactly the
         * on-device validation item called out in CLAUDE.md, not something this ignore-list can
         * resolve for both cases at once.
         */
        val IGNORED_PACKAGES =
            setOf(
                "com.odin.gameassistant",
                "com.android.systemui",
                "com.android.launcher3",
                "android",
            )
    }
}

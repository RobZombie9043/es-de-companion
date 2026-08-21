package com.esde.companion.data.thor

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.HandlerThread
import com.esde.companion.data.debug.DebugFileLogger
import com.esde.companion.data.thor.accessibility.CompanionAccessibilityService
import com.esde.companion.domain.thor.THOR_IGNORED_SYSTEM_PACKAGES
import com.esde.companion.domain.thor.TaskKillerCapabilities
import com.esde.companion.domain.thor.TaskKillerDecision
import com.esde.companion.domain.thor.TaskKillerDecisionResult
import com.esde.companion.domain.thor.TaskKillerForegroundContext
import com.esde.companion.domain.thor.TaskKillerHoldEvent
import com.esde.companion.domain.usecase.ObserveTaskKillerEnabledUseCase
import com.esde.companion.domain.usecase.ObserveTaskKillerExcludedPackagesUseCase
import com.esde.companion.domain.usecase.SetTaskKillerEnabledUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * App-scoped reactive piece for Thor Settings > Task Killer - full port of Asgard's
 * `TaskKillerReactor` mechanism. Same opt-in-gated/single-registration shape as
 * [AutoFpsCoordinator]/[LidWakeGuardCoordinator]: [CompanionAccessibilityService]'s BACK
 * press/release listeners and disconnect listener are registered exactly once in [start] (that
 * service's listener lists have no remove API); [featureEnabled] is what actually gates whether
 * [onBackPressed]/[onBackReleased] do anything, matching [TaskKillerDecision.decide]'s own
 * `DISABLED` short-circuit.
 *
 * The actual force-stop decision is deliberately release-gated, not press-plus-delay: on the
 * Thor the physical BACK button also presents as a joystick/gamepad HID device, and SDL-based
 * apps (ES-DE, RetroArch) read that raw controller state directly, bypassing Android's KeyEvent
 * pipeline entirely - so nothing this app does can suppress a still-held button from leaking into
 * whatever app gains focus once the held-over one is killed. Acting only after release means the
 * whole press/release cycle plays out over the original app, and the button is already idle by
 * the time anything else gains focus. The confirmation *vibration*, by contrast, is scheduled on
 * press: it never touches input dispatch, so there's no leak risk in giving the user that
 * feedback the moment the hold threshold elapses rather than waiting for release too.
 *
 * The foreground app to act on is resolved fresh via root shell ([TaskKillerShell]) at *press*
 * time, not release: a window-state-changed event only fires when a window's state actually
 * changes, not on a pure focus shift between two already-running windows on different displays,
 * so it can't be tracked from accessibility events. Press time (not release) matters too: an
 * ordinary app sitting at its root/home screen can finish exiting to the previous task on the
 * same physical BACK release - delivered to it normally, since nothing here consumes the
 * KeyEvent - faster than the two root-shell round trips take, so a release-time resolution risks
 * reading the *next* app instead of the one actually held.
 */
class TaskKillerCoordinator(
    private val observeTaskKillerEnabled: ObserveTaskKillerEnabledUseCase,
    private val setTaskKillerEnabled: SetTaskKillerEnabledUseCase,
    private val observeTaskKillerExcludedPackages: ObserveTaskKillerExcludedPackagesUseCase,
    private val debugFileLogger: DebugFileLogger,
) {
    private val workerThread = HandlerThread("TaskKillerCoordinator").apply { start() }
    private val handler = Handler(workerThread.looper)

    @Volatile
    private var featureEnabled = false

    // SYSTEM_PACKAGES-equivalent (THOR_IGNORED_SYSTEM_PACKAGES) can never be un-protected; the
    // user-editable exclude-apps list layers on top - see ThorSettingsRepository's kdoc.
    @Volatile
    private var excludedPackages: Set<String> = THOR_IGNORED_SYSTEM_PACKAGES

    private var pendingVibration: Runnable? = null

    // Resolved on press, not on release (see class doc) - only ever touched on `handler`'s
    // thread, same as everything else here, so no synchronization needed.
    private var pressTimeForegroundPackage: String? = null

    fun start(
        context: Context,
        applicationScope: CoroutineScope,
    ) {
        val appContext = context.applicationContext
        CompanionAccessibilityService.addBackPressListener {
            handler.post { onBackPressed(appContext) }
        }
        CompanionAccessibilityService.addBackReleaseListener { heldMs ->
            handler.post { onBackReleased(appContext, heldMs) }
        }
        CompanionAccessibilityService.addDisconnectListener {
            if (featureEnabled) applicationScope.launch { setTaskKillerEnabled(false) }
        }
        applicationScope.launch {
            observeTaskKillerExcludedPackages().collect { userExcluded ->
                excludedPackages = THOR_IGNORED_SYSTEM_PACKAGES + userExcluded
            }
        }
        applicationScope.launch {
            observeTaskKillerEnabled().distinctUntilChanged().collect { enabled -> featureEnabled = enabled }
        }
    }

    private fun onBackPressed(context: Context) {
        cancelPendingVibration()
        pressTimeForegroundPackage = null
        if (!featureEnabled) return
        // Short, light tap - just enough to confirm the hold registered, not a full alert buzz.
        val runnable =
            Runnable {
                vibrate(context, durationMs = VIBRATION_DURATION_MS, amplitude = VIBRATION_AMPLITUDE)
            }
        pendingVibration = runnable
        handler.postDelayed(runnable, HOLD_DURATION_MS)
        pressTimeForegroundPackage = TaskKillerShell.resolveFocusedForegroundPackage()
    }

    private fun cancelPendingVibration() {
        pendingVibration?.let { handler.removeCallbacks(it) }
        pendingVibration = null
    }

    private fun onBackReleased(
        context: Context,
        heldMs: Long,
    ) {
        cancelPendingVibration()
        val foreground = pressTimeForegroundPackage
        val hold = TaskKillerHoldEvent(heldMs = heldMs, thresholdMs = HOLD_DURATION_MS)
        val capabilities =
            TaskKillerCapabilities(
                featureEnabled = featureEnabled,
                privilegedAvailable = RefreshRateController.canWrite(),
            )
        val foregroundContext =
            TaskKillerForegroundContext(
                foregroundPackage = foreground,
                homePackage = resolveHomePackage(context),
                foregroundPinned = isLockTaskModeActive(context),
            )
        val decision =
            TaskKillerDecision.decide(
                hold = hold,
                capabilities = capabilities,
                foreground = foregroundContext,
                ownPackage = context.packageName,
                excludedPackages = excludedPackages,
            )
        logDecision(decision, foreground)
        if (decision != TaskKillerDecisionResult.FORCE_STOP) return
        // decide() only returns FORCE_STOP once foregroundPackage is confirmed non-null.
        val pkg = checkNotNull(foreground)
        val stopped = TaskKillerShell.forceStop(pkg)
        debugFileLogger.logInfo(LOG_TAG, "${if (stopped) "force-stopped" else "FAILED to force-stop"} $pkg")
        // force-stop alone kills the process but leaves a stale card in Recents - only worth
        // attempting cleanup once the kill itself is confirmed.
        if (stopped) cleanUpRecents(pkg)
    }

    /** Same skip/force-stop outcomes as Asgard's `TaskKillerAction`, logged via
     * [DebugFileLogger.logInfo] instead of a separate per-feature event store - see CLAUDE.md's
     * Debug Logging note. `DISABLED`/`SHORT_PRESS` aren't logged, matching Asgard. */
    private fun logDecision(
        decision: TaskKillerDecisionResult,
        foreground: String?,
    ) {
        val message =
            when (decision) {
                TaskKillerDecisionResult.DISABLED, TaskKillerDecisionResult.SHORT_PRESS -> return
                TaskKillerDecisionResult.NO_FOREGROUND -> "skipped - no foreground package resolved"
                TaskKillerDecisionResult.PROTECTED -> "skipped - $foreground is protected"
                TaskKillerDecisionResult.PRIVILEGED_UNAVAILABLE ->
                    "skipped - privileged settings service unavailable ($foreground)"
                TaskKillerDecisionResult.FORCE_STOP -> return
            }
        debugFileLogger.logInfo(LOG_TAG, message)
    }

    /**
     * A single delayed re-lookup covers Recents state not having caught up with the force-stop
     * yet. A package can have more than one task, so every match gets removed, not just the
     * first.
     */
    private fun cleanUpRecents(packageName: String) {
        var taskIds = TaskKillerShell.findRecentTaskIds(packageName)
        if (taskIds.isEmpty()) {
            Thread.sleep(RECENTS_LOOKUP_RETRY_DELAY_MS)
            taskIds = TaskKillerShell.findRecentTaskIds(packageName)
        }
        if (taskIds.isEmpty()) {
            debugFileLogger.logInfo(LOG_TAG, "no Recents task found for $packageName after force-stop")
            return
        }
        taskIds.forEach { taskId ->
            val removed = TaskKillerShell.removeRecentsTask(taskId)
            val outcome = if (removed) "removed" else "FAILED to remove"
            debugFileLogger.logInfo(LOG_TAG, "$outcome Recents task $taskId ($packageName)")
        }
    }

    /** The device's current default home/launcher package - always protected, whichever app it is. */
    private fun resolveHomePackage(context: Context): String? =
        runCatching {
            val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            context.packageManager
                .resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo
                ?.packageName
        }.getOrNull()

    /**
     * Screen pinning (Settings > Security > "App pinning", or an app calling `startLockTask()`)
     * is the standard, documented way a user or app declares "don't let this get closed" - a
     * reasonable, robust proxy for the same intent as this OEM's own Recents-card lock toggle,
     * whose backing storage isn't publicly queryable.
     */
    private fun isLockTaskModeActive(context: Context): Boolean =
        runCatching {
            val activityManager = context.getSystemService(ActivityManager::class.java)
            activityManager?.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
        }.getOrDefault(false)

    private companion object {
        // Comfortably above ViewConfiguration.getLongPressTimeout() (~500ms) - this triggers an
        // irreversible action (unsaved game progress lost), so it needs more margin against an
        // accidental hold than an ordinary long-press gesture would. Matches Asgard's
        // TaskKillerPrefs.DEFAULT_HOLD_DURATION_MS; not user-configurable in either app.
        const val HOLD_DURATION_MS = 800L

        // Covers Recents state propagation, not a general retry loop.
        const val RECENTS_LOOKUP_RETRY_DELAY_MS = 150L

        const val VIBRATION_DURATION_MS = 50L
        const val VIBRATION_AMPLITUDE = 130

        const val LOG_TAG = "Killer"
    }
}

package com.esde.companion.data.thor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import com.esde.companion.data.debug.DebugFileLogger
import com.esde.companion.data.thor.accessibility.CompanionAccessibilityService
import com.esde.companion.data.thor.sensor.HallSensorReader
import com.esde.companion.domain.model.HallSensorCalibration
import com.esde.companion.domain.thor.LidWakeGuardDecision
import com.esde.companion.domain.usecase.ObserveHallSensorCalibrationUseCase
import com.esde.companion.domain.usecase.ObserveLidWakeGuardEnabledUseCase
import com.esde.companion.domain.usecase.SetLidWakeGuardEnabledUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * App-scoped reactive piece for Thor Settings > Lid Wake Guard - the deliberate architectural
 * divergence from Asgard's always-on `LidWakeGuardReactor.install()` (see CLAUDE.md): the
 * `ACTION_SCREEN_ON`/`ACTION_SCREEN_OFF` [android.content.BroadcastReceiver] here is only
 * registered while [ObserveLidWakeGuardEnabledUseCase] reports the setting is on, and torn down
 * the moment it isn't - constructed once at [com.esde.companion.AppContainer] construction (cheap,
 * same as [com.esde.companion.data.storage.SelfHealingDirectoryWatcher] instances), started via
 * [start].
 *
 * The Settings UI (`ThorSettingsContent`) prevents *turning on* this setting while the
 * accessibility service isn't granted, but a user can still revoke that grant afterward from
 * system Settings while the feature is on - [CompanionAccessibilityService.addDisconnectListener]
 * fires when that happens (the OS unbinds the service), and this coordinator reacts by persisting
 * the setting back to disabled via [setLidWakeGuardEnabled], which the existing
 * [observeLidWakeGuardEnabled] collector below then disarms in response - so the same single
 * enabled-flow drives both directions, and the Settings UI reflects the auto-disable the next
 * time it reloads that flow. Registered once in [start], not tied to arm/disarm, since
 * [CompanionAccessibilityService]'s listener lists have no matching remove API - registering it
 * repeatedly would accumulate duplicate listeners.
 */
class LidWakeGuardCoordinator(
    private val context: Context,
    private val observeLidWakeGuardEnabled: ObserveLidWakeGuardEnabledUseCase,
    private val setLidWakeGuardEnabled: SetLidWakeGuardEnabledUseCase,
    private val observeHallSensorCalibration: ObserveHallSensorCalibrationUseCase,
    private val debugFileLogger: DebugFileLogger,
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val hallSensorReader = HallSensorReader(sensorManager)
    private val workerThread = HandlerThread("LidWakeGuardCoordinator").apply { start() }
    private val handler = Handler(workerThread.looper)

    @Volatile
    private var calibration: HallSensorCalibration = HallSensorCalibration.Uncalibrated

    @Volatile
    private var armedElapsedMs: Long = 0

    private var receiverRegistered = false

    private val screenReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                receiverContext: Context,
                intent: Intent,
            ) {
                if (intent.action == Intent.ACTION_SCREEN_ON) handler.post(::onScreenOn)
            }
        }

    @Volatile
    private var guardEnabled = false

    fun start(applicationScope: CoroutineScope) {
        applicationScope.launch {
            observeHallSensorCalibration().collect { calibration = it }
        }
        applicationScope.launch {
            observeLidWakeGuardEnabled().distinctUntilChanged().collect { enabled ->
                guardEnabled = enabled
                if (enabled) arm() else disarm()
            }
        }
        CompanionAccessibilityService.addDisconnectListener {
            if (guardEnabled) applicationScope.launch { setLidWakeGuardEnabled(false) }
        }
    }

    private fun arm() {
        if (receiverRegistered) return
        receiverRegistered = true
        armedElapsedMs = SystemClock.elapsedRealtime()
        context.registerReceiver(screenReceiver, IntentFilter(Intent.ACTION_SCREEN_ON))
    }

    private fun disarm() {
        if (!receiverRegistered) return
        receiverRegistered = false
        context.unregisterReceiver(screenReceiver)
    }

    // Guard-clause style, same reasoning as RefreshRateDecision.decide - suppressed rather
    // than restructured into nested if-lets, which would only hurt readability here.
    @Suppress("ReturnCount")
    private fun onScreenOn() {
        // Ignore a screen-on immediately after arming - mirrors Asgard's STARTUP_GRACE_MS,
        // avoiding a spurious lock right as the feature is turned on.
        if (SystemClock.elapsedRealtime() - armedElapsedMs < STARTUP_GRACE_MS) return
        val current = calibration
        if (!current.isCalibrated) return
        val sensorType = current.sensorType ?: return
        val closedValue = current.closedValue ?: return
        val openValue = current.openValue ?: return

        hallSensorReader.readOnce(sensorType, SENSOR_READ_TIMEOUT_MS, handler) { reading ->
            onSensorRead(reading, closedValue, openValue)
        }
    }

    /**
     * Logs the same four outcomes as Asgard's `LidWakeGuardReactor.onScreenOn`/`GuardAction`
     * (timeout/open/locked/lock-failed), via [DebugFileLogger.logInfo] rather than a separate
     * per-feature event store - see CLAUDE.md's Debug Logging note.
     */
    @Suppress("ReturnCount")
    private fun onSensorRead(
        reading: Float?,
        closedValue: Float,
        openValue: Float,
    ) {
        if (reading == null) {
            debugFileLogger.logInfo(LOG_TAG, "sensor read timed out, ignoring screen-on")
            return
        }
        val shouldLock =
            LidWakeGuardDecision.shouldLock(
                guardEnabled = true,
                isCalibrated = true,
                sensorReadsClosed = LidWakeGuardDecision.isReadingClosed(reading, closedValue, openValue),
            )
        if (!shouldLock) {
            debugFileLogger.logInfo(LOG_TAG, "lid reads open (reading=$reading), ignoring screen-on")
            return
        }
        if (CompanionAccessibilityService.requestLock()) {
            debugFileLogger.logInfo(LOG_TAG, "locked screen (reading=$reading)")
        } else {
            debugFileLogger.logInfo(LOG_TAG, "lock failed - accessibility service not bound (reading=$reading)")
        }
    }

    private companion object {
        const val STARTUP_GRACE_MS = 5000L

        /** Matches Asgard's `GuardPrefs.DEFAULT_TIMEOUT_MS`. */
        const val SENSOR_READ_TIMEOUT_MS = 2000L

        const val LOG_TAG = "Guard"
    }
}

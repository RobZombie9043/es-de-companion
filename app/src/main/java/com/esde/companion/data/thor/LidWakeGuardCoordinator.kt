package com.esde.companion.data.thor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import com.esde.companion.data.thor.accessibility.CompanionAccessibilityService
import com.esde.companion.data.thor.sensor.HallSensorReader
import com.esde.companion.domain.model.HallSensorCalibration
import com.esde.companion.domain.thor.LidWakeGuardDecision
import com.esde.companion.domain.usecase.ObserveHallSensorCalibrationUseCase
import com.esde.companion.domain.usecase.ObserveLidWakeGuardEnabledUseCase
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
 * Whether a screen-on actually results in a lock additionally requires the user to have
 * separately granted the accessibility service (see [ThorAccessibilityPermission]/Thor Settings'
 * own status UI) - [CompanionAccessibilityService.requestLock] simply returns `false` and does
 * nothing if the service isn't bound, so this coordinator doesn't need to track that grant state
 * itself to stay opt-in: an unwanted `BroadcastReceiver` registration is the only thing gated
 * purely by the DataStore toggle, and it does nothing observable without the accessibility grant.
 */
class LidWakeGuardCoordinator(
    private val context: Context,
    private val observeLidWakeGuardEnabled: ObserveLidWakeGuardEnabledUseCase,
    private val observeHallSensorCalibration: ObserveHallSensorCalibrationUseCase,
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

    fun start(applicationScope: CoroutineScope) {
        applicationScope.launch {
            observeHallSensorCalibration().collect { calibration = it }
        }
        applicationScope.launch {
            observeLidWakeGuardEnabled().distinctUntilChanged().collect { enabled ->
                if (enabled) arm() else disarm()
            }
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
            val readsClosed = reading != null && LidWakeGuardDecision.isReadingClosed(reading, closedValue, openValue)
            val shouldLock =
                LidWakeGuardDecision.shouldLock(
                    guardEnabled = true,
                    isCalibrated = true,
                    sensorReadsClosed = readsClosed,
                )
            if (shouldLock) {
                CompanionAccessibilityService.requestLock()
            }
        }
    }

    private companion object {
        const val STARTUP_GRACE_MS = 5000L

        /** Matches Asgard's `GuardPrefs.DEFAULT_TIMEOUT_MS`. */
        const val SENSOR_READ_TIMEOUT_MS = 2000L
    }
}

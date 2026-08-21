package com.esde.companion.data.thor.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.SystemClock

/**
 * One-shot, timeout-bounded read of a single sensor value - ported from Asgard's
 * `HallSensorReader`, used for both Lid Wake Guard's stray-wake re-check and its
 * calibrate-on-first-view flow.
 */
class HallSensorReader(
    private val sensorManager: SensorManager,
) {
    /** Callback receives the first reported value, or null on timeout/missing sensor. */
    fun readOnce(
        sensorType: Int,
        timeoutMs: Long,
        handler: Handler,
        callback: (Float?) -> Unit,
    ) {
        val sensor = sensorManager.getDefaultSensor(sensorType)
        if (sensor == null) {
            callback(null)
            return
        }

        val token = Any()
        var delivered = false

        val listener =
            object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    if (delivered) return
                    delivered = true
                    sensorManager.unregisterListener(this)
                    handler.removeCallbacksAndMessages(token)
                    callback(event.values.getOrNull(0))
                }

                override fun onAccuracyChanged(
                    sensor: Sensor?,
                    accuracy: Int,
                ) = Unit
            }

        handler.postAtTime(
            {
                if (!delivered) {
                    delivered = true
                    sensorManager.unregisterListener(listener)
                    callback(null)
                }
            },
            token,
            SystemClock.uptimeMillis() + timeoutMs,
        )

        // A saved sensor type that's permission-gated can throw here - this may run from an
        // accessibility-service callback, so a crash here would take down more than just this
        // one read.
        val registered =
            runCatching {
                sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_FASTEST, handler)
            }.getOrDefault(false)
        if (!registered && !delivered) {
            delivered = true
            handler.removeCallbacksAndMessages(token)
            callback(null)
        }
    }
}

package com.esde.companion.data.thor.sensor

import android.hardware.Sensor
import android.hardware.SensorManager

/**
 * Ported from Asgard's `HallSensorPicker`.
 *
 * 2 = a "Hall" sensor whose name marks it as the wake-capable variant - this is the one
 *     that keeps reporting values even once the device is fully asleep, which is exactly
 *     when the guard needs a reading right after the lid closes.
 * 1 = any other "Hall" sensor, e.g. a "Non-wakeup" twin of the same chip - these commonly
 *     stop delivering events during deep sleep, which is why they aren't preferred.
 * 0 = doesn't look like a hall sensor at all.
 */
fun hallSensorScore(sensor: Sensor): Int {
    val name = sensor.name.lowercase()
    if (!name.contains("hall")) return 0
    val isNonWakeup = name.contains("non-wakeup") || name.contains("non wakeup") || name.contains("nonwakeup")
    return if (!isNonWakeup && name.contains("wakeup")) 2 else 1
}

/** The best-scoring hall-like sensor on the device, or null if none look like one. */
fun autoPickHallSensor(sensorManager: SensorManager): Sensor? =
    sensorManager
        .getSensorList(Sensor.TYPE_ALL)
        .filter { hallSensorScore(it) > 0 }
        .maxByOrNull { hallSensorScore(it) }

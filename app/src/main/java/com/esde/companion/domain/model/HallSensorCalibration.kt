package com.esde.companion.domain.model

/**
 * Lid Wake Guard's calibration state: which Hall-effect sensor to read, and what value it
 * reports when the lid is closed vs. open. [sensorType] is an `android.hardware.Sensor`
 * type constant, but stored as a plain `Int` here (not the Android `Sensor` type itself) so
 * this stays representable in pure domain code - see CLAUDE.md's domain-purity rule.
 */
data class HallSensorCalibration(
    val sensorType: Int? = null,
    val sensorName: String = "",
    val closedValue: Float? = null,
    val openValue: Float? = null,
) {
    val isCalibrated: Boolean
        get() = sensorType != null && closedValue != null && openValue != null

    companion object {
        val Uncalibrated = HallSensorCalibration()
    }
}

package com.esde.companion.domain.model

/** Coarse battery level for the SystemStatus/ClockAndSystemStatus FABs - see [batteryTierFor]. */
enum class BatteryTier { Low, Medium, High, Full }

data class BatteryStatus(val tier: BatteryTier, val isCharging: Boolean)

/**
 * wifiConnected/bluetoothConnected: false means "hide this icon" - covers not connected,
 * radio off, no hardware, and (bluetooth only) BLUETOOTH_CONNECT not granted, all as one
 * outcome. There's no separate "disabled" icon variant for either - see SystemStatusFabContent,
 * which simply omits an icon from its Row when its flag is false. Battery has no such hidden
 * state; it's unconditionally shown.
 */
data class SystemStatus(
    val battery: BatteryStatus,
    val wifiConnected: Boolean,
    val bluetoothConnected: Boolean,
)

private const val BATTERY_FULL_THRESHOLD_PERCENT = 85
private const val BATTERY_HIGH_THRESHOLD_PERCENT = 50
private const val BATTERY_MEDIUM_THRESHOLD_PERCENT = 20

/** Pure, unit-testable without Android - see BatteryTierTest. */
fun batteryTierFor(percent: Int): BatteryTier =
    when {
        percent >= BATTERY_FULL_THRESHOLD_PERCENT -> BatteryTier.Full
        percent >= BATTERY_HIGH_THRESHOLD_PERCENT -> BatteryTier.High
        percent >= BATTERY_MEDIUM_THRESHOLD_PERCENT -> BatteryTier.Medium
        else -> BatteryTier.Low
    }

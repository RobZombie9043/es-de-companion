package com.esde.companion.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery1Bar
import androidx.compose.material.icons.filled.Battery3Bar
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.esde.companion.domain.model.BatteryStatus
import com.esde.companion.domain.model.BatteryTier
import com.esde.companion.domain.model.SystemStatus

private val STATUS_ICON_SIZE = 20.dp

/**
 * The status-icon row for [SystemStatusFabContent][com.esde.companion.ui.SystemStatusFabContent]/
 * [ClockAndSystemStatusFabContent][com.esde.companion.ui.ClockAndSystemStatusFabContent] - split
 * out of `SystemStatusFabContent.kt` into its own file once adding per-icon fade animations
 * pushed that file over detekt's per-file function-count threshold.
 */
@Composable
internal fun RowScope.SystemStatusIcons(systemStatus: SystemStatus) {
    StatusIcon(visible = systemStatus.wifiConnected, icon = Icons.Filled.Wifi, contentDescription = "Wifi connected")
    StatusIcon(
        visible = systemStatus.bluetoothConnected,
        icon = Icons.Filled.Bluetooth,
        contentDescription = "Bluetooth connected",
    )
    Icon(
        imageVector = batteryIconFor(systemStatus.battery),
        contentDescription = batteryContentDescription(systemStatus.battery),
        modifier = Modifier.size(STATUS_ICON_SIZE),
    )
}

/**
 * A standalone composable (not inlined into [SystemStatusIcons]'s own body) so its
 * `AnimatedVisibility` call has no ambient `RowScope` receiver in lexical scope - called from
 * inside a `RowScope` extension function, `AnimatedVisibility` there is ambiguous between the
 * plain top-level overload and `RowScope.AnimatedVisibility` (both satisfiable via that
 * receiver), the same compile error already hit and fixed this way for a `ColumnScope` case in
 * `GameGuidesBrowserScreen.kt`'s `DownloadButton`.
 */
@Composable
private fun StatusIcon(
    visible: Boolean,
    icon: ImageVector,
    contentDescription: String,
) {
    // Wifi/Bluetooth previously popped in/out instantly (no "off" icon variant exists - see
    // SystemStatus's kdoc - so this is the only visual feedback for a connectivity change).
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Icon(imageVector = icon, contentDescription = contentDescription, modifier = Modifier.size(STATUS_ICON_SIZE))
    }
}

// Compose's Material Icons Extended has only one charging glyph (BatteryChargingFull) - no
// tier-aware BatteryCharging20/50/80 equivalents like the plain (non-charging) Battery1Bar/
// 3Bar/5Bar/Full set has. So charging always shows this one icon regardless of actual tier,
// a deliberate simplicity-over-precision tradeoff (confirmed with the user) rather than
// pairing a separate charging indicator with the real tier icon.
private fun batteryIconFor(battery: BatteryStatus): ImageVector =
    if (battery.isCharging) {
        Icons.Filled.BatteryChargingFull
    } else {
        when (battery.tier) {
            BatteryTier.Low -> Icons.Filled.Battery1Bar
            BatteryTier.Medium -> Icons.Filled.Battery3Bar
            BatteryTier.High -> Icons.Filled.Battery5Bar
            BatteryTier.Full -> Icons.Filled.BatteryFull
        }
    }

private fun batteryContentDescription(battery: BatteryStatus): String =
    if (battery.isCharging) "Battery charging" else "Battery: ${battery.tier}"

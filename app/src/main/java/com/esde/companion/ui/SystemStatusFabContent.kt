package com.esde.companion.ui

import android.content.Context
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery1Bar
import androidx.compose.material.icons.filled.Battery3Bar
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.esde.companion.data.systemstatus.BluetoothConnectPermission
import com.esde.companion.domain.model.BatteryStatus
import com.esde.companion.domain.model.BatteryTier
import com.esde.companion.domain.model.FabPosition
import com.esde.companion.domain.model.SystemStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Date

/**
 * Bundles position/visible/overlayOpacityPercent/onWidthMeasured/onClick - shared by all three
 * Clock/SystemStatus/ClockAndSystemStatusFabContent composables below - into one value.
 * These are BoxScope extension functions, whose receiver counts toward this project's
 * LongParameterList limit alongside their explicit parameters, so keeping the always-together
 * set bundled (rather than five separate parameters) is what keeps them under threshold -
 * same reasoning as SelfHealConfig/BackupRepositories elsewhere. [onClick] opens the same main
 * menu the Settings FAB does (see MainActivity's openSettingsMenuRequest) - these FABs are
 * otherwise purely informational, so a tap has nothing more specific to do.
 */
internal data class WideFabContext(
    val position: FabPosition,
    val visible: Boolean,
    val overlayOpacityPercent: Int,
    val onWidthMeasured: (Int) -> Unit,
    val onClick: () -> Unit,
)

private fun BoxScope.wideFabModifier(context: WideFabContext): Modifier =
    Modifier.align(context.position.toAlignment()).padding(CORNER_BUTTON_EDGE_PADDING)
        .onSizeChanged { context.onWidthMeasured(it.width) }

/**
 * Content for a corner assigned [com.esde.companion.domain.model.FabType.Clock] - a
 * fixed-height, content-width FAB showing the current time, ticking once a second. Uses the
 * system's own 12h/24h time-format preference (DateFormat.getTimeFormat) rather than a
 * hardcoded format. Tapping it opens the main menu, same as the Settings FAB (see
 * MainActivity's openSettingsMenuRequest) - it's otherwise purely informational, so a tap has
 * nothing more specific to do.
 */
@Composable
internal fun BoxScope.ClockFabContent(context: WideFabContext) {
    if (!context.visible) {
        SideEffect { context.onWidthMeasured(0) }
        return
    }
    val timeText = rememberTickingTimeText()
    WideCornerFab(
        onClick = context.onClick,
        opacityPercent = context.overlayOpacityPercent,
        modifier = wideFabModifier(context),
    ) {
        Text(text = timeText, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * Content for a corner assigned [com.esde.companion.domain.model.FabType.SystemStatus] - a
 * fixed-height, content-width FAB showing a row of status icons (Wifi, Bluetooth, Battery).
 * Wifi/Bluetooth are omitted entirely when not connected (no "off" icon variant - see
 * SystemStatus's kdoc); Battery always shows. Tapping it opens the main menu, same as the
 * Settings FAB (see MainActivity's openSettingsMenuRequest) - it's otherwise purely
 * informational, so a tap has nothing more specific to do. See
 * [rememberBluetoothPermissionPrompt] for the one-shot BLUETOOTH_CONNECT request this FAB
 * triggers the first time it's shown - if dismissed, the recovery path is Settings > UI
 * Settings > Floating Action Buttons (see UISettingsContent's BluetoothPermissionRow), not
 * tapping the FAB itself.
 */
@Composable
internal fun BoxScope.SystemStatusFabContent(
    context: WideFabContext,
    systemStatus: SystemStatus,
    bluetoothPermission: BluetoothPermissionState,
) {
    if (!context.visible) {
        SideEffect { context.onWidthMeasured(0) }
        return
    }
    rememberBluetoothPermissionPrompt(bluetoothPermission)
    WideCornerFab(
        onClick = context.onClick,
        opacityPercent = context.overlayOpacityPercent,
        modifier = wideFabModifier(context),
    ) {
        SystemStatusIcons(systemStatus)
    }
}

/**
 * Content for a corner assigned [com.esde.companion.domain.model.FabType.ClockAndSystemStatus]
 * - both [ClockFabContent] and [SystemStatusFabContent] combined in one FAB.
 */
@Composable
internal fun BoxScope.ClockAndSystemStatusFabContent(
    context: WideFabContext,
    systemStatus: SystemStatus,
    bluetoothPermission: BluetoothPermissionState,
) {
    if (!context.visible) {
        SideEffect { context.onWidthMeasured(0) }
        return
    }
    rememberBluetoothPermissionPrompt(bluetoothPermission)
    val timeText = rememberTickingTimeText()
    WideCornerFab(
        onClick = context.onClick,
        opacityPercent = context.overlayOpacityPercent,
        modifier = wideFabModifier(context),
    ) {
        Text(text = timeText, style = MaterialTheme.typography.bodyMedium)
        SystemStatusIcons(systemStatus)
    }
}

private const val CLOCK_TICK_INTERVAL_MILLIS = 1_000L

@Composable
private fun rememberTickingTimeText(): String {
    val context = LocalContext.current
    var timeText by remember { mutableStateOf(currentTimeText(context)) }
    LaunchedEffect(Unit) {
        while (isActive) {
            timeText = currentTimeText(context)
            delay(CLOCK_TICK_INTERVAL_MILLIS)
        }
    }
    return timeText
}

private fun currentTimeText(context: Context): String = DateFormat.getTimeFormat(context).format(Date())

private val STATUS_ICON_SIZE = 20.dp

@Composable
private fun RowScope.SystemStatusIcons(systemStatus: SystemStatus) {
    if (systemStatus.wifiConnected) {
        Icon(
            imageVector = Icons.Filled.Wifi,
            contentDescription = "Wifi connected",
            modifier = Modifier.size(STATUS_ICON_SIZE),
        )
    }
    if (systemStatus.bluetoothConnected) {
        Icon(
            imageVector = Icons.Filled.Bluetooth,
            contentDescription = "Bluetooth connected",
            modifier = Modifier.size(STATUS_ICON_SIZE),
        )
    }
    Icon(
        imageVector = batteryIconFor(systemStatus.battery),
        contentDescription = batteryContentDescription(systemStatus.battery),
        modifier = Modifier.size(STATUS_ICON_SIZE),
    )
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

/**
 * The first runtime-dangerous-permission request flow in this codebase (see
 * BluetoothConnectPermission's kdoc). Auto-shows a one-shot rationale dialog the first time a
 * SystemStatus-family FAB is visible with BLUETOOTH_CONNECT ungranted, then never nags again
 * automatically - [BluetoothPermissionState.requested] is persisted regardless of the user's
 * choice. The ON_RESUME recheck (same idiom as OnboardingScreen's AllFilesAccessPermission
 * check) picks up a grant made later via system Settings.
 *
 * Not wired to the FAB's own onClick - that opens the main menu instead (same as the Settings
 * FAB, see MainActivity's openSettingsMenuRequest), so a user who dismissed the one-shot
 * auto-prompt ("Not Now") recovers via Settings > UI Settings > Floating Action Buttons (see
 * UISettingsContent's BluetoothPermissionRow), not by tapping the FAB again.
 */
@Composable
private fun rememberBluetoothPermissionPrompt(state: BluetoothPermissionState) {
    val context = LocalContext.current
    var showRationale by remember { mutableStateOf(false) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            state.onRecheck()
        }

    LaunchedEffect(state.requested) {
        val hardwarePresent = BluetoothConnectPermission.hasBluetoothHardware(context)
        val granted = BluetoothConnectPermission.isGranted(context)
        if (!state.requested && hardwarePresent && !granted) {
            showRationale = true
        }
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = {
                showRationale = false
                state.onRequested()
            },
            confirmButton = {
                TextButton(onClick = {
                    showRationale = false
                    state.onRequested()
                    launcher.launch(BluetoothConnectPermission.PERMISSION)
                }) { Text("Allow") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRationale = false
                    state.onRequested()
                }) { Text("Not Now") }
            },
            title = { Text("Allow Bluetooth access?") },
            text = {
                Text("Bluetooth access is required to check whether Bluetooth is enabled and show its current status.")
            },
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnRecheck = rememberUpdatedState(state.onRecheck)
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    currentOnRecheck.value()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

/**
 * Bundles the SystemStatus-family Bluetooth-permission callbacks into one value - keeps
 * SystemStatusFabContent/ClockAndSystemStatusFabContent within this project's
 * LongParameterList limit, same reasoning as SelfHealConfig/BackupRepositories elsewhere.
 */
internal data class BluetoothPermissionState(
    val requested: Boolean,
    val onRequested: () -> Unit,
    val onRecheck: () -> Unit,
)

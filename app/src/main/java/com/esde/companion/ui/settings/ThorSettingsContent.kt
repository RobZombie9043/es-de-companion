// This file covers four separate Thor Settings panels (Lid Wake Guard, Auto FPS, Task Killer,
// Volume Sync), each needing its own composables/dropdowns - same TooManyFunctions reasoning as
// ThorSettingsRepository's suppression: more panels naturally means more small functions, not
// something to force into fewer larger ones.
@file:Suppress("TooManyFunctions")

package com.esde.companion.ui.settings

import android.content.Context
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.DevicesOther
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.ShieldMoon
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.esde.companion.data.thor.ThorAccessibilityPermission
import com.esde.companion.data.thor.sensor.HallSensorReader
import com.esde.companion.data.thor.sensor.autoPickHallSensor
import com.esde.companion.domain.model.HallSensorCalibration
import com.esde.companion.domain.model.TaskKillerTarget
import com.esde.companion.domain.model.VolumeSyncMode
import com.esde.companion.ui.SegmentedButtonLabel

/** Bundles Lid Wake Guard's settings-panel state into one [ThorSettingsContent] parameter,
 * keeping that function's parameter count under detekt's LongParameterList threshold - same
 * reasoning as [DimAmountControl]. */
internal data class LidWakeGuardSettingsState(
    val enabled: Boolean,
    val accessibilityGranted: Boolean,
    val calibration: HallSensorCalibration,
    val onEnabledChanged: (Boolean) -> Unit,
    val onCalibrationChanged: (HallSensorCalibration) -> Unit,
)

/** Same bundling reasoning as [LidWakeGuardSettingsState], for Auto FPS Mode. */
internal data class AutoFpsSettingsState(
    val enabled: Boolean,
    val accessibilityGranted: Boolean,
    val privilegedServiceAvailable: Boolean,
    val onEnabledChanged: (Boolean) -> Unit,
    val onManageTriggerAppsClick: () -> Unit,
)

/** Same bundling reasoning as [LidWakeGuardSettingsState], for Task Killer. */
internal data class TaskKillerSettingsState(
    val enabled: Boolean,
    val accessibilityGranted: Boolean,
    val privilegedServiceAvailable: Boolean,
    val target: TaskKillerTarget,
    val onEnabledChanged: (Boolean) -> Unit,
    val onTargetChanged: (TaskKillerTarget) -> Unit,
    val onManageExcludedAppsClick: () -> Unit,
)

/** Same bundling reasoning as [LidWakeGuardSettingsState], for Volume Sync. */
internal data class VolumeSyncSettingsState(
    val enabled: Boolean,
    val accessibilityGranted: Boolean,
    val privilegedServiceAvailable: Boolean,
    val secondarySettingPresent: Boolean,
    val mode: VolumeSyncMode,
    val onEnabledChanged: (Boolean) -> Unit,
    val onModeChanged: (VolumeSyncMode) -> Unit,
)

/**
 * Thor Settings (Ayn Thor-only, see CLAUDE.md) - Auto FPS Mode, Lid Wake Guard, Task Killer,
 * and Volume Control. Only reachable when [com.esde.companion.data.thor.isAynThorDevice] is true
 * (see `SettingsMenuHome`'s filtering in `LongPressSettingsMenu`); the `when` branch routing
 * here still exists unconditionally, same forcing-function reasoning as every other
 * [SettingsCategory].
 */
@Composable
internal fun ThorSettingsContent(
    lidWakeGuard: LidWakeGuardSettingsState,
    autoFps: AutoFpsSettingsState,
    taskKiller: TaskKillerSettingsState,
    volumeSync: VolumeSyncSettingsState,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        AutoFpsSetting(autoFps)
        LidWakeGuardSetting(lidWakeGuard)
        TaskKillerSetting(taskKiller)
        VolumeSyncSetting(volumeSync)
    }
}

/**
 * The Hall sensor is a plain on/off switch, so there's nothing to physically calibrate:
 * whichever value is live *right now* is necessarily "open" (the screen has to be on, and the
 * user has to be looking at this panel, for this composable to be running at all - the lid
 * can't be closed), and the other of the two binary values is "closed". Ported from Asgard's
 * `LidWakeGuardDetails` - see CLAUDE.md for why this app doesn't also port Asgard's manual
 * `SensorPickerActivity` fallback screen.
 */
@Composable
private fun LidWakeGuardSetting(state: LidWakeGuardSettingsState) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    var autoCalibrating by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        autoCalibrateHallSensor(
            context = context,
            alreadyCalibrated = state.calibration.isCalibrated,
            onCalibratingChanged = { autoCalibrating = it },
            onCalibrationChanged = state.onCalibrationChanged,
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingsLabel(icon = Icons.Filled.ShieldMoon, text = "Lid Wake Guard")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Re-lock the screen if a stray button press wakes it while the lid is closed",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(16.dp))
                Switch(
                    checked = state.enabled,
                    // Turning ON requires the accessibility grant (the lock action needs it) -
                    // turning OFF always stays available, including right after a revoke this
                    // switch hasn't yet reflected. See LidWakeGuardCoordinator's kdoc for the
                    // auto-disable path when the grant is revoked while this is already on.
                    enabled = state.enabled || state.accessibilityGranted,
                    onCheckedChange = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        state.onEnabledChanged(it)
                    },
                )
            }
            val statusText = calibrationStatusText(state.calibration, autoCalibrating)
            Text(text = statusText, style = MaterialTheme.typography.bodySmall)
            if (!state.accessibilityGranted) {
                AccessibilityGrantRow(
                    text = "Accessibility service not granted - required to lock the screen",
                    onClick = { context.startActivity(ThorAccessibilityPermission.requestIntent(context)) },
                )
            }
        }
    }
}

private fun calibrationStatusText(
    calibration: HallSensorCalibration,
    autoCalibrating: Boolean,
): String =
    when {
        calibration.isCalibrated -> "Sensor calibrated (${calibration.sensorName})"
        autoCalibrating -> "Checking sensor…"
        else -> "No Hall sensor found on this device"
    }

/**
 * See [LidWakeGuardSetting]'s kdoc for why a live read is always treated as "open". Split out
 * of that composable purely to stay under detekt's LongMethod threshold once the panel's own
 * UI joined the calibration logic in one function.
 */
private suspend fun autoCalibrateHallSensor(
    context: Context,
    alreadyCalibrated: Boolean,
    onCalibratingChanged: (Boolean) -> Unit,
    onCalibrationChanged: (HallSensorCalibration) -> Unit,
) {
    if (alreadyCalibrated) return
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val sensor = autoPickHallSensor(sensorManager) ?: return
    onCalibratingChanged(true)
    HallSensorReader(sensorManager).readOnce(
        sensorType = sensor.type,
        timeoutMs = HALL_SENSOR_CALIBRATION_TIMEOUT_MS,
        handler = Handler(Looper.getMainLooper()),
    ) { value ->
        if (value != null) {
            onCalibrationChanged(
                HallSensorCalibration(
                    sensorType = sensor.type,
                    sensorName = sensor.name,
                    openValue = value,
                    closedValue = 1f - value,
                ),
            )
        }
        onCalibratingChanged(false)
    }
}

@Composable
private fun AutoFpsSetting(state: AutoFpsSettingsState) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingsLabel(icon = Icons.Filled.Speed, text = "Auto FPS Mode")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Boost the top screen to 120Hz automatically for selected apps",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(16.dp))
                Switch(
                    checked = state.enabled,
                    // Same accessibility-grant gating as Lid Wake Guard's switch - see that
                    // one's comment.
                    enabled = state.enabled || state.accessibilityGranted,
                    onCheckedChange = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        state.onEnabledChanged(it)
                    },
                )
            }
            if (!state.accessibilityGranted) {
                AccessibilityGrantRow(
                    text = "Accessibility service not granted - required to detect the foreground app",
                    onClick = { context.startActivity(ThorAccessibilityPermission.requestIntent(context)) },
                )
            }
            if (!state.privilegedServiceAvailable) {
                Text(
                    text = "Privileged settings service not found on this firmware - Auto FPS Mode is unavailable",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            ThorAppPickerNavigationRow(label = "Choose Trigger Apps", onClick = state.onManageTriggerAppsClick)
        }
    }
}

@Composable
private fun TaskKillerSetting(state: TaskKillerSettingsState) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingsLabel(icon = Icons.Filled.PowerSettingsNew, text = "Task Killer")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Hold BACK to force-quit",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(16.dp))
                Switch(
                    checked = state.enabled,
                    enabled = state.enabled || state.accessibilityGranted,
                    onCheckedChange = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        state.onEnabledChanged(it)
                    },
                )
            }
            if (!state.accessibilityGranted) {
                AccessibilityGrantRow(
                    text = "Accessibility service not granted - required to detect the BACK button",
                    onClick = { context.startActivity(ThorAccessibilityPermission.requestIntent(context)) },
                )
            }
            if (!state.privilegedServiceAvailable) {
                Text(
                    text = "Privileged settings service not found on this firmware - Task Killer is unavailable",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "Force-Quit Target", style = MaterialTheme.typography.labelLarge)
                TaskKillerTargetDropdown(selected = state.target, onSelected = state.onTargetChanged)
            }
            ThorAppPickerNavigationRow(label = "Choose Excluded Apps", onClick = state.onManageExcludedAppsClick)
        }
    }
}

/** Dropdown variant of a target picker, used since [TaskKillerTarget] has 4 options - same
 * shape as [FabTypeDropdown]/[ScreenBehaviorDropdown] in UISettingsContent.kt, parameterized on
 * [TaskKillerTarget]. */
@Composable
private fun TaskKillerTargetDropdown(
    selected: TaskKillerTarget,
    onSelected: (TaskKillerTarget) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        FabRowSurface(onClick = { expanded = true }) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = selected.icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(text = selected.label, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TaskKillerTarget.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    leadingIcon = { Icon(imageVector = option.icon, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onSelected(option)
                    },
                )
            }
        }
    }
}

// Presentation-only icon/label, same reasoning as ScreenBehavior.icon/label in UISettingsContent.
// ThisScreen/OtherScreen deliberately reuse GameLaunchDisplayTarget's exact icon/label choices
// for the same shared concept.
private val TaskKillerTarget.icon: ImageVector
    get() =
        when (this) {
            TaskKillerTarget.FocusApp -> Icons.Filled.CenterFocusStrong
            TaskKillerTarget.ThisScreen -> Icons.Filled.PhoneAndroid
            TaskKillerTarget.OtherScreen -> Icons.Filled.Devices
            TaskKillerTarget.Both -> Icons.Filled.DevicesOther
        }

private val TaskKillerTarget.label: String
    get() =
        when (this) {
            TaskKillerTarget.FocusApp -> "Focus App"
            TaskKillerTarget.ThisScreen -> "This Screen"
            TaskKillerTarget.OtherScreen -> "Other Screen"
            TaskKillerTarget.Both -> "Both Screens"
        }

@Composable
private fun VolumeSyncSetting(state: VolumeSyncSettingsState) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingsLabel(icon = Icons.AutoMirrored.Filled.VolumeUp, text = "Volume Control")
            Text(
                text = "Customize the volume button behavior",
                style = MaterialTheme.typography.bodyMedium,
            )
            VolumeControlModePicker(
                enabled = state.enabled,
                mode = state.mode,
                selectionEnabled = state.enabled || state.accessibilityGranted,
                onSelectionChanged = { selection ->
                    when (selection) {
                        null -> state.onEnabledChanged(false)
                        else -> {
                            state.onModeChanged(selection)
                            state.onEnabledChanged(true)
                        }
                    }
                },
            )
            if (!state.accessibilityGranted) {
                AccessibilityGrantRow(
                    text = "Accessibility service not granted - required to intercept the volume buttons",
                    onClick = { context.startActivity(ThorAccessibilityPermission.requestIntent(context)) },
                )
            }
            if (!state.privilegedServiceAvailable) {
                Text(
                    text = "Privileged settings service not found on this firmware - Volume Control is unavailable",
                    style = MaterialTheme.typography.bodySmall,
                )
            } else if (!state.secondarySettingPresent) {
                Text(
                    text = "This firmware doesn't expose a bottom-screen volume setting",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VolumeControlModePicker(
    enabled: Boolean,
    mode: VolumeSyncMode,
    selectionEnabled: Boolean,
    onSelectionChanged: (VolumeSyncMode?) -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val selection = if (enabled) mode else null
    val selections = listOf(null, VolumeSyncMode.Linked, VolumeSyncMode.FollowFocus)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            selections.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = selection == option,
                    enabled = selectionEnabled,
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSelectionChanged(option)
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = selections.size),
                    icon = {
                        SegmentedButtonDefaults.Icon(active = selection == option) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = null,
                                modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                            )
                        }
                    },
                    label = { SegmentedButtonLabel(option.label) },
                )
            }
        }
        Text(
            text =
                when (selection) {
                    null -> "Volume buttons use standard system behavior."
                    VolumeSyncMode.Linked -> "Volume buttons control both screens together."
                    VolumeSyncMode.FollowFocus -> "Volume buttons control the screen currently in focus."
                },
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

// Presentation-only icon/label, same reasoning as ThemePreference.icon/label in UISettingsContent.
private val VolumeSyncMode?.icon: ImageVector
    get() =
        when (this) {
            null -> Icons.AutoMirrored.Filled.VolumeUp
            VolumeSyncMode.Linked -> Icons.Filled.SyncAlt
            VolumeSyncMode.FollowFocus -> Icons.Filled.CenterFocusStrong
        }

private val VolumeSyncMode?.label: String
    get() =
        when (this) {
            null -> "Default"
            VolumeSyncMode.Linked -> "Synced"
            VolumeSyncMode.FollowFocus -> "Focus"
        }

/** Shared "navigate to the app picker" row for Auto FPS's trigger-apps picker and Task
 * Killer's excluded-apps picker - identical shape, different destination/label. */
@Composable
private fun ThorAppPickerNavigationRow(
    label: String,
    onClick: () -> Unit,
) {
    val rowModifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Row(
            modifier = rowModifier,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun AccessibilityGrantRow(
    text: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onClick) { Text("Grant") }
    }
}

/** Matches [com.esde.companion.data.thor.LidWakeGuardCoordinator]'s own sensor-read timeout. */
private const val HALL_SENSOR_CALIBRATION_TIMEOUT_MS = 2000L

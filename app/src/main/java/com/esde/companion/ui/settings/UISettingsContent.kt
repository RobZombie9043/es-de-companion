package com.esde.companion.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Brightness1
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil3.compose.AsyncImage
import com.esde.companion.data.apps.AppIconLoader
import com.esde.companion.data.systemstatus.BluetoothConnectPermission
import com.esde.companion.domain.model.FabAssignments
import com.esde.companion.domain.model.FabPosition
import com.esde.companion.domain.model.FabSlot
import com.esde.companion.domain.model.FabType
import com.esde.companion.domain.model.InstalledApp
import com.esde.companion.domain.model.ScreenBehavior
import com.esde.companion.domain.model.ThemePreference
import kotlin.math.roundToInt

@Composable
internal fun UISettingsContent(
    themePreference: ThemePreference,
    onThemePreferenceChanged: (ThemePreference) -> Unit,
    overlayOpacityPercent: Int,
    onOverlayOpacityChanged: (Int) -> Unit,
    gamePlayingBehavior: ScreenBehavior,
    onGamePlayingBehaviorChanged: (ScreenBehavior) -> Unit,
    gamePlayingDimPercent: Int,
    onGamePlayingDimPercentChanged: (Int) -> Unit,
    screensaverBehavior: ScreenBehavior,
    onScreensaverBehaviorChanged: (ScreenBehavior) -> Unit,
    screensaverDimPercent: Int,
    onScreensaverDimPercentChanged: (Int) -> Unit,
    fabAssignments: FabAssignments,
    installedApps: List<InstalledApp>,
    onFabTypeChanged: (FabPosition, FabType) -> Unit,
    onFabCustomAppChanged: (FabPosition, String) -> Unit,
    onBluetoothPermissionRequested: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        ThemePicker(selected = themePreference, onSelected = onThemePreferenceChanged)
        OverlayOpacitySetting(percent = overlayOpacityPercent, onPercentChanged = onOverlayOpacityChanged)
        ScreenBehaviorPicker(
            title = "Game Playing Screen Behavior",
            icon = Icons.Filled.SportsEsports,
            options = listOf(ScreenBehavior.Nothing, ScreenBehavior.Dim, ScreenBehavior.Black, ScreenBehavior.GameManual),
            selected = gamePlayingBehavior,
            onSelected = onGamePlayingBehaviorChanged,
            dimAmount = DimAmountControl(gamePlayingDimPercent, onGamePlayingDimPercentChanged),
        )
        ScreenBehaviorPicker(
            title = "Screensaver Screen Behavior",
            icon = Icons.Filled.Nightlight,
            options = listOf(ScreenBehavior.Nothing, ScreenBehavior.Dim, ScreenBehavior.Black),
            selected = screensaverBehavior,
            onSelected = onScreensaverBehaviorChanged,
            dimAmount = DimAmountControl(screensaverDimPercent, onScreensaverDimPercentChanged),
        )
        FabControlSetting(
            fabAssignments = fabAssignments,
            installedApps = installedApps,
            onFabTypeChanged = onFabTypeChanged,
            onFabCustomAppChanged = onFabCustomAppChanged,
            onBluetoothPermissionRequested = onBluetoothPermissionRequested,
        )
    }
}

// Bottom corners never offer Music - it can only occupy one of the two top corners (see
// FabAssignments.with) - so the picker itself simply never presents the option there
// rather than needing runtime validation to reject an invalid selection.
private val TOP_FAB_OPTIONS =
    listOf(
        FabType.Music,
        FabType.Settings,
        FabType.GameManual,
        FabType.AppDrawer,
        FabType.CustomApp,
        FabType.RetroAchievements,
        FabType.Clock,
        FabType.SystemStatus,
        FabType.ClockAndSystemStatus,
        FabType.None,
    )
private val BOTTOM_FAB_OPTIONS =
    listOf(
        FabType.Settings,
        FabType.GameManual,
        FabType.AppDrawer,
        FabType.CustomApp,
        FabType.RetroAchievements,
        FabType.None,
    )

/**
 * Master background opacity for every translucent overlay surface - the App Drawer, the
 * App Dock, the music controls panel, and the Settings/music-FAB/Edit-Widgets corner
 * buttons - see OnboardingRepository.observeOverlayOpacityPercent's kdoc for why this
 * replaced a separate slider per surface.
 */
@Composable
private fun OverlayOpacitySetting(
    percent: Int,
    onPercentChanged: (Int) -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingsLabel(icon = Icons.Filled.Opacity, text = "Overlay Opacity")
            Text(text = "$percent%", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = percent.toFloat(),
                onValueChange = { onPercentChanged(it.roundToInt()) },
                onValueChangeFinished = { hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress) },
                valueRange = 0f..100f,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScreenBehaviorPicker(
    title: String,
    icon: ImageVector,
    options: List<ScreenBehavior>,
    selected: ScreenBehavior,
    onSelected: (ScreenBehavior) -> Unit,
    dimAmount: DimAmountControl,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingsLabel(icon = icon, text = title)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, behavior ->
                    SegmentedButton(
                        selected = behavior == selected,
                        onClick = { onSelected(behavior) },
                        shape =
                            SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = options.size,
                            ),
                        icon = {
                            SegmentedButtonDefaults.Icon(active = behavior == selected) {
                                Icon(
                                    imageVector = behavior.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                                )
                            }
                        },
                        label = { Text(behavior.label) },
                    )
                }
            }
            if (selected == ScreenBehavior.Dim) {
                DimAmountSlider(percent = dimAmount.percent, onPercentChanged = dimAmount.onPercentChanged)
            }
        }
    }
}

/** Bundles a Dim amount with its change handler into one [ScreenBehaviorPicker] parameter,
 * keeping that function's parameter count under detekt's LongParameterList threshold. */
private data class DimAmountControl(
    val percent: Int,
    val onPercentChanged: (Int) -> Unit,
)

/**
 * Strength of the translucent black scrim MainActivity draws when a Screen Behavior
 * picker above is set to Dim - only shown while that's the current selection, same
 * on-demand-reveal idiom as VideoDelaySetting appearing only once video playback is
 * enabled.
 */
@Composable
private fun DimAmountSlider(
    percent: Int,
    onPercentChanged: (Int) -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = "Dimming Amount: $percent%", style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = percent.toFloat(),
            onValueChange = { onPercentChanged(it.roundToInt()) },
            onValueChangeFinished = { hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress) },
            valueRange = 0f..100f,
        )
    }
}

// Presentation-only icon/label, same reasoning as ThemePreference.icon/label above.
private val ScreenBehavior.icon: ImageVector
    get() =
        when (this) {
            ScreenBehavior.Nothing -> Icons.Filled.Brightness7
            ScreenBehavior.Dim -> Icons.Filled.Brightness4
            ScreenBehavior.Black -> Icons.Filled.Brightness1
            ScreenBehavior.GameManual -> Icons.AutoMirrored.Filled.MenuBook
        }

private val ScreenBehavior.label: String
    get() =
        when (this) {
            ScreenBehavior.Nothing -> "On"
            ScreenBehavior.Dim -> "Dim"
            ScreenBehavior.Black -> "Off"
            ScreenBehavior.GameManual -> "Manual"
        }

/**
 * Settings > UI Settings > Floating Action Buttons - one panel covering all four corners
 * (see [UISettingsContent]), rather than four separate cards. Each corner is a dropdown
 * (see [FabTypeDropdown]) rather than a [SingleChoiceSegmentedButtonRow] - once
 * [FabType.AppDrawer]/[FabType.CustomApp] joined the original four options, a segmented
 * row of 5-6 icon+label buttons per corner no longer fit comfortably; a dropdown scales to
 * any option count without the row wrapping or shrinking illegibly.
 */
@Composable
private fun FabControlSetting(
    fabAssignments: FabAssignments,
    installedApps: List<InstalledApp>,
    onFabTypeChanged: (FabPosition, FabType) -> Unit,
    onFabCustomAppChanged: (FabPosition, String) -> Unit,
    onBluetoothPermissionRequested: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SettingsLabel(icon = Icons.Filled.Apps, text = "Floating Action Buttons")
            FabPositionRow(
                title = "Top Left",
                options = TOP_FAB_OPTIONS,
                slot = fabAssignments[FabPosition.TopStart],
                installedApps = installedApps,
                onTypeSelected = { onFabTypeChanged(FabPosition.TopStart, it) },
                onAppSelected = { onFabCustomAppChanged(FabPosition.TopStart, it) },
                onBluetoothPermissionRequested = onBluetoothPermissionRequested,
            )
            FabPositionRow(
                title = "Top Right",
                options = TOP_FAB_OPTIONS,
                slot = fabAssignments[FabPosition.TopEnd],
                installedApps = installedApps,
                onTypeSelected = { onFabTypeChanged(FabPosition.TopEnd, it) },
                onAppSelected = { onFabCustomAppChanged(FabPosition.TopEnd, it) },
                onBluetoothPermissionRequested = onBluetoothPermissionRequested,
            )
            FabPositionRow(
                title = "Bottom Left",
                options = BOTTOM_FAB_OPTIONS,
                slot = fabAssignments[FabPosition.BottomStart],
                installedApps = installedApps,
                onTypeSelected = { onFabTypeChanged(FabPosition.BottomStart, it) },
                onAppSelected = { onFabCustomAppChanged(FabPosition.BottomStart, it) },
                onBluetoothPermissionRequested = onBluetoothPermissionRequested,
            )
            FabPositionRow(
                title = "Bottom Right",
                options = BOTTOM_FAB_OPTIONS,
                slot = fabAssignments[FabPosition.BottomEnd],
                installedApps = installedApps,
                onTypeSelected = { onFabTypeChanged(FabPosition.BottomEnd, it) },
                onAppSelected = { onFabCustomAppChanged(FabPosition.BottomEnd, it) },
                onBluetoothPermissionRequested = onBluetoothPermissionRequested,
            )
        }
    }
}

/** One corner's worth of controls: its type dropdown, plus an app-picker row that only
 * appears once [FabType.CustomApp] is selected. */
@Composable
private fun FabPositionRow(
    title: String,
    options: List<FabType>,
    slot: FabSlot,
    installedApps: List<InstalledApp>,
    onTypeSelected: (FabType) -> Unit,
    onAppSelected: (String) -> Unit,
    onBluetoothPermissionRequested: () -> Unit,
) {
    var showAppPicker by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = title, style = MaterialTheme.typography.labelLarge)
        FabTypeDropdown(options = options, selected = slot.type, onSelected = onTypeSelected)
        if (slot.type == FabType.CustomApp) {
            val selectedApp = installedApps.firstOrNull { it.packageName == slot.customAppPackageName }
            FabRowSurface(onClick = { showAppPicker = true }) {
                Text(text = selectedApp?.label ?: "Select App", style = MaterialTheme.typography.bodyMedium)
                Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null)
            }
        }
        if (slot.type == FabType.SystemStatus || slot.type == FabType.ClockAndSystemStatus) {
            BluetoothPermissionRow(onRequested = onBluetoothPermissionRequested)
        }
    }

    if (showAppPicker) {
        SelectAppDialog(
            installedApps = installedApps,
            onAppPicked = {
                onAppSelected(it)
                showAppPicker = false
            },
            onDismiss = { showAppPicker = false },
        )
    }
}

/**
 * Shown under the FAB type dropdown once [FabType.SystemStatus]/[FabType.ClockAndSystemStatus]
 * is selected and BLUETOOTH_CONNECT isn't granted - the discoverable way to (re-)request the
 * permission, since the live FAB itself is a plain display and doesn't request it on tap.
 * Absent once granted, or if the device has no Bluetooth hardware at all.
 */
@Composable
private fun BluetoothPermissionRow(onRequested: () -> Unit) {
    val context = LocalContext.current
    val hasHardware = remember { BluetoothConnectPermission.hasBluetoothHardware(context) }
    var granted by remember { mutableStateOf(BluetoothConnectPermission.isGranted(context)) }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            granted = BluetoothConnectPermission.isGranted(context)
            onRequested()
        }

    // Covers granting via system Settings after backgrounding this screen, same idiom as
    // OnboardingScreen's AllFilesAccessPermission check.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    granted = BluetoothConnectPermission.isGranted(context)
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (hasHardware && !granted) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Bluetooth permission needed to show Bluetooth status.",
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = { launcher.launch(BluetoothConnectPermission.PERMISSION) }) {
                Text("Grant Permission")
            }
        }
    }
}

@Composable
private fun FabTypeDropdown(
    options: List<FabType>,
    selected: FabType,
    onSelected: (FabType) -> Unit,
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
            options.forEach { option ->
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

/** Shared row chrome for [FabTypeDropdown]'s trigger and the Custom App selector row - a
 * tappable surface a shade lighter than the panel behind it, with its content spaced to
 * the edges. */
@Composable
private fun FabRowSurface(
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
private fun SelectAppDialog(
    installedApps: List<InstalledApp>,
    onAppPicked: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select app") },
        text = {
            // Same fillMaxWidth-on-every-row reasoning as AppDock's AddAppDialog - without
            // it, varying label widths make the dialog visibly wobble side to side while
            // scrolling.
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(installedApps, key = { it.packageName }) { app ->
                    SelectAppRow(app = app, onClick = { onAppPicked(app.packageName) })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun SelectAppRow(
    app: InstalledApp,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val icon by produceState<Any?>(initialValue = null, key1 = app.packageName) {
        value = AppIconLoader.loadIcon(context, app.packageName)
    }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(model = icon, contentDescription = null, modifier = Modifier.size(40.dp))
        Text(text = app.label, style = MaterialTheme.typography.bodyLarge)
    }
}

// Presentation-only icon/label, same reasoning as ScreenBehavior.icon/label above.
private val FabType.icon: ImageVector
    get() =
        when (this) {
            FabType.Music -> Icons.Filled.MusicNote
            FabType.Settings -> Icons.Filled.Menu
            FabType.GameManual -> Icons.AutoMirrored.Filled.MenuBook
            FabType.AppDrawer -> Icons.Filled.Apps
            FabType.CustomApp -> Icons.AutoMirrored.Filled.Launch
            FabType.RetroAchievements -> Icons.Filled.EmojiEvents
            FabType.Clock -> Icons.Filled.AccessTime
            FabType.SystemStatus -> Icons.Filled.Wifi
            FabType.ClockAndSystemStatus -> Icons.Filled.AccessTime
            FabType.None -> Icons.Filled.Clear
        }

private val FabType.label: String
    get() =
        when (this) {
            FabType.Music -> "Music"
            FabType.Settings -> "Main Menu"
            FabType.GameManual -> "Manual"
            FabType.AppDrawer -> "App Drawer"
            FabType.CustomApp -> "App"
            FabType.RetroAchievements -> "Achievements"
            FabType.Clock -> "Clock"
            FabType.SystemStatus -> "System Status"
            FabType.ClockAndSystemStatus -> "Clock & Status"
            FabType.None -> "None"
        }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemePicker(
    selected: ThemePreference,
    onSelected: (ThemePreference) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingsLabel(icon = Icons.Filled.Palette, text = "Theme")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ThemePreference.entries.forEachIndexed { index, theme ->
                    SegmentedButton(
                        selected = theme == selected,
                        onClick = { onSelected(theme) },
                        shape =
                            SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = ThemePreference.entries.size,
                            ),
                        icon = {
                            SegmentedButtonDefaults.Icon(active = theme == selected) {
                                Icon(
                                    imageVector = theme.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                                )
                            }
                        },
                        label = { Text(theme.label) },
                    )
                }
            }
        }
    }
}

// Presentation-only icon/label, kept in the UI layer rather than on the enum itself so
// ThemePreference stays a plain domain identifier with no display concerns.
private val ThemePreference.icon: ImageVector
    get() =
        when (this) {
            ThemePreference.Auto -> Icons.Filled.BrightnessAuto
            ThemePreference.Light -> Icons.Filled.LightMode
            ThemePreference.Dark -> Icons.Filled.DarkMode
        }

private val ThemePreference.label: String
    get() =
        when (this) {
            ThemePreference.Auto -> "Auto"
            ThemePreference.Light -> "Light"
            ThemePreference.Dark -> "Dark"
        }

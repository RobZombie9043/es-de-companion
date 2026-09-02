package com.esde.companion.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Wifi
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.esde.companion.data.retroachievements.retroAchievementsEnabled
import com.esde.companion.data.systemstatus.BluetoothConnectPermission
import com.esde.companion.domain.model.FabAssignments
import com.esde.companion.domain.model.FabPosition
import com.esde.companion.domain.model.FabSlot
import com.esde.companion.domain.model.FabType
import com.esde.companion.domain.model.GameLaunchDisplayTarget
import com.esde.companion.domain.model.InstalledApp
import com.esde.companion.domain.model.ScreenBehavior
import com.esde.companion.domain.model.ThemePreference
import com.esde.companion.ui.SegmentedButtonLabel
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
    onManageGameLaunchOverridesClick: () -> Unit,
    gameLaunchEnabled: Boolean,
    onGameLaunchEnabledChanged: (Boolean) -> Unit,
    gameLaunchDisplayTarget: GameLaunchDisplayTarget,
    onGameLaunchDisplayTargetChanged: (GameLaunchDisplayTarget) -> Unit,
    closeAppOnGameEndEnabled: Boolean,
    onCloseAppOnGameEndEnabledChanged: (Boolean) -> Unit,
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
        FabControlSetting(
            fabAssignments = fabAssignments,
            installedApps = installedApps,
            onFabTypeChanged = onFabTypeChanged,
            onFabCustomAppChanged = onFabCustomAppChanged,
            onBluetoothPermissionRequested = onBluetoothPermissionRequested,
        )
        ScreenBehaviorPicker(
            title = "Screensaver Screen Behavior",
            icon = Icons.Filled.Nightlight,
            options = listOf(ScreenBehavior.Nothing, ScreenBehavior.Dim, ScreenBehavior.Black),
            selected = screensaverBehavior,
            onSelected = onScreensaverBehaviorChanged,
            dimAmount = DimAmountControl(screensaverDimPercent, onScreensaverDimPercentChanged),
            forceDropdown = true,
        )
        ScreenBehaviorPicker(
            title = "Game Playing Screen Behavior",
            icon = Icons.Filled.SportsEsports,
            options =
                listOf(
                    ScreenBehavior.Nothing,
                    ScreenBehavior.Dim,
                    ScreenBehavior.Black,
                    ScreenBehavior.GameManual,
                    ScreenBehavior.GameGuide,
                ),
            selected = gamePlayingBehavior,
            onSelected = onGamePlayingBehaviorChanged,
            dimAmount = DimAmountControl(gamePlayingDimPercent, onGamePlayingDimPercentChanged),
        )
        GameLaunchOverrideSetting(
            state =
                GameLaunchOverrideState(
                    enabled = gameLaunchEnabled,
                    launchDisplayTarget = gameLaunchDisplayTarget,
                    closeAppOnGameEndEnabled = closeAppOnGameEndEnabled,
                ),
            actions =
                GameLaunchOverrideActions(
                    onEnabledChanged = onGameLaunchEnabledChanged,
                    onLaunchDisplayTargetChanged = onGameLaunchDisplayTargetChanged,
                    onManageClick = onManageGameLaunchOverridesClick,
                    onCloseAppOnGameEndEnabledChanged = onCloseAppOnGameEndEnabledChanged,
                ),
        )
    }
}

/** Bundles [GameLaunchOverrideSetting]'s non-callback state into one parameter, keeping that
 * composable under detekt's LongParameterList threshold - same reasoning as [DimAmountControl]. */
private data class GameLaunchOverrideState(
    val enabled: Boolean,
    val launchDisplayTarget: GameLaunchDisplayTarget,
    val closeAppOnGameEndEnabled: Boolean,
)

/** The callback half of [GameLaunchOverrideSetting]'s parameters - see [GameLaunchOverrideState]. */
private data class GameLaunchOverrideActions(
    val onEnabledChanged: (Boolean) -> Unit,
    val onLaunchDisplayTargetChanged: (GameLaunchDisplayTarget) -> Unit,
    val onManageClick: () -> Unit,
    val onCloseAppOnGameEndEnabledChanged: (Boolean) -> Unit,
)

/**
 * Settings > UI Settings > Game Launch Override - a master enable toggle ([GameLaunchOverrideState.enabled],
 * defaults on - see [com.esde.companion.domain.repository.GameLaunchAppRepository.observeEnabled]'s
 * kdoc for why), plus, only while that's on: the entry point into the system/game browser (see
 * [com.esde.companion.ui.main.LongPressSettingsMenu]'s `GameLaunchOverrideSystems`/
 * `GameLaunchOverrideGames` pages) and the two settings that aren't per-system/per-game: a
 * global choice of which display a launched app opens on (see [GameLaunchDisplayTarget]), and
 * whether that launched app gets force-stopped once the game that triggered it ends (see
 * [com.esde.companion.data.gamelist.GameLaunchOverrideCoordinator]'s kdoc - Thor-only, best-effort,
 * off by default). Hiding those three while disabled (rather than just disabling them) avoids
 * showing settings for per-game overrides that, while the master toggle is off, don't do
 * anything regardless of what they're set to. A segmented row rather than a dropdown for the
 * display-target choice - only two options, well under [SEGMENTED_ROW_TO_DROPDOWN_THRESHOLD].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameLaunchOverrideSetting(
    state: GameLaunchOverrideState,
    actions: GameLaunchOverrideActions,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            GameLaunchOverrideHeader(enabled = state.enabled, onEnabledChanged = actions.onEnabledChanged)
            SettingsRowVisibility(visible = state.enabled) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    FabRowSurface(onClick = actions.onManageClick) {
                        Text(text = "Manage Systems & Games", style = MaterialTheme.typography.bodyMedium)
                        Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null)
                    }
                    LaunchDisplayTargetPicker(
                        selected = state.launchDisplayTarget,
                        onSelected = actions.onLaunchDisplayTargetChanged,
                    )
                    ToggleSettingRow(
                        icon = Icons.Filled.Close,
                        title = "Close App on Game End",
                        description = "Force-stop the launched app once the game ends (Thor devices only)",
                        enabled = state.closeAppOnGameEndEnabled,
                        onEnabledChanged = actions.onCloseAppOnGameEndEnabledChanged,
                    )
                }
            }
        }
    }
}

// Plain label + inline Switch, not ToggleSettingRow - same "panel header with its own master
// switch" shape ThorSettingsContent's LidWakeGuard/AutoFpsMode/TaskKiller panels use, rather
// than nesting ToggleSettingRow's own card-styled Surface inside this panel's Surface (doubled
// padding/background, inconsistent with every other panel here). Pulled out of
// GameLaunchOverrideSetting purely to keep that function under detekt's LongMethod threshold.
@Composable
private fun GameLaunchOverrideHeader(
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    SettingsLabel(icon = Icons.AutoMirrored.Filled.Launch, text = "Launch App on Game Start")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Launch an app automatically when a game starts",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = enabled,
            onCheckedChange = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onEnabledChanged(it)
            },
        )
    }
}

/** The "Launch App On" segmented display-target picker - pulled out of
 * [GameLaunchOverrideSetting] for the same LongMethod reasoning as [GameLaunchOverrideHeader]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LaunchDisplayTargetPicker(
    selected: GameLaunchDisplayTarget,
    onSelected: (GameLaunchDisplayTarget) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Launch App On", style = MaterialTheme.typography.labelLarge)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            GameLaunchDisplayTarget.entries.forEachIndexed { index, target ->
                SegmentedButton(
                    selected = target == selected,
                    onClick = { onSelected(target) },
                    shape =
                        SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = GameLaunchDisplayTarget.entries.size,
                        ),
                    icon = {
                        SegmentedButtonDefaults.Icon(active = target == selected) {
                            Icon(
                                imageVector = target.icon,
                                contentDescription = null,
                                modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                            )
                        }
                    },
                    label = { SegmentedButtonLabel(target.label) },
                )
            }
        }
    }
}

// Presentation-only icon/label, same reasoning as ThemePreference.icon/label above.
private val GameLaunchDisplayTarget.icon: ImageVector
    get() =
        when (this) {
            GameLaunchDisplayTarget.ThisScreen -> Icons.Filled.PhoneAndroid
            GameLaunchDisplayTarget.OtherScreen -> Icons.Filled.Devices
        }

private val GameLaunchDisplayTarget.label: String
    get() =
        when (this) {
            GameLaunchDisplayTarget.ThisScreen -> "This Screen"
            GameLaunchDisplayTarget.OtherScreen -> "Other Screen"
        }

// Bottom corners never offer Music - it can only occupy one of the two top corners (see
// FabAssignments.with) - so the picker itself simply never presents the option there
// rather than needing runtime validation to reject an invalid selection. RetroAchievements
// is filtered out entirely while its own feature flag is false - see
// retroAchievementsEnabled()'s kdoc.
private val TOP_FAB_OPTIONS =
    listOf(
        FabType.None,
        FabType.Settings,
        FabType.AppDrawer,
        FabType.CustomApp,
        FabType.GameGuides,
        FabType.GameManual,
        FabType.RetroAchievements,
        FabType.Music,
        FabType.Clock,
        FabType.SystemStatus,
        FabType.ClockAndSystemStatus,
    ).filter { it != FabType.RetroAchievements || retroAchievementsEnabled() }
private val BOTTOM_FAB_OPTIONS =
    listOf(
        FabType.None,
        FabType.Settings,
        FabType.AppDrawer,
        FabType.CustomApp,
        FabType.GameGuides,
        FabType.GameManual,
        FabType.RetroAchievements,
    ).filter { it != FabType.RetroAchievements || retroAchievementsEnabled() }

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
    forceDropdown: Boolean = false,
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
            if (forceDropdown || options.size >= SEGMENTED_ROW_TO_DROPDOWN_THRESHOLD) {
                ScreenBehaviorDropdown(options = options, selected = selected, onSelected = onSelected)
            } else {
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
                            label = { SegmentedButtonLabel(behavior.label) },
                        )
                    }
                }
            }
            SettingsRowVisibility(visible = selected == ScreenBehavior.Dim) {
                DimAmountSlider(percent = dimAmount.percent, onPercentChanged = dimAmount.onPercentChanged)
            }
        }
    }
}

// A segmented row of icon+label buttons stops fitting comfortably past this many options -
// same reasoning as FabTypeDropdown's own switch away from a segmented row, and confirmed by
// "Manual" truncating in the 4-option Game Playing Screen Behavior picker even at the
// device's regular font scale. Screensaver Screen Behavior (3 options) is under this threshold
// but forced to a dropdown anyway (see ScreenBehaviorPicker's forceDropdown) to match Game
// Playing Screen Behavior's look, per user request.
private const val SEGMENTED_ROW_TO_DROPDOWN_THRESHOLD = 4

/** Dropdown variant of [ScreenBehaviorPicker]'s selector, used once [options] crosses
 * [SEGMENTED_ROW_TO_DROPDOWN_THRESHOLD] - same shape as [FabTypeDropdown], parameterized on
 * [ScreenBehavior] instead of [FabType]. */
@Composable
private fun ScreenBehaviorDropdown(
    options: List<ScreenBehavior>,
    selected: ScreenBehavior,
    onSelected: (ScreenBehavior) -> Unit,
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
            ScreenBehavior.GameGuide -> Icons.Filled.LibraryBooks
        }

private val ScreenBehavior.label: String
    get() =
        when (this) {
            ScreenBehavior.Nothing -> "On"
            ScreenBehavior.Dim -> "Dim"
            ScreenBehavior.Black -> "Off"
            ScreenBehavior.GameManual -> "Manual"
            ScreenBehavior.GameGuide -> "Guide"
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
        SettingsRowVisibility(visible = slot.type == FabType.CustomApp) {
            val selectedApp = installedApps.firstOrNull { it.packageName == slot.customAppPackageName }
            FabRowSurface(onClick = { showAppPicker = true }) {
                Text(text = selectedApp?.label ?: "Select App", style = MaterialTheme.typography.bodyMedium)
                Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null)
            }
        }
        val isSystemStatusFab = slot.type == FabType.SystemStatus || slot.type == FabType.ClockAndSystemStatus
        SettingsRowVisibility(visible = isSystemStatusFab) {
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
 * the edges. Internal (not private) so ThorSettingsContent.kt's TaskKillerTargetDropdown can
 * reuse the same trigger-row chrome rather than duplicating it. */
@Composable
internal fun FabRowSurface(
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
            FabType.GameGuides -> Icons.Filled.LibraryBooks
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
            FabType.GameGuides -> "Game Guides"
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
                        label = { SegmentedButtonLabel(theme.label) },
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

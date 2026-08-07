package com.esde.companion.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness1
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.esde.companion.data.storage.SafPathResolver
import com.esde.companion.domain.model.DockSize
import com.esde.companion.domain.model.LogFolderValidation
import com.esde.companion.domain.model.MediaFolderValidation
import com.esde.companion.domain.model.MusicDuckingMode
import com.esde.companion.domain.model.ScreenBehavior
import com.esde.companion.domain.model.ThemePreference
import kotlin.math.roundToInt

/**
 * Shared corner radius for the background panel behind every settings item (category
 * rows, folder settings, toggles, the theme picker) so they read as a consistent set of
 * cards rather than each item picking its own rounding.
 */
internal val SettingsItemShape = RoundedCornerShape(16.dp)

/**
 * Shared translucency for every settings item's background - lets the fallback background
 * image behind the long-press popup's content (see LongPressSettingsMenu) show through the
 * cards rather than being fully hidden behind an opaque surface color.
 */
private const val SETTINGS_PANEL_ALPHA = 0.8f

/**
 * Per-category settings content, one composable per [SettingsCategory] plus the shared
 * category-list row and leaf setting composables (toggle/slider/picker/folder-picker
 * rows). These used to be hosted by a standalone full-screen `SettingsScreen`; that
 * screen is gone - this content is now hosted inside the long-press popup instead, see
 * `LongPressSettingsMenu` in the `ui.main` package (hence `internal`, not `private`,
 * visibility throughout this file).
 */
@Composable
internal fun SettingsCategoryRow(category: SettingsCategory, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(text = category.description, style = MaterialTheme.typography.bodySmall)
            }
            Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
internal fun SetupSettingsContent(
    uiState: SettingsUiState,
    onLogFolderPicked: (String) -> Unit,
    onMediaFolderPicked: (String) -> Unit,
    onCustomSystemImagesFolderPicked: (String) -> Unit,
    onCustomSystemImagesFolderCleared: () -> Unit,
    onCustomLogosFolderPicked: (String) -> Unit,
    onCustomLogosFolderCleared: () -> Unit,
    onCustomMusicFolderPicked: (String) -> Unit,
    onCustomMusicFolderCleared: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        if (!uiState.permissionGranted) {
            Text(
                "All files access isn't currently granted - folder changes below " +
                        "may not take effect until it's re-enabled in system Settings.",
            )
        }

        FolderSetting(
            label = "ES-DE folder",
            path = uiState.logFolderPath,
            isValidating = uiState.isValidatingLogFolder,
            statusText = uiState.logFolderValidation.toStatusText(),
            onPick = { uri -> SafPathResolver.resolvePath(uri)?.let(onLogFolderPicked) },
        )

        FolderSetting(
            label = "Media folder",
            path = uiState.mediaFolderPath,
            isValidating = uiState.isValidatingMediaFolder,
            statusText = uiState.mediaFolderValidation.toStatusText(),
            onPick = { uri -> SafPathResolver.resolvePath(uri)?.let(onMediaFolderPicked) },
        )

        OptionalFolderSetting(
            label = "Custom System Images Folder",
            path = uiState.customSystemImagesFolderPath,
            isValidating = uiState.isValidatingCustomSystemImagesFolder,
            statusText = uiState.customSystemImagesFolderValidation.toStatusText(),
            onPick = { uri -> SafPathResolver.resolvePath(uri)?.let(onCustomSystemImagesFolderPicked) },
            onClear = onCustomSystemImagesFolderCleared,
        )

        OptionalFolderSetting(
            label = "Custom Logos Folder",
            path = uiState.customLogosFolderPath,
            isValidating = uiState.isValidatingCustomLogosFolder,
            statusText = uiState.customLogosFolderValidation.toStatusText(),
            onPick = { uri -> SafPathResolver.resolvePath(uri)?.let(onCustomLogosFolderPicked) },
            onClear = onCustomLogosFolderCleared,
        )

        OptionalFolderSetting(
            label = "Custom Music Folder",
            path = uiState.customMusicFolderPath,
            isValidating = uiState.isValidatingCustomMusicFolder,
            statusText = uiState.customMusicFolderValidation.toStatusText(),
            onPick = { uri -> SafPathResolver.resolvePath(uri)?.let(onCustomMusicFolderPicked) },
            onClear = onCustomMusicFolderCleared,
        )
    }
}

@Composable
internal fun UISettingsContent(
    themePreference: ThemePreference,
    onThemePreferenceChanged: (ThemePreference) -> Unit,
    overlayOpacityPercent: Int,
    onOverlayOpacityChanged: (Int) -> Unit,
    gamePlayingBehavior: ScreenBehavior,
    onGamePlayingBehaviorChanged: (ScreenBehavior) -> Unit,
    screensaverBehavior: ScreenBehavior,
    onScreensaverBehaviorChanged: (ScreenBehavior) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        ThemePicker(selected = themePreference, onSelected = onThemePreferenceChanged)
        OverlayOpacitySetting(percent = overlayOpacityPercent, onPercentChanged = onOverlayOpacityChanged)
        ScreenBehaviorPicker(
            title = "Game Playing Screen Behavior",
            options = listOf(ScreenBehavior.Nothing, ScreenBehavior.Dim, ScreenBehavior.Black, ScreenBehavior.GameManual),
            selected = gamePlayingBehavior,
            onSelected = onGamePlayingBehaviorChanged,
        )
        ScreenBehaviorPicker(
            title = "Screensaver Screen Behavior",
            options = listOf(ScreenBehavior.Nothing, ScreenBehavior.Dim, ScreenBehavior.Black),
            selected = screensaverBehavior,
            onSelected = onScreensaverBehaviorChanged,
        )
    }
}

/**
 * Widgets category content - just hands off to the full-screen widget editor, same
 * "navigate out" idiom Edit Widgets used under UI Settings before it got its own
 * top-level category.
 */
@Composable
internal fun WidgetsSettingsContent(onEditWidgetsClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        EditWidgetsEntry(onClick = onEditWidgetsClick)
    }
}

/**
 * Master background opacity for every translucent overlay surface - the App Drawer, the
 * App Dock, the music controls panel, and the Settings/music-FAB/Edit-Widgets corner
 * buttons - see OnboardingRepository.observeOverlayOpacityPercent's kdoc for why this
 * replaced a separate slider per surface.
 */
@Composable
private fun OverlayOpacitySetting(percent: Int, onPercentChanged: (Int) -> Unit) {
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
            Text(
                text = "Overlay Opacity",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
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

@Composable
internal fun VideoPlaybackSettingsContent(
    videoPlaybackEnabled: Boolean,
    onVideoPlaybackEnabledChanged: (Boolean) -> Unit,
    videoDelaySeconds: Int,
    onVideoDelaySecondsChanged: (Int) -> Unit,
    videoAudioEnabled: Boolean,
    onVideoAudioEnabledChanged: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        VideoPlaybackEnabledSetting(enabled = videoPlaybackEnabled, onEnabledChanged = onVideoPlaybackEnabledChanged)
        if (videoPlaybackEnabled) {
            VideoDelaySetting(delaySeconds = videoDelaySeconds, onDelaySecondsChanged = onVideoDelaySecondsChanged)
            VideoAudioSetting(enabled = videoAudioEnabled, onEnabledChanged = onVideoAudioEnabledChanged)
        }
    }
}

@Composable
private fun VideoPlaybackEnabledSetting(enabled: Boolean, onEnabledChanged: (Boolean) -> Unit) {
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
            Text(
                text = "Background Video",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Play game videos while browsing",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onEnabledChanged(it)
                    },
                )
            }
        }
    }
}

@Composable
private fun VideoDelaySetting(delaySeconds: Int, onDelaySecondsChanged: (Int) -> Unit) {
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
            Text(
                text = "Video Start Delay",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = if (delaySeconds == 0) "Off" else "${delaySeconds}s",
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = delaySeconds.toFloat(),
                onValueChange = { onDelaySecondsChanged(it.roundToInt()) },
                onValueChangeFinished = { hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress) },
                valueRange = 0f..10f,
                steps = 9,
            )
        }
    }
}

@Composable
private fun VideoAudioSetting(enabled: Boolean, onEnabledChanged: (Boolean) -> Unit) {
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
            Text(
                text = "Video Audio",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Enable video audio",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onEnabledChanged(it)
                    },
                )
            }
        }
    }
}

@Composable
internal fun OtherSettingsContent(
    closeCompanionOnQuitEnabled: Boolean,
    onCloseCompanionOnQuitEnabledChanged: (Boolean) -> Unit,
    settingsFabVisible: Boolean,
    onSettingsFabVisibleChanged: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        CloseCompanionOnQuitSetting(
            enabled = closeCompanionOnQuitEnabled,
            onEnabledChanged = onCloseCompanionOnQuitEnabledChanged,
        )
        SettingsFabVisibleSetting(
            enabled = settingsFabVisible,
            onEnabledChanged = onSettingsFabVisibleChanged,
        )
    }
}

@Composable
private fun CloseCompanionOnQuitSetting(enabled: Boolean, onEnabledChanged: (Boolean) -> Unit) {
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
            Text(
                text = "Close Companion App on ES-DE Quit",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Close ES-DE Companion when ES-DE quits",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onEnabledChanged(it)
                    },
                )
            }
        }
    }
}

@Composable
private fun SettingsFabVisibleSetting(enabled: Boolean, onEnabledChanged: (Boolean) -> Unit) {
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
            Text(
                text = "Show Settings Button",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Show the Settings gear on the main screen. It's always reachable via the long-press menu regardless of this setting.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onEnabledChanged(it)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScreenBehaviorPicker(
    title: String,
    options: List<ScreenBehavior>,
    selected: ScreenBehavior,
    onSelected: (ScreenBehavior) -> Unit,
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
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, behavior ->
                    SegmentedButton(
                        selected = behavior == selected,
                        onClick = { onSelected(behavior) },
                        shape = SegmentedButtonDefaults.itemShape(
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
        }
    }
}

// Presentation-only icon/label, same reasoning as ThemePreference.icon/label above.
private val ScreenBehavior.icon: ImageVector
    get() = when (this) {
        ScreenBehavior.Nothing -> Icons.Filled.Brightness7
        ScreenBehavior.Dim -> Icons.Filled.Brightness4
        ScreenBehavior.Black -> Icons.Filled.Brightness1
        ScreenBehavior.GameManual -> Icons.Filled.MenuBook
    }

private val ScreenBehavior.label: String
    get() = when (this) {
        ScreenBehavior.Nothing -> "On"
        ScreenBehavior.Dim -> "Dimmed"
        ScreenBehavior.Black -> "Off"
        ScreenBehavior.GameManual -> "Manual"
    }

@Composable
internal fun AppDrawerSettingsContent(
    gridColumns: Int,
    onGridColumnsChanged: (Int) -> Unit,
    sortFoldersOnTop: Boolean,
    onSortFoldersOnTopChanged: (Boolean) -> Unit,
    onManageAppsClick: () -> Unit,
    dockEnabled: Boolean,
    onDockEnabledChanged: (Boolean) -> Unit,
    dockMaxApps: Int,
    onDockMaxAppsChanged: (Int) -> Unit,
    dockSize: DockSize,
    onDockSizeChanged: (DockSize) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        ManageAppsEntry(onClick = onManageAppsClick)
        GridColumnsSetting(columns = gridColumns, onColumnsChanged = onGridColumnsChanged)
        SortFoldersOnTopSetting(enabled = sortFoldersOnTop, onEnabledChanged = onSortFoldersOnTopChanged)
        DockEnabledSetting(enabled = dockEnabled, onEnabledChanged = onDockEnabledChanged)
        if (dockEnabled) {
            DockMaxAppsSetting(maxApps = dockMaxApps, onMaxAppsChanged = onDockMaxAppsChanged)
            DockSizeSetting(size = dockSize, onSizeChanged = onDockSizeChanged)
        }
    }
}

@Composable
private fun SortFoldersOnTopSetting(enabled: Boolean, onEnabledChanged: (Boolean) -> Unit) {
    val hapticFeedback = LocalHapticFeedback.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text = "Sort folders on top of apps",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Group folders ahead of ungrouped apps instead of sorting them in alphabetically",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEnabledChanged(it)
                },
            )
        }
    }
}

@Composable
private fun DockEnabledSetting(enabled: Boolean, onEnabledChanged: (Boolean) -> Unit) {
    val hapticFeedback = LocalHapticFeedback.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text = "Enable Dock",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "A row of pinned apps at the bottom of the main screen",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEnabledChanged(it)
                },
            )
        }
    }
}

@Composable
private fun DockMaxAppsSetting(maxApps: Int, onMaxAppsChanged: (Int) -> Unit) {
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
            Text(
                text = "Maximum dock apps",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(text = "$maxApps apps", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = maxApps.toFloat(),
                onValueChange = { onMaxAppsChanged(it.roundToInt()) },
                onValueChangeFinished = { hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress) },
                valueRange = 2f..5f,
                steps = 2,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DockSizeSetting(size: DockSize, onSizeChanged: (DockSize) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Dock size",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                DockSize.entries.forEachIndexed { index, entry ->
                    SegmentedButton(
                        selected = entry == size,
                        onClick = { onSizeChanged(entry) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = DockSize.entries.size),
                        label = { Text(entry.label) },
                    )
                }
            }
        }
    }
}

// Presentation-only label, same reasoning as ThemePreference.label above.
private val DockSize.label: String
    get() = when (this) {
        DockSize.Small -> "Small"
        DockSize.Medium -> "Medium"
        DockSize.Large -> "Large"
    }

@Composable
private fun ManageAppsEntry(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Manage Apps",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Choose which apps appear in the App Drawer",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun GridColumnsSetting(columns: Int, onColumnsChanged: (Int) -> Unit) {
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
            Text(
                text = "Grid columns",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(text = "$columns columns", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = columns.toFloat(),
                onValueChange = { onColumnsChanged(it.roundToInt()) },
                onValueChangeFinished = { hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress) },
                valueRange = 3f..6f,
                steps = 2,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SoundSettingsContent(
    musicEnabled: Boolean,
    onMusicEnabledChanged: (Boolean) -> Unit,
    musicPlayWhileBrowsingSystems: Boolean,
    onMusicPlayWhileBrowsingSystemsChanged: (Boolean) -> Unit,
    musicPlayWhileBrowsingGames: Boolean,
    onMusicPlayWhileBrowsingGamesChanged: (Boolean) -> Unit,
    musicPlayDuringScreensaver: Boolean,
    onMusicPlayDuringScreensaverChanged: (Boolean) -> Unit,
    musicDuckingMode: MusicDuckingMode,
    onMusicDuckingModeChanged: (MusicDuckingMode) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        MusicEnabledSetting(enabled = musicEnabled, onEnabledChanged = onMusicEnabledChanged)
        if (musicEnabled) {
            MusicPlayWhileBrowsingSystemsSetting(
                enabled = musicPlayWhileBrowsingSystems,
                onEnabledChanged = onMusicPlayWhileBrowsingSystemsChanged,
            )
            MusicPlayWhileBrowsingGamesSetting(
                enabled = musicPlayWhileBrowsingGames,
                onEnabledChanged = onMusicPlayWhileBrowsingGamesChanged,
            )
            MusicPlayDuringScreensaverSetting(
                enabled = musicPlayDuringScreensaver,
                onEnabledChanged = onMusicPlayDuringScreensaverChanged,
            )
            MusicDuckingModeSetting(selected = musicDuckingMode, onSelected = onMusicDuckingModeChanged)
        }
    }
}

@Composable
private fun MusicEnabledSetting(enabled: Boolean, onEnabledChanged: (Boolean) -> Unit) {
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
            Text(
                text = "Background Music",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Enable background music",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onEnabledChanged(it)
                    },
                )
            }
        }
    }
}

@Composable
private fun MusicPlayWhileBrowsingSystemsSetting(enabled: Boolean, onEnabledChanged: (Boolean) -> Unit) {
    val hapticFeedback = LocalHapticFeedback.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Play while browsing systems",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = enabled,
                onCheckedChange = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEnabledChanged(it)
                },
            )
        }
    }
}

@Composable
private fun MusicPlayWhileBrowsingGamesSetting(enabled: Boolean, onEnabledChanged: (Boolean) -> Unit) {
    val hapticFeedback = LocalHapticFeedback.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Play while browsing games",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = enabled,
                onCheckedChange = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEnabledChanged(it)
                },
            )
        }
    }
}

@Composable
private fun MusicPlayDuringScreensaverSetting(enabled: Boolean, onEnabledChanged: (Boolean) -> Unit) {
    val hapticFeedback = LocalHapticFeedback.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Play during screensaver",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = enabled,
                onCheckedChange = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEnabledChanged(it)
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MusicDuckingModeSetting(selected: MusicDuckingMode, onSelected: (MusicDuckingMode) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "During Video Playback",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                MusicDuckingMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = mode == selected,
                        onClick = { onSelected(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = MusicDuckingMode.entries.size,
                        ),
                        icon = {
                            SegmentedButtonDefaults.Icon(active = mode == selected) {
                                Icon(
                                    imageVector = mode.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                                )
                            }
                        },
                        label = { Text(mode.label) },
                    )
                }
            }
        }
    }
}

// Presentation-only icon/label, same reasoning as ThemePreference.icon/label above.
private val MusicDuckingMode.icon: ImageVector
    get() = when (this) {
        MusicDuckingMode.Unchanged -> Icons.Filled.VolumeUp
        MusicDuckingMode.LowerVolume -> Icons.Filled.VolumeDown
        MusicDuckingMode.Pause -> Icons.Filled.Pause
    }

private val MusicDuckingMode.label: String
    get() = when (this) {
        MusicDuckingMode.Unchanged -> "Unchanged"
        MusicDuckingMode.LowerVolume -> "Lower volume"
        MusicDuckingMode.Pause -> "Pause"
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemePicker(selected: ThemePreference, onSelected: (ThemePreference) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ThemePreference.entries.forEachIndexed { index, theme ->
                    SegmentedButton(
                        selected = theme == selected,
                        onClick = { onSelected(theme) },
                        shape = SegmentedButtonDefaults.itemShape(
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
    get() = when (this) {
        ThemePreference.Auto -> Icons.Filled.BrightnessAuto
        ThemePreference.Light -> Icons.Filled.LightMode
        ThemePreference.Dark -> Icons.Filled.DarkMode
    }

private val ThemePreference.label: String
    get() = when (this) {
        ThemePreference.Auto -> "Auto"
        ThemePreference.Light -> "Light"
        ThemePreference.Dark -> "Dark"
    }

@Composable
private fun EditWidgetsEntry(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Edit Widgets",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Add, move, and resize widgets on the main screen",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun FolderSetting(
    label: String,
    path: String,
    isValidating: Boolean,
    statusText: String,
    onPick: (Uri) -> Unit,
) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let(onPick)
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
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = path,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { launcher.launch(null) }) {
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        contentDescription = "Change folder",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (isValidating) {
                CircularProgressIndicator()
            } else {
                Text(text = statusText, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun OptionalFolderSetting(
    label: String,
    path: String?,
    isValidating: Boolean,
    statusText: String,
    onPick: (Uri) -> Unit,
    onClear: () -> Unit,
) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let(onPick)
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
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = path ?: "Not set",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                if (path != null) {
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "Clear folder",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                IconButton(onClick = { launcher.launch(null) }) {
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        contentDescription = "Choose folder",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (path != null) {
                if (isValidating) {
                    CircularProgressIndicator()
                } else {
                    Text(text = statusText, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private fun LogFolderValidation?.toStatusText(): String = when (this) {
    null -> ""
    is LogFolderValidation.FolderNotFound -> "Folder not found"
    is LogFolderValidation.FolderFound ->
        if (settingsFileFound) "settings/es_settings.xml found" else "Folder found, but appears to be the incorrect folder"
}

private fun MediaFolderValidation?.toStatusText(): String = when (this) {
    null -> ""
    is MediaFolderValidation.FolderNotFound -> "Folder not found"
    is MediaFolderValidation.FolderFound -> "Folder found"
}

package com.esde.companion.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.esde.companion.domain.model.MusicDuckingMode
import com.esde.companion.ui.SegmentedButtonLabel

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
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        MusicEnabledSetting(enabled = musicEnabled, onEnabledChanged = onMusicEnabledChanged)
        if (musicEnabled) {
            ToggleSettingRow(
                icon = Icons.Filled.Devices,
                title = "Play while browsing systems",
                description = null,
                enabled = musicPlayWhileBrowsingSystems,
                onEnabledChanged = onMusicPlayWhileBrowsingSystemsChanged,
            )
            ToggleSettingRow(
                icon = Icons.Filled.SportsEsports,
                title = "Play while browsing games",
                description = null,
                enabled = musicPlayWhileBrowsingGames,
                onEnabledChanged = onMusicPlayWhileBrowsingGamesChanged,
            )
            ToggleSettingRow(
                icon = Icons.Filled.Nightlight,
                title = "Play during screensaver",
                description = null,
                enabled = musicPlayDuringScreensaver,
                onEnabledChanged = onMusicPlayDuringScreensaverChanged,
            )
            MusicDuckingModeSetting(selected = musicDuckingMode, onSelected = onMusicDuckingModeChanged)
        }
    }
}

@Composable
private fun MusicEnabledSetting(
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
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
            SettingsLabel(icon = Icons.Filled.MusicNote, text = "Background Music")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MusicDuckingModeSetting(
    selected: MusicDuckingMode,
    onSelected: (MusicDuckingMode) -> Unit,
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
            SettingsLabel(icon = Icons.Filled.Movie, text = "Volume During Video Playback")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                MusicDuckingMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = mode == selected,
                        onClick = { onSelected(mode) },
                        shape =
                            SegmentedButtonDefaults.itemShape(
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
                        label = { SegmentedButtonLabel(mode.label) },
                    )
                }
            }
        }
    }
}

// Presentation-only icon/label, same reasoning as ThemePreference.icon/label above.
private val MusicDuckingMode.icon: ImageVector
    get() =
        when (this) {
            MusicDuckingMode.Unchanged -> Icons.AutoMirrored.Filled.VolumeUp
            MusicDuckingMode.LowerVolume -> Icons.AutoMirrored.Filled.VolumeDown
            MusicDuckingMode.Pause -> Icons.Filled.Pause
        }

private val MusicDuckingMode.label: String
    get() =
        when (this) {
            MusicDuckingMode.Unchanged -> "Unchanged"
            MusicDuckingMode.LowerVolume -> "Lower"
            MusicDuckingMode.Pause -> "Pause"
        }

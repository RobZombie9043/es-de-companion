package com.esde.companion.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

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
        modifier =
            Modifier
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
private fun VideoPlaybackEnabledSetting(
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
            SettingsLabel(icon = Icons.Filled.Videocam, text = "Background Video")
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
private fun VideoDelaySetting(
    delaySeconds: Int,
    onDelaySecondsChanged: (Int) -> Unit,
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
            SettingsLabel(icon = Icons.Filled.Timer, text = "Video Start Delay")
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
private fun VideoAudioSetting(
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
            SettingsLabel(icon = Icons.AutoMirrored.Filled.VolumeUp, text = "Video Audio")
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

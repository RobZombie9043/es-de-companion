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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.esde.companion.domain.model.UpdateCheckResult

@Composable
internal fun OtherSettingsContent(
    updateCheckResult: UpdateCheckResult?,
    isCheckingForUpdate: Boolean,
    onCheckForUpdatesClicked: () -> Unit,
    closeCompanionOnQuitEnabled: Boolean,
    onCloseCompanionOnQuitEnabledChanged: (Boolean) -> Unit,
    launchEsdeOnStartEnabled: Boolean,
    onLaunchEsdeOnStartEnabledChanged: (Boolean) -> Unit,
    debugLoggingEnabled: Boolean,
    onDebugLoggingEnabledChanged: (Boolean) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        CheckForUpdatesRow(updateCheckResult, isCheckingForUpdate, onCheckForUpdatesClicked)
        CloseCompanionOnQuitSetting(
            enabled = closeCompanionOnQuitEnabled,
            onEnabledChanged = onCloseCompanionOnQuitEnabledChanged,
        )
        LaunchEsdeOnStartSetting(
            enabled = launchEsdeOnStartEnabled,
            onEnabledChanged = onLaunchEsdeOnStartEnabledChanged,
        )
        DebugLoggingSetting(
            enabled = debugLoggingEnabled,
            onEnabledChanged = onDebugLoggingEnabledChanged,
        )
    }
}

/**
 * Top item of Other Settings - manual "Check for Updates" entry. [checkResult] is null
 * until the first manual check completes (the silent startup check never sets it - see
 * UpdateUiState's kdoc), in which case no status text is shown yet.
 */
@Composable
internal fun CheckForUpdatesRow(
    checkResult: UpdateCheckResult?,
    isChecking: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsLabel(icon = Icons.Filled.SystemUpdate, text = "Check for Updates")
            when {
                isChecking -> CircularProgressIndicator(modifier = Modifier.size(20.dp))
                checkResult != null -> Text(text = checkResult.statusText(), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun UpdateCheckResult.statusText(): String =
    when (this) {
        is UpdateCheckResult.UpdateAvailable -> "Update available"
        UpdateCheckResult.UpToDate -> "Up to date"
        is UpdateCheckResult.Failed -> "Check failed"
    }

@Composable
private fun CloseCompanionOnQuitSetting(
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
            SettingsLabel(icon = Icons.AutoMirrored.Filled.ExitToApp, text = "Close Companion App on ES-DE Quit")
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
private fun LaunchEsdeOnStartSetting(
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
            SettingsLabel(icon = Icons.Filled.RocketLaunch, text = "Launch ES-DE on Companion App Start")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Launch ES-DE on the other display when Companion App starts",
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
private fun DebugLoggingSetting(
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
            SettingsLabel(icon = Icons.Filled.BugReport, text = "Debug Logging")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Write a debug log to help diagnose reported issues",
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

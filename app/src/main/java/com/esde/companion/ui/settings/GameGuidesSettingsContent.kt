package com.esde.companion.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Settings > Game Guides. Text size lives in the guide viewer itself (its own toolbar's
 * text-size buttons, persisted the same way this used to duplicate) - not repeated here, so
 * there's exactly one place that controls it. Three independent sections, each its own card:
 * [onAddGuideClicked] opens the system -> game -> GameFAQs browser drill-down (Add Guide);
 * [onBrowseDownloadedGuidesClicked] opens the system -> game -> guide drill-down over what's
 * already downloaded (see `DownloadedGuidesScreens.kt`); [onClearAllGuidesClicked] is only
 * actually invoked after the confirmation dialog below.
 */
@Composable
internal fun GameGuidesSettingsContent(
    onAddGuideClicked: () -> Unit,
    onBrowseDownloadedGuidesClicked: () -> Unit,
    onClearAllGuidesClicked: () -> Unit,
    manualFallbackOnNoGuideEnabled: Boolean,
    onManualFallbackOnNoGuideEnabledChanged: (Boolean) -> Unit,
) {
    var showClearAllConfirmation by remember { mutableStateOf(false) }

    val columnModifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    Column(
        modifier = columnModifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        GameGuidesSettingsSection(
            icon = Icons.Filled.Add,
            title = "Add Guide",
            description = "Pick a system and game, then import a file or browse GameFAQs to find one.",
            buttonLabel = "Add Guide",
            onClick = onAddGuideClicked,
        )
        val manualFallbackDescription =
            "When Game Playing Behavior is set to Guide and the current game has no downloaded " +
                "guide, show its manual instead."
        ToggleSettingRow(
            icon = Icons.AutoMirrored.Filled.MenuBook,
            title = "Load game manual on game start when no guide is available",
            description = manualFallbackDescription,
            enabled = manualFallbackOnNoGuideEnabled,
            onEnabledChanged = onManualFallbackOnNoGuideEnabledChanged,
        )
        GameGuidesSettingsSection(
            icon = Icons.Filled.LibraryBooks,
            title = "Downloaded Guides",
            description = "Browse every guide saved for offline reading, organized by system and game.",
            buttonLabel = "Browse Downloaded Guides",
            onClick = onBrowseDownloadedGuidesClicked,
        )
        GameGuidesSettingsSection(
            icon = Icons.Filled.LibraryBooks,
            title = "Clear Storage",
            description = "Removes every guide saved for offline reading, across all games.",
            buttonLabel = "Clear All Downloaded Guides",
            onClick = { showClearAllConfirmation = true },
        )
    }

    if (showClearAllConfirmation) {
        ClearAllGuidesConfirmationDialog(
            onConfirm = {
                showClearAllConfirmation = false
                onClearAllGuidesClicked()
            },
            onDismiss = { showClearAllConfirmation = false },
        )
    }
}

@Composable
private fun ClearAllGuidesConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Clear all downloaded guides?") },
        text = {
            Text("This removes every guide saved for offline reading, across all games. This can't be undone.")
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Clear All") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun GameGuidesSettingsSection(
    icon: ImageVector,
    title: String,
    description: String,
    buttonLabel: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsLabel(icon = icon, text = title)
            Text(text = description, style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = onClick) {
                Text(buttonLabel)
            }
        }
    }
}

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
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Dock
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.esde.companion.domain.model.DockSize
import com.esde.companion.ui.SegmentedButtonLabel
import kotlin.math.roundToInt

@Composable
internal fun AppDrawerSettingsContent(
    gridColumns: Int,
    onGridColumnsChanged: (Int) -> Unit,
    sortFoldersOnTop: Boolean,
    onSortFoldersOnTopChanged: (Boolean) -> Unit,
    showSearchBar: Boolean,
    onShowSearchBarChanged: (Boolean) -> Unit,
    onManageAppsClick: () -> Unit,
    dockEnabled: Boolean,
    onDockEnabledChanged: (Boolean) -> Unit,
    dockMaxApps: Int,
    onDockMaxAppsChanged: (Int) -> Unit,
    dockSize: DockSize,
    onDockSizeChanged: (DockSize) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        ManageAppsEntry(onClick = onManageAppsClick)
        ToggleSettingRow(
            icon = Icons.Filled.Search,
            title = "Show Search Bar",
            description = "Search bar and settings shortcuts at the top of the app drawer",
            enabled = showSearchBar,
            onEnabledChanged = onShowSearchBarChanged,
        )
        GridColumnsSetting(columns = gridColumns, onColumnsChanged = onGridColumnsChanged)
        ToggleSettingRow(
            icon = Icons.AutoMirrored.Filled.Sort,
            title = "Sort folders on top of apps",
            description = "Group folders ahead of ungrouped apps instead of sorting them in alphabetically",
            enabled = sortFoldersOnTop,
            onEnabledChanged = onSortFoldersOnTopChanged,
        )
        ToggleSettingRow(
            icon = Icons.Filled.Dock,
            title = "Enable Dock",
            description = "A row of pinned apps at the bottom of the main screen",
            enabled = dockEnabled,
            onEnabledChanged = onDockEnabledChanged,
        )
        SettingsRowVisibility(visible = dockEnabled) {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                DockMaxAppsSetting(maxApps = dockMaxApps, onMaxAppsChanged = onDockMaxAppsChanged)
                DockSizeSetting(size = dockSize, onSizeChanged = onDockSizeChanged)
            }
        }
    }
}

@Composable
private fun DockMaxAppsSetting(
    maxApps: Int,
    onMaxAppsChanged: (Int) -> Unit,
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
            SettingsLabel(icon = Icons.Filled.FormatListNumbered, text = "Maximum dock apps")
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
private fun DockSizeSetting(
    size: DockSize,
    onSizeChanged: (DockSize) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SettingsItemShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = SETTINGS_PANEL_ALPHA),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SettingsLabel(icon = Icons.Filled.AspectRatio, text = "Dock size")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                DockSize.entries.forEachIndexed { index, entry ->
                    SegmentedButton(
                        selected = entry == size,
                        onClick = { onSizeChanged(entry) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = DockSize.entries.size),
                        label = { SegmentedButtonLabel(entry.label) },
                    )
                }
            }
        }
    }
}

// Presentation-only label, same reasoning as ThemePreference.label above.
private val DockSize.label: String
    get() =
        when (this) {
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
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SettingsLabel(icon = Icons.Filled.Apps, text = "Manage Apps")
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
private fun GridColumnsSetting(
    columns: Int,
    onColumnsChanged: (Int) -> Unit,
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
            SettingsLabel(icon = Icons.Filled.GridView, text = "Grid columns")
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

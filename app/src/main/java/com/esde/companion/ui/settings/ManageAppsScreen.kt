package com.esde.companion.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.esde.companion.data.apps.AppIconLoader

/**
 * Full list of installed apps with a checkbox per row to hide/unhide it from the App
 * Drawer. Checked = visible in the drawer. Toggling applies immediately via
 * [ManageAppsViewModel] - there's no separate save step, matching the rest of Settings.
 */
@Composable
fun ManageAppsScreen(viewModel: ManageAppsViewModel, modifier: Modifier = Modifier) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(rows, key = { it.app.packageName }) { row ->
            ManageAppsRow(
                row = row,
                onToggle = { hidden -> viewModel.onVisibilityToggled(row.app.packageName, hidden) },
            )
        }
    }
}

@Composable
private fun ManageAppsRow(row: AppVisibilityRow, onToggle: (hidden: Boolean) -> Unit) {
    val context = LocalContext.current
    val icon by produceState<Any?>(initialValue = null, key1 = row.app.packageName) {
        value = AppIconLoader.loadIcon(context, row.app.packageName)
    }
    val isVisible = !row.isHidden

    Surface(
        // Tapping anywhere in the row toggles, same as the Checkbox - new hidden state
        // is simply the current visible state (a flip).
        onClick = { onToggle(isVisible) },
        modifier = Modifier.fillMaxWidth(),
        // Matches the same translucency every other settings card uses (see
        // SettingsContent.kt's SETTINGS_PANEL_ALPHA) so this reads as one consistent set
        // of cards with the rest of the settings UI.
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
            )
            Text(
                text = row.app.label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Checkbox(
                checked = isVisible,
                onCheckedChange = { checked -> onToggle(!checked) },
            )
        }
    }
}
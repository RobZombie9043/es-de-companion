package com.esde.companion.ui.thor

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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.esde.companion.data.apps.AppIconLoader

/**
 * Full list of installed apps with a checkbox per row marking it as protected from Task
 * Killer's force-stop - checked = never force-stopped while held BACK targets this app. Same
 * shape as [AutoFpsTriggerAppsScreen] (see that file's kdoc for why this isn't a shared
 * composable - the checkbox semantics differ). Toggling applies immediately via
 * [TaskKillerExcludedAppsViewModel] - there's no separate save step, matching the rest of
 * Settings.
 */
@Composable
fun TaskKillerExcludedAppsScreen(
    viewModel: TaskKillerExcludedAppsViewModel,
    modifier: Modifier = Modifier,
) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(rows, key = { it.app.packageName }) { row ->
            TaskKillerExcludedAppRow(
                row = row,
                onToggle = { isExcluded -> viewModel.onExcludedToggled(row.app.packageName, isExcluded) },
            )
        }
    }
}

@Composable
private fun TaskKillerExcludedAppRow(
    row: TaskKillerExcludedAppRow,
    onToggle: (isExcluded: Boolean) -> Unit,
) {
    val context = LocalContext.current
    val icon by produceState<Any?>(initialValue = null, key1 = row.app.packageName) {
        value = AppIconLoader.loadIcon(context, row.app.packageName)
    }
    val hapticFeedback = LocalHapticFeedback.current
    val rowModifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)

    Surface(
        onClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            onToggle(!row.isExcluded)
        },
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f),
    ) {
        Row(
            modifier = rowModifier,
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
                checked = row.isExcluded,
                onCheckedChange = { checked ->
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggle(checked)
                },
            )
        }
    }
}

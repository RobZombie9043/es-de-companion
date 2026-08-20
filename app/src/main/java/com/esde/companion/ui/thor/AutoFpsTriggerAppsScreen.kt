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
 * Full list of installed apps with a checkbox per row to mark it as an Auto FPS trigger app -
 * checked = entering this app boosts the top screen to 120Hz. Same shape as
 * [com.esde.companion.ui.settings.ManageAppsScreen] (see that file's kdoc for why this isn't
 * a shared composable - the checkbox semantics differ). Toggling applies immediately via
 * [AutoFpsTriggerAppsViewModel] - there's no separate save step, matching the rest of Settings.
 */
@Composable
fun AutoFpsTriggerAppsScreen(
    viewModel: AutoFpsTriggerAppsViewModel,
    modifier: Modifier = Modifier,
) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(rows, key = { it.app.packageName }) { row ->
            AutoFpsTriggerAppRow(
                row = row,
                onToggle = { isTrigger -> viewModel.onTriggerToggled(row.app.packageName, isTrigger) },
            )
        }
    }
}

@Composable
private fun AutoFpsTriggerAppRow(
    row: AutoFpsTriggerAppRow,
    onToggle: (isTrigger: Boolean) -> Unit,
) {
    val context = LocalContext.current
    val icon by produceState<Any?>(initialValue = null, key1 = row.app.packageName) {
        value = AppIconLoader.loadIcon(context, row.app.packageName)
    }
    val hapticFeedback = LocalHapticFeedback.current

    Surface(
        onClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            onToggle(!row.isTrigger)
        },
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f),
    ) {
        val rowModifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
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
                checked = row.isTrigger,
                onCheckedChange = { checked ->
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggle(checked)
                },
            )
        }
    }
}

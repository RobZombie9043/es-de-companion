package com.esde.companion.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class AddGuideMethodOption(
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit,
)

/** Settings > Game Guides > Add Guide's last drill-down page, for the game picked in
 * [AddGuideGamesScreen] - the "+" dropdown's Settings counterpart (see
 * `GameGuideLibraryScreen`'s own Import/GameFAQs menu). [onImportSelected] is expected to be
 * the launcher `rememberGuideImportLauncher` returns, called directly on tap. */
@Composable
fun AddGuideMethodScreen(
    onGameFaqsSelected: () -> Unit,
    onImportSelected: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val options =
        listOf(
            AddGuideMethodOption(
                title = "Import a File",
                subtitle = "Pick a .txt, single-page .html, .pdf, or image file already on this device.",
                onClick = onImportSelected,
            ),
            AddGuideMethodOption(
                title = "Search GameFAQs",
                subtitle = "Browse GameFAQs to find and download a guide for this game.",
                onClick = onGameFaqsSelected,
            ),
        )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(options, key = { it.title }) { option ->
            DrillDownRow(title = option.title, subtitle = option.subtitle, onClick = option.onClick)
        }
    }
}

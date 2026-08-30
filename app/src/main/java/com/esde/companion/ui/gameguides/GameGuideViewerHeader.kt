package com.esde.companion.ui.gameguides

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.esde.companion.domain.model.GameGuideDisplayPreferences

private const val FONT_SCALE_STEP = 0.1f
private const val MIN_FONT_SCALE = 0.6f
private const val MAX_FONT_SCALE = 2.5f

/** [totalPages] > 1 is what actually gates showing [PageNavRow] - a single-page guide
 * (every plain-text guide, or a one-chapter HTML one) always reports 1. */
data class PageNav(
    val currentPageIndex: Int,
    val totalPages: Int,
)

data class HeaderConfig(
    val title: String,
    val isHtml: Boolean,
    val showSearch: Boolean,
    val searchQuery: String,
    val matchTotal: Int,
    val currentMatchIndex: Int,
    val pageNav: PageNav,
)

data class HeaderActions(
    val displayPreferences: GameGuideDisplayPreferences,
    val onDisplayPreferencesChanged: (GameGuideDisplayPreferences) -> Unit,
    val onToggleSearch: () -> Unit,
    val onSearchQueryChanged: (String) -> Unit,
    val onNextMatch: () -> Unit,
    val onShowToc: () -> Unit,
    val onClose: () -> Unit,
    val onPreviousPage: () -> Unit,
    val onNextPage: () -> Unit,
)

/** Bundles the toolbar row plus its conditional page-nav/search bars into one header block. */
@Composable
fun GuideHeader(
    config: HeaderConfig,
    actions: HeaderActions,
) {
    var showMoreMenu by remember { mutableStateOf(false) }

    val toolbarConfig = ToolbarConfig(config.title, actions.displayPreferences, config.isHtml, showMoreMenu)
    val toolbarActions =
        ToolbarActions(
            onDisplayPreferencesChanged = actions.onDisplayPreferencesChanged,
            onShowMoreMenuChanged = { showMoreMenu = it },
            onToggleSearch = actions.onToggleSearch,
            onShowToc = actions.onShowToc,
            onClose = actions.onClose,
        )

    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column {
            GuideViewerToolbar(config = toolbarConfig, actions = toolbarActions)
            if (config.showSearch) {
                SearchBarRow(
                    query = config.searchQuery,
                    onQueryChanged = actions.onSearchQueryChanged,
                    matchTotal = config.matchTotal,
                    currentMatchIndex = config.currentMatchIndex,
                    onNext = actions.onNextMatch,
                )
            }
        }
    }
}

/** The page-turn footer, pinned to the bottom of the screen rather than the header - a saved
 * in-line HTML guide keeps GameFAQs' own per-chapter page structure rather than being
 * flattened into one document (see `GameFaqsBrowserBridge.downloadFullGuide`), so this is how
 * the viewer moves between chapters, the same role a book's page-turn controls play. Only
 * rendered by the caller when [PageNav.totalPages] > 1 - a single-page guide (every plain-
 * text guide, or a one-chapter HTML one) has nothing to page between. */
@Composable
fun GuideFooter(
    pageNav: PageNav,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        PageNavRow(pageNav = pageNav, onPreviousPage = onPreviousPage, onNextPage = onNextPage)
    }
}

@Composable
private fun PageNavRow(
    pageNav: PageNav,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPreviousPage, enabled = pageNav.currentPageIndex > 0) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous page")
        }
        Text(
            text = "Page ${pageNav.currentPageIndex + 1} of ${pageNav.totalPages}",
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )
        IconButton(onClick = onNextPage, enabled = pageNav.currentPageIndex < pageNav.totalPages - 1) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next page")
        }
    }
}

private data class ToolbarConfig(
    val title: String,
    val displayPreferences: GameGuideDisplayPreferences,
    val isHtml: Boolean,
    val showMoreMenu: Boolean,
)

private data class ToolbarActions(
    val onDisplayPreferencesChanged: (GameGuideDisplayPreferences) -> Unit,
    val onShowMoreMenuChanged: (Boolean) -> Unit,
    val onToggleSearch: () -> Unit,
    val onShowToc: () -> Unit,
    val onClose: () -> Unit,
)

@Composable
private fun GuideViewerToolbar(
    config: ToolbarConfig,
    actions: ToolbarActions,
) {
    val displayPreferences = config.displayPreferences
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = config.title,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
        )
        IconButton(onClick = actions.onToggleSearch) {
            Icon(Icons.Filled.Search, contentDescription = "Find in guide")
        }
        IconButton(onClick = actions.onShowToc) {
            Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "Table of contents")
        }
        IconButton(
            onClick = {
                val scale = (displayPreferences.fontScale - FONT_SCALE_STEP).coerceAtLeast(MIN_FONT_SCALE)
                actions.onDisplayPreferencesChanged(displayPreferences.copy(fontScale = scale))
            },
        ) {
            Icon(Icons.Filled.TextDecrease, contentDescription = "Smaller text")
        }
        IconButton(
            onClick = {
                val scale = (displayPreferences.fontScale + FONT_SCALE_STEP).coerceAtMost(MAX_FONT_SCALE)
                actions.onDisplayPreferencesChanged(displayPreferences.copy(fontScale = scale))
            },
        ) {
            Icon(Icons.Filled.TextIncrease, contentDescription = "Larger text")
        }
        if (!config.isHtml) {
            ReflowMenu(config, actions)
        }
        IconButton(onClick = actions.onClose) {
            Icon(Icons.Filled.Close, contentDescription = "Close")
        }
    }
}

@Composable
private fun ReflowMenu(
    config: ToolbarConfig,
    actions: ToolbarActions,
) {
    val displayPreferences = config.displayPreferences
    Box {
        IconButton(onClick = { actions.onShowMoreMenuChanged(true) }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "More options")
        }
        DropdownMenu(expanded = config.showMoreMenu, onDismissRequest = { actions.onShowMoreMenuChanged(false) }) {
            DropdownMenuItem(
                text = { Text("Reflow text") },
                leadingIcon = { if (displayPreferences.reflowEnabled) Icon(Icons.Filled.Check, null) },
                onClick = {
                    val updated = displayPreferences.copy(reflowEnabled = !displayPreferences.reflowEnabled)
                    actions.onDisplayPreferencesChanged(updated)
                    actions.onShowMoreMenuChanged(false)
                },
            )
            DropdownMenuItem(
                text = { Text("Monospace font") },
                leadingIcon = { if (displayPreferences.monospaceFont) Icon(Icons.Filled.Check, null) },
                onClick = {
                    val updated = displayPreferences.copy(monospaceFont = !displayPreferences.monospaceFont)
                    actions.onDisplayPreferencesChanged(updated)
                    actions.onShowMoreMenuChanged(false)
                },
            )
        }
    }
}

@Composable
private fun SearchBarRow(
    query: String,
    onQueryChanged: (String) -> Unit,
    matchTotal: Int,
    currentMatchIndex: Int,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            modifier = Modifier.weight(1f),
            singleLine = true,
            placeholder = { Text("Find in guide") },
        )
        Text(
            text = if (matchTotal > 0) "${currentMatchIndex + 1}/$matchTotal" else "0/0",
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        IconButton(onClick = onNext, enabled = matchTotal > 0) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Next match")
        }
    }
}

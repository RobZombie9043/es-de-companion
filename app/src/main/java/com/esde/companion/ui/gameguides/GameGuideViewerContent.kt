package com.esde.companion.ui.gameguides

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.esde.companion.domain.gameguides.GuideTextReflow
import com.esde.companion.domain.gameguides.PlainTextGuideTocParser
import com.esde.companion.domain.model.GameGuideDisplayPreferences
import com.esde.companion.domain.model.GameGuideFormat
import com.esde.companion.domain.model.GuideTocEntry
import com.esde.companion.ui.theme.LocalIsDarkTheme

private const val TOC_DIALOG_MAX_HEIGHT_DP = 480
private const val TOC_ENTRY_INDENT_DP = 16

data class ViewerDerivedState(
    val isHtml: Boolean,
    val originalText: String,
    val displayedText: String,
    val plainTextMatches: List<IntRange>,
)

@Composable
fun rememberDerivedViewerState(
    state: GameGuidesUiState.Viewing,
    searchQuery: String,
): ViewerDerivedState {
    return remember(state, searchQuery) { buildDerivedViewerState(state, searchQuery) }
}

private fun buildDerivedViewerState(
    state: GameGuidesUiState.Viewing,
    searchQuery: String,
): ViewerDerivedState {
    val isHtml = state.guide.format == GameGuideFormat.Html
    // HTML guides render each page straight from state.currentPageContent (see
    // GuideContentArea's HtmlGuideContent branch, fed via state.currentPage) and never read
    // originalText/displayedText/plainTextMatches - confirmed no reference to any of the
    // three outside this file's own plain-text branches. Skipping the join for HTML avoids
    // joining every chapter's already-multi-megabyte embedded-image HTML into one combined
    // string just to discard it - confirmed crashing an 18-chapter, image-heavy Zelda Dungeon
    // guide with an OutOfMemoryError right here (~58MB single allocation) the moment it was
    // opened.
    if (isHtml) {
        return ViewerDerivedState(isHtml = true, originalText = "", displayedText = "", plainTextMatches = emptyList())
    }
    // PlainText guides are always exactly one page (GameFaqsBrowserBridge's chapter-walking
    // only ever runs for GameGuideFormat.Html), so currentPageContent already IS the whole
    // guide - no join needed.
    val originalText = state.currentPageContent
    val displayedText =
        if (state.displayPreferences.reflowEnabled) GuideTextReflow.reflow(originalText) else originalText
    val plainTextMatches =
        if (searchQuery.isNotBlank()) findAllMatches(displayedText, searchQuery) else emptyList()
    return ViewerDerivedState(isHtml = false, originalText, displayedText, plainTextMatches)
}

/**
 * The guide's table of contents, computed only when actually asked for - call this from
 * inside `if (uiState.showToc) { ... }`, not unconditionally alongside [rememberDerivedViewerState].
 * [PlainTextGuideTocParser.parse] runs several full-text passes and can take a real,
 * user-visible moment on a large guide (confirmed on a ~1MB real guide); computing it eagerly
 * on every guide open - even though the table of contents is only ever shown after an explicit
 * tap - was adding that delay to opening the guide itself, not just to opening the dialog.
 * [GuideTocEntry]s for HTML are already stored on the guide's own metadata (tagged at download
 * time - see `GameFaqsBrowserBridge`), so this is a no-op lookup for that format.
 */
@Composable
fun rememberGuideTocEntries(
    state: GameGuidesUiState.Viewing,
    derived: ViewerDerivedState,
): List<GuideTocEntry> =
    remember(state.guide.tocEntries, derived.isHtml, derived.displayedText) {
        if (derived.isHtml) state.guide.tocEntries else PlainTextGuideTocParser.parse(derived.displayedText)
    }

data class GuideContentState(
    val currentPage: String,
    val displayPreferences: GameGuideDisplayPreferences,
    val initialScrollFraction: Float,
    val showSearch: Boolean,
    val searchQuery: String,
    val htmlFindRequestId: Int,
    val scrollToAnchorId: String?,
    val scrollToCharOffsetRequest: Int?,
)

data class GuideContentActions(
    val onScrollFractionChanged: (Float) -> Unit,
    val onHtmlFindResult: (active: Int, total: Int) -> Unit,
    val onScrollToAnchorHandled: () -> Unit,
    val onScrollToCharOffsetHandled: () -> Unit,
    val onToggleChrome: () -> Unit,
)

internal fun buildGuideContentState(
    state: GameGuidesUiState.Viewing,
    uiState: GuideViewerUiState,
): GuideContentState {
    // Only the page the guide was actually resumed on gets its saved scroll fraction - any
    // other page (reached via next/previous or a table-of-contents jump) starts at the top,
    // the same way opening a different chapter of a book would.
    val isResumedPage = uiState.currentPageIndex == state.initialPageIndex
    val initialScrollFraction = if (isResumedPage) state.initialScrollFraction else 0f
    return GuideContentState(
        currentPage = state.currentPageContent,
        initialScrollFraction = initialScrollFraction,
        displayPreferences = state.displayPreferences,
        showSearch = uiState.showSearch,
        searchQuery = uiState.searchQuery,
        htmlFindRequestId = uiState.htmlFindRequestId,
        scrollToAnchorId = uiState.scrollToAnchorId,
        scrollToCharOffsetRequest = uiState.scrollToCharOffsetRequest,
    )
}

internal fun buildGuideContentActions(
    uiState: GuideViewerUiState,
    onScrollFractionChanged: (pageIndex: Int, fraction: Float) -> Unit,
): GuideContentActions =
    GuideContentActions(
        onScrollFractionChanged = { fraction -> onScrollFractionChanged(uiState.currentPageIndex, fraction) },
        onHtmlFindResult = { active, total ->
            uiState.currentMatchIndex = active
            uiState.htmlMatchTotal = total
        },
        onScrollToAnchorHandled = { uiState.scrollToAnchorId = null },
        onScrollToCharOffsetHandled = { uiState.scrollToCharOffsetRequest = null },
        onToggleChrome = { uiState.chromeVisible = !uiState.chromeVisible },
    )

@Composable
fun GuideContentArea(
    derived: ViewerDerivedState,
    state: GuideContentState,
    actions: GuideContentActions,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    // A tap anywhere on the guide content (header/footer chrome excluded, since those are
    // siblings outside this Box) toggles chromeVisible. Works for the plain-text branch via
    // this ordinary Compose gesture detector - detectTapGestures only claims a tap that isn't
    // also a drag, so LazyColumn's own scroll gesture is unaffected. The embedded WebView in
    // the HTML branch fully owns its own native touch dispatch and would never let a tap
    // reach this Compose-level detector, so that branch instead wires the identical
    // [GuideContentActions.onToggleChrome] callback straight into a native
    // GestureDetector.onSingleTapConfirmed on the WebView itself - see
    // [HtmlViewerCallbacks.onTap].
    val tapModifier =
        modifier.fillMaxSize().pointerInput(Unit) {
            detectTapGestures(onTap = { actions.onToggleChrome() })
        }
    Box(modifier = tapModifier) {
        if (derived.isHtml) {
            val htmlConfig =
                HtmlViewerConfig(
                    html = state.currentPage,
                    fontScale = state.displayPreferences.fontScale,
                    isDarkTheme = LocalIsDarkTheme.current,
                    searchQuery = if (state.showSearch) state.searchQuery else "",
                    findNextRequestId = state.htmlFindRequestId,
                    scrollToAnchorId = state.scrollToAnchorId,
                    initialScrollFraction = state.initialScrollFraction,
                )
            val htmlCallbacks =
                HtmlViewerCallbacks(
                    onFindResult = actions.onHtmlFindResult,
                    onScrollToAnchorHandled = actions.onScrollToAnchorHandled,
                    onScrollFractionChanged = actions.onScrollFractionChanged,
                    onTap = actions.onToggleChrome,
                )
            HtmlGuideContent(config = htmlConfig, callbacks = htmlCallbacks)
        } else {
            val plainTextConfig =
                PlainTextViewerConfig(
                    text = derived.displayedText,
                    matches = derived.plainTextMatches,
                    displayPreferences = state.displayPreferences,
                )
            val scrollControl =
                ScrollControl(
                    listState = listState,
                    initialScrollFraction = state.initialScrollFraction,
                    scrollToCharOffsetRequest = state.scrollToCharOffsetRequest,
                    onScrollToCharOffsetHandled = actions.onScrollToCharOffsetHandled,
                )
            PlainTextGuideContent(
                config = plainTextConfig,
                scroll = scrollControl,
                onScrollFractionChanged = actions.onScrollFractionChanged,
            )
        }
    }
}

@Composable
fun TocDialog(
    entries: List<GuideTocEntry>,
    onEntrySelected: (GuideTocEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Table of Contents") },
        text = {
            if (entries.isEmpty()) {
                Text("No sections found in this guide.")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = TOC_DIALOG_MAX_HEIGHT_DP.dp)) {
                    items(entries) { entry ->
                        val indent = (TOC_ENTRY_INDENT_DP * entry.depth).dp
                        TextButton(onClick = { onEntrySelected(entry) }, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = entry.title,
                                modifier = Modifier.fillMaxWidth().padding(start = indent),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

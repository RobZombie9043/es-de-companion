package com.esde.companion.ui.gameguides

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.esde.companion.domain.model.GameGuideDisplayPreferences
import com.esde.companion.domain.model.GuideTocEntry

@Composable
private fun rememberGuideViewerUiState(initialPageIndex: Int): GuideViewerUiState {
    return remember { GuideViewerUiState(initialPageIndex) }
}

private fun buildHeaderConfig(
    state: GameGuidesUiState.Viewing,
    derived: ViewerDerivedState,
    uiState: GuideViewerUiState,
    matchTotal: Int,
): HeaderConfig =
    HeaderConfig(
        title = state.guide.title,
        isHtml = derived.isHtml,
        showSearch = uiState.showSearch,
        searchQuery = uiState.searchQuery,
        matchTotal = matchTotal,
        currentMatchIndex = uiState.currentMatchIndex,
        pageNav = PageNav(currentPageIndex = uiState.currentPageIndex, totalPages = state.pages.size),
    )

/** [derived] (rather than a plain match-jump callback) keeps this within detekt's
 * LongParameterList limit - [matchTotal]/the "last page" bound both derive from it and
 * [state] rather than needing their own separate parameters too. */
private fun buildHeaderActions(
    state: GameGuidesUiState.Viewing,
    uiState: GuideViewerUiState,
    derived: ViewerDerivedState,
    onDisplayPreferencesChanged: (GameGuideDisplayPreferences) -> Unit,
    onClose: () -> Unit,
): HeaderActions {
    val matchTotal = if (derived.isHtml) uiState.htmlMatchTotal else derived.plainTextMatches.size
    val lastPageIndex = (state.pages.size - 1).coerceAtLeast(0)
    return HeaderActions(
        displayPreferences = state.displayPreferences,
        onDisplayPreferencesChanged = onDisplayPreferencesChanged,
        onToggleSearch = {
            uiState.showSearch = !uiState.showSearch
            if (!uiState.showSearch) uiState.searchQuery = ""
        },
        onSearchQueryChanged = { uiState.searchQuery = it },
        onNextMatch = {
            if (matchTotal > 0) {
                uiState.currentMatchIndex = (uiState.currentMatchIndex + 1).mod(matchTotal)
                jumpToMatch(derived, uiState, uiState.currentMatchIndex)
            }
        },
        onShowToc = { uiState.showToc = true },
        onClose = onClose,
        onPreviousPage = { uiState.currentPageIndex = (uiState.currentPageIndex - 1).coerceAtLeast(0) },
        onNextPage = { uiState.currentPageIndex = (uiState.currentPageIndex + 1).coerceAtMost(lastPageIndex) },
    )
}

private fun jumpToMatch(
    derived: ViewerDerivedState,
    uiState: GuideViewerUiState,
    index: Int,
) {
    if (derived.isHtml) {
        uiState.htmlFindRequestId++
    } else if (derived.plainTextMatches.isNotEmpty()) {
        val target = derived.plainTextMatches[index.mod(derived.plainTextMatches.size)]
        uiState.scrollToFractionRequest = target.first.toFloat() / derived.displayedText.length.coerceAtLeast(1)
    }
}

/**
 * Persists [currentPageIndex] the moment it actually changes (next/previous, a table-of-
 * contents jump), rather than relying solely on the scroll-fraction poll each viewer
 * implementation runs - that poll only fires after roughly a second of dwell time AND a
 * meaningful scroll delta, so quickly flipping through several chapters and closing before
 * either happens left the saved position stuck on an earlier chapter. Skipped on the very
 * first composition, since that "change" is really just the resumed page - a 0f write there
 * would clobber the real resumed scroll fraction before the poll ever runs.
 */
@Composable
private fun PersistPageIndexOnChange(
    currentPageIndex: Int,
    onScrollFractionChanged: (pageIndex: Int, fraction: Float) -> Unit,
) {
    val isFirstComposition = remember { mutableStateOf(true) }
    LaunchedEffect(currentPageIndex) {
        if (isFirstComposition.value) {
            isFirstComposition.value = false
        } else {
            onScrollFractionChanged(currentPageIndex, 0f)
        }
    }
}

@Composable
fun GameGuideViewerScreen(
    state: GameGuidesUiState.Viewing,
    onScrollFractionChanged: (pageIndex: Int, fraction: Float) -> Unit,
    onDisplayPreferencesChanged: (GameGuideDisplayPreferences) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onClose)

    val uiState = rememberGuideViewerUiState(state.initialPageIndex)
    val derived = rememberDerivedViewerState(state, uiState.searchQuery)
    val matchTotal = if (derived.isHtml) uiState.htmlMatchTotal else derived.plainTextMatches.size
    val lastPageIndex = (state.pages.size - 1).coerceAtLeast(0)

    LaunchedEffect(uiState.searchQuery) { uiState.currentMatchIndex = 0 }
    PersistPageIndexOnChange(uiState.currentPageIndex, onScrollFractionChanged)

    fun onEntrySelected(entry: GuideTocEntry) {
        uiState.showToc = false
        if (derived.isHtml) {
            uiState.currentPageIndex = entry.pageIndex.coerceIn(0, lastPageIndex)
            uiState.scrollToAnchorId = entry.anchorId
        } else {
            val offset = entry.anchorId.toIntOrNull() ?: 0
            uiState.scrollToFractionRequest = offset.toFloat() / derived.originalText.length.coerceAtLeast(1)
        }
    }

    val headerConfig = buildHeaderConfig(state, derived, uiState, matchTotal)
    val headerActions = buildHeaderActions(state, uiState, derived, onDisplayPreferencesChanged, onClose)
    val contentState = buildGuideContentState(state, uiState)
    val contentActions = buildGuideContentActions(uiState, onScrollFractionChanged)

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (uiState.chromeVisible) GuideHeader(config = headerConfig, actions = headerActions)
            GuideContentArea(
                derived = derived,
                state = contentState,
                actions = contentActions,
                modifier = Modifier.weight(1f),
            )
            if (uiState.chromeVisible && headerConfig.pageNav.totalPages > 1) {
                GuideFooter(
                    pageNav = headerConfig.pageNav,
                    onPreviousPage = headerActions.onPreviousPage,
                    onNextPage = headerActions.onNextPage,
                )
            }
        }
    }

    if (uiState.showToc) {
        TocDialog(
            entries = derived.tocEntries,
            onEntrySelected = ::onEntrySelected,
            onDismiss = { uiState.showToc = false },
        )
    }
}

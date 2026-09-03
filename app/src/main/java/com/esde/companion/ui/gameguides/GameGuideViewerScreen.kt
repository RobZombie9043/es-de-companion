package com.esde.companion.ui.gameguides

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.esde.companion.domain.model.GameGuideDisplayPreferences
import com.esde.companion.domain.model.GuideTocEntry

/**
 * Keyed on [guideId] + [initialPageIndex], not a bare [remember] - this composable first
 * mounts during the brief "opening" window (see `GameGuidesViewModel.openingViewingStateFor`'s
 * kdoc) where [GameGuidesUiState.Viewing.initialPageIndex] is still its 0 default, before
 * `loadedViewingStateFor` resolves the real resumed page from disk moments later. A bare
 * `remember { GuideViewerUiState(initialPageIndex) }` locks onto that transient 0 forever,
 * leaving [GuideViewerUiState.currentPageIndex] stuck at 0 even once the real resumed page
 * (say, 5) loads - confirmed on-device: the guide list correctly showed 20% progress, but the
 * viewer opened with its own page indicator reading "Page 1" and (via
 * [buildGuideContentState]'s `isResumedPage` check comparing the two, now permanently
 * mismatched) the saved scroll position lost too, resetting to the top of the page. Keying on
 * [initialPageIndex] re-runs the initializer exactly once more when it settles to the real
 * value, and on [guideId] too so switching to a different guide never reuses stale UI state
 * (search query, TOC dialog, etc.) by coincidence of both guides resuming on the same index.
 */
@Composable
private fun rememberGuideViewerUiState(
    guideId: String,
    initialPageIndex: Int,
): GuideViewerUiState {
    return remember(guideId, initialPageIndex) { GuideViewerUiState(initialPageIndex) }
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
        pageNav = PageNav(currentPageIndex = uiState.currentPageIndex, totalPages = state.guide.pageCount),
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
    val lastPageIndex = (state.guide.pageCount - 1).coerceAtLeast(0)
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
        onPreviousPage = {
            uiState.resumeConsumed = true
            uiState.currentPageIndex = (uiState.currentPageIndex - 1).coerceAtLeast(0)
        },
        onNextPage = {
            uiState.resumeConsumed = true
            uiState.currentPageIndex = (uiState.currentPageIndex + 1).coerceAtMost(lastPageIndex)
        },
    )
}

/**
 * Resolves a tapped in-content link's [fragment] (see [HtmlViewerCallbacks.onInternalAnchorTapped])
 * against the guide's own [GuideTocEntry] list, whose `anchorId` is that exact fragment text for
 * an HTML guide (see [GuideTocOutlineExtractor]'s kdoc) - the same lookup a table-of-contents
 * dialog tap already gets for free from [GuideTocEntry.pageIndex] directly. No match (an in-body
 * cross-reference the `.ftoc` outline never listed) falls back to treating it as same-page - a
 * safe no-op via `scrollToAnchorId`'s own `?.` if no element on the current page actually has
 * that id.
 */
private fun onInternalAnchorTapped(
    fragment: String,
    state: GameGuidesUiState.Viewing,
    uiState: GuideViewerUiState,
    lastPageIndex: Int,
) {
    val targetPageIndex =
        state.guide.tocEntries.firstOrNull { it.anchorId == fragment }
            ?.pageIndex
            ?.coerceIn(0, lastPageIndex)
    if (targetPageIndex == null || targetPageIndex == uiState.currentPageIndex) {
        uiState.scrollToAnchorId = fragment
    } else {
        uiState.pendingAnchorId = fragment
        uiState.resumeConsumed = true
        uiState.currentPageIndex = targetPageIndex
    }
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
        uiState.scrollToCharOffsetRequest = target.first
    }
}

/**
 * Persists [currentPageIndex] the moment it actually changes (next/previous, a table-of-
 * contents jump), rather than relying solely on the scroll-fraction poll each viewer
 * implementation runs - that poll only fires after roughly a second of dwell time AND a
 * meaningful scroll delta, so quickly flipping through several chapters and closing before
 * either happens left the saved position stuck on an earlier chapter. Skipped on the very
 * first composition of [resetKey], since that "change" is really just the resumed page - a 0f
 * write there would clobber the real resumed scroll fraction before the poll ever runs.
 *
 * [resetKey] must be [rememberGuideViewerUiState]'s own `uiState` instance, not just
 * [currentPageIndex] read some other way - that `GuideViewerUiState` is itself re-created once
 * [GameGuidesUiState.Viewing.initialPageIndex] settles from its transient 0 default to the
 * real resumed page (see that composable's kdoc), which changes `currentPageIndex` too. Without
 * re-arming this "skip the first change" guard on the same trigger, that settling was
 * mistaken for a real user navigation and clobbered the just-restored scroll fraction with 0f
 * - confirmed on-device as a guide's saved position resetting to the top of the page.
 */
@Composable
private fun PersistPageIndexOnChange(
    resetKey: Any,
    currentPageIndex: Int,
    onScrollFractionChanged: (pageIndex: Int, fraction: Float) -> Unit,
) {
    val isFirstComposition = remember(resetKey) { mutableStateOf(true) }
    LaunchedEffect(currentPageIndex) {
        if (isFirstComposition.value) {
            isFirstComposition.value = false
        } else {
            onScrollFractionChanged(currentPageIndex, 0f)
        }
    }
}

/**
 * Loads [currentPageIndex]'s content the moment it actually changes (next/previous, a table-
 * of-contents jump to a different page) - see [GameGuidesUiState.Viewing]'s kdoc for why a
 * page's content isn't already sitting in memory the way it used to be. Skipped on the very
 * first composition of [resetKey], same reasoning as [PersistPageIndexOnChange] - see its
 * kdoc for why [resetKey] must be `uiState` itself, not [currentPageIndex].
 */
@Composable
private fun LoadPageOnChange(
    resetKey: Any,
    currentPageIndex: Int,
    onPageChanged: (pageIndex: Int) -> Unit,
) {
    val isFirstComposition = remember(resetKey) { mutableStateOf(true) }
    LaunchedEffect(currentPageIndex) {
        if (isFirstComposition.value) {
            isFirstComposition.value = false
        } else {
            onPageChanged(currentPageIndex)
        }
    }
}

/**
 * Promotes [GuideViewerUiState.pendingAnchorId] to [GuideViewerUiState.scrollToAnchorId] the
 * moment [isLoadingContent] goes back to false - i.e. once the page a cross-page TOC jump
 * navigated to has actually finished loading its content. Declared in this composable (which
 * survives the isLoadingContent-driven unmount/remount of [GuideContentArea]/[HtmlGuideContent]
 * below it), not inside [HtmlGuideContent] itself, specifically so it isn't torn down by that
 * same unmount before it can act - see [GameGuideViewerScreen.onEntrySelected]'s kdoc for the
 * full race this exists to avoid. A no-op whenever nothing is actually pending (a same-page
 * entry, or [isLoadingContent] flipping for an unrelated reason).
 */
@Composable
private fun PromotePendingAnchorWhenLoaded(
    uiState: GuideViewerUiState,
    isLoadingContent: Boolean,
) {
    LaunchedEffect(isLoadingContent) {
        if (isLoadingContent) return@LaunchedEffect
        val anchorId = uiState.pendingAnchorId ?: return@LaunchedEffect
        uiState.pendingAnchorId = null
        uiState.scrollToAnchorId = anchorId
    }
}

/** Bundles [GameGuideViewerScreen]'s own external callbacks into one parameter - same
 * LongParameterList-avoidance reasoning as [HeaderActions]/[GuideContentActions] below,
 * needed once [onPageChanged] joined the other three (see [GameGuidesUiState.Viewing]'s kdoc
 * for why loading a page is now its own callback rather than already-available data). */
data class GuideViewerActions(
    val onScrollFractionChanged: (pageIndex: Int, fraction: Float) -> Unit,
    val onDisplayPreferencesChanged: (GameGuideDisplayPreferences) -> Unit,
    val onPageChanged: (pageIndex: Int) -> Unit,
    val onClose: () -> Unit,
)

@Composable
fun GameGuideViewerScreen(
    state: GameGuidesUiState.Viewing,
    actions: GuideViewerActions,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = actions.onClose)

    val uiState = rememberGuideViewerUiState(state.guide.id, state.initialPageIndex)
    val derived = rememberDerivedViewerState(state, uiState.searchQuery)
    val matchTotal = if (derived.isHtml) uiState.htmlMatchTotal else derived.plainTextMatches.size
    val lastPageIndex = (state.guide.pageCount - 1).coerceAtLeast(0)

    LaunchedEffect(uiState.searchQuery) { uiState.currentMatchIndex = 0 }
    PersistPageIndexOnChange(uiState, uiState.currentPageIndex, actions.onScrollFractionChanged)
    LoadPageOnChange(uiState, uiState.currentPageIndex, actions.onPageChanged)
    PromotePendingAnchorWhenLoaded(uiState, state.isLoadingContent)

    // A same-page entry can set scrollToAnchorId directly - HtmlGuideContent's own WebView
    // already has the right page loaded, so there's nothing to wait for. A cross-page entry
    // instead stages the anchor in pendingAnchorId and lets currentPageIndex's own change
    // trigger the real navigation - confirmed on-device (via temporary diagnostic logging) that
    // setting both here in the same synchronous step raced that navigation and always lost:
    // currentPageIndex's change only reaches LoadPageOnChange's effect, then this screen's
    // ViewModel, then (asynchronously) HtmlGuideContent's own page-loading effect, while
    // scrollToAnchorId's effect - reading state that hadn't moved yet - saw its own target
    // page's content as "already loaded" (technically true: nothing had changed yet) and ran
    // immediately, scrolling against whatever page was still actually displayed. See
    // PromotePendingAnchorWhenLoaded for the other half of this fix.
    fun onEntrySelected(entry: GuideTocEntry) {
        uiState.showToc = false
        if (derived.isHtml) {
            // entry.anchorId is null for a chapter-level entry (see GuideTocEntry's kdoc) -
            // switching currentPageIndex (or, for a same-page chapter entry, doing nothing at
            // all) is the whole jump then, with no in-page position to also scroll to.
            val targetPageIndex = entry.pageIndex.coerceIn(0, lastPageIndex)
            if (targetPageIndex == uiState.currentPageIndex) {
                entry.anchorId?.let { uiState.scrollToAnchorId = it }
            } else {
                entry.anchorId?.let { uiState.pendingAnchorId = it }
                uiState.resumeConsumed = true
                uiState.currentPageIndex = targetPageIndex
            }
        } else {
            uiState.scrollToCharOffsetRequest = entry.anchorId?.toIntOrNull() ?: 0
        }
    }

    val headerConfig = buildHeaderConfig(state, derived, uiState, matchTotal)
    val headerActions =
        buildHeaderActions(state, uiState, derived, actions.onDisplayPreferencesChanged, actions.onClose)
    val contentState = buildGuideContentState(state, uiState)
    val loadingState =
        rememberGuideContentLoadingState(
            state = state,
            uiState = uiState,
            derived = derived,
            onScrollFractionChanged = actions.onScrollFractionChanged,
            onInternalAnchorTapped = { fragment -> onInternalAnchorTapped(fragment, state, uiState, lastPageIndex) },
        )

    // AnimatedVisibility, not a plain if - the underlying reliability issue that motivated
    // *not* animating this (see git history: overlay/layer-toggle attempts, all reverted) has
    // since been root-caused to a stale-callback bug (HtmlGuideContent's WebView listeners
    // capturing the pre-swap GuideViewerUiState via a keyless remember - see that file's kdoc),
    // now fixed via rememberUpdatedState - not the WebView resize/repaint theory those attempts
    // were chasing. Column siblings still resize the content area (unchanged, proven behavior),
    // just gradually over the animation instead of instantly.
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(visible = uiState.chromeVisible, enter = fadeIn(), exit = fadeOut()) {
                GuideHeader(config = headerConfig, actions = headerActions)
            }
            GuideContentWithLoadingOverlay(
                derived = derived,
                contentState = contentState,
                contentActions = loadingState.contentActions,
                indicators = loadingState.indicators,
                modifier = Modifier.weight(1f),
            )
            AnimatedVisibility(
                visible = uiState.chromeVisible && headerConfig.pageNav.totalPages > 1,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                GuideFooter(
                    pageNav = headerConfig.pageNav,
                    onPreviousPage = headerActions.onPreviousPage,
                    onNextPage = headerActions.onNextPage,
                )
            }
        }
    }

    if (uiState.showToc) {
        val tocEntries = rememberGuideTocEntries(state, derived)
        TocDialog(
            entries = tocEntries,
            onEntrySelected = ::onEntrySelected,
            onDismiss = { uiState.showToc = false },
        )
    }
}

/**
 * The two independent loading signals [GuideContentWithLoadingOverlay] renders - bundled into
 * one type purely to keep that composable's own parameter count under detekt's threshold, not
 * because the two are conceptually linked (see their own call-site kdocs for why they're
 * deliberately rendered differently).
 */
internal data class GuideLoadingIndicators(
    val showFullScreen: Boolean,
    val showAnchorJump: Boolean,
)

/**
 * [GuideContentArea] stays mounted regardless of [GuideLoadingIndicators.showFullScreen]
 * (rather than being swapped out while true) so [HtmlGuideContent]'s own content-visible effect
 * can actually run and report back once ready - see [rememberGuideContentLoadingState]'s kdoc
 * for why. The spinner overlay just covers whatever's underneath (a blank first page, or the
 * previous page for the one frame before its own `contentVisible` drops back to false) with a
 * solid background until then, extracted here purely to keep [GameGuideViewerScreen] itself
 * under detekt's length/complexity thresholds.
 */
@Composable
private fun GuideContentWithLoadingOverlay(
    derived: ViewerDerivedState,
    contentState: GuideContentState,
    contentActions: GuideContentActions,
    indicators: GuideLoadingIndicators,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        GuideContentArea(
            derived = derived,
            state = contentState,
            actions = contentActions,
            modifier = Modifier.fillMaxSize(),
        )
        // A pure Compose overlay Box - never touches the WebView's own bounds/sizing, so
        // (unlike the chrome header/footer toggle) fading it carries none of that risk.
        AnimatedVisibility(visible = indicators.showFullScreen, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        // Deliberately small and non-obscuring, unlike showFullScreen's overlay above - a
        // same-page TOC jump has a fully valid page already on screen (just about to scroll
        // once its target's images finish loading), so covering it entirely would hide content
        // the user was already reading instead of just signaling a wait.
        AnimatedVisibility(
            visible = indicators.showAnchorJump,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        }
    }
}

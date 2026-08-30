package com.esde.companion.ui.gameguides

import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esde.companion.data.gameguides.GameFaqsBrowserBridge
import com.esde.companion.domain.gameguides.GuideDownloadProgress
import com.esde.companion.domain.model.DownloadedGameGuide
import com.esde.companion.domain.model.EsdeConnectionState
import com.esde.companion.domain.model.GameGuideDisplayPreferences
import com.esde.companion.domain.model.GameGuideReadingProgress
import com.esde.companion.domain.model.GameReference
import com.esde.companion.domain.model.currentGameName
import com.esde.companion.domain.model.currentGameReference
import com.esde.companion.domain.usecase.ObserveConnectionStateUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.time.Clock

/**
 * Drives the Game Guides FAB overlay: shows the downloaded-guide Library for the current
 * game if any exist, otherwise the GameFAQs Browser to find one. Game context follows the
 * same [ObserveConnectionStateUseCase] -> `currentGameReference()` plumbing
 * `GameManualViewModel` uses to resolve per-game media.
 */
class GameGuidesViewModel(
    observeConnectionState: ObserveConnectionStateUseCase,
    private val useCases: GameGuidesUseCases,
    private val browserBridge: GameFaqsBrowserBridge = GameFaqsBrowserBridge(),
    private val clock: Clock = Clock.systemUTC(),
) : ViewModel() {
    // Eagerly shared, not WhileSubscribed - open()/openBrowser()/saveCurrentGuide() all read
    // currentGame.value imperatively rather than only ever observing it reactively, so this
    // must stay live even during a brief gap with no active UI collector (e.g. between
    // recompositions), not just while something happens to be subscribed.
    private val currentGame: StateFlow<Pair<GameReference, String>?> =
        observeConnectionState()
            .map { connection ->
                val appState = (connection as? EsdeConnectionState.Connected)?.appState ?: return@map null
                val reference = appState.currentGameReference() ?: return@map null
                reference to (appState.currentGameName() ?: "")
            }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** True whenever there's a current game to show guides for - drives the FAB's own visibility. */
    val hasCurrentGame: StateFlow<Boolean> =
        currentGame
            .map { it != null }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _uiState = MutableStateFlow<GameGuidesUiState>(GameGuidesUiState.NoGame)
    val uiState: StateFlow<GameGuidesUiState> = _uiState

    /** Reopens to the Library/Browser for whichever game is current right now - called when
     * the FAB is tapped, when a FAB-opened Viewer is closed (back to that game's Library),
     * and after deleting a guide. */
    fun open() {
        viewModelScope.launch {
            val (reference, name) =
                currentGame.value ?: run {
                    _uiState.value = GameGuidesUiState.NoGame
                    return@launch
                }
            _uiState.value = libraryOrBrowsingState(useCases, reference, name)
        }
    }

    fun openBrowser() {
        val (reference, name) = currentGame.value ?: return
        _uiState.value = browsingStateFor(reference, name)
    }

    /** Same as [openBrowser], but for an explicitly-picked game rather than ES-DE's current
     * one - Settings > Game Guides > Add Guide, so that flow can reuse this exact FAB-driven
     * overlay/viewer instead of a separate, Settings-contained copy of it. */
    fun openBrowserFor(
        reference: GameReference,
        name: String,
    ) {
        _uiState.value = browsingStateFor(reference, name)
    }

    /** Called after every page load in the Browser's WebView to refresh whether Save should show. */
    fun onBrowserPageLoaded(webView: WebView) {
        viewModelScope.launch {
            val browsing = _uiState.value as? GameGuidesUiState.Browsing ?: return@launch
            val page = browserBridge.detectGuidePage(webView)
            _uiState.value = browsing.copy(currentPageIsGuide = page.isGuidePage)
        }
    }

    fun saveCurrentGuide(
        webView: WebView,
        sourceUrl: String,
    ) {
        val browsing = _uiState.value as? GameGuidesUiState.Browsing ?: return
        _uiState.value = browsing.copy(downloadProgress = GuideDownloadProgress.LoadingPage(1, 1))
        viewModelScope.launch {
            // GameFaqsBrowserBridge bounds its own slow steps (each chapter-page navigation,
            // image embedding) with timeouts, but this still needs a hard guarantee that the
            // Save dialog clears even if something else in this block throws - otherwise
            // downloadProgress is stuck non-null forever with no way for the UI to recover.
            try {
                downloadAndSaveGuide(
                    deps = GuideDownloadDeps(browserBridge, useCases.saveGameGuide, clock),
                    webView = webView,
                    sourceUrl = sourceUrl,
                    target = GuideSaveTarget(browsing.gameReference, browsing.gameName),
                    onProgress = { progress ->
                        (_uiState.value as? GameGuidesUiState.Browsing)?.let { current ->
                            _uiState.value = current.copy(downloadProgress = progress)
                        }
                    },
                )
            } finally {
                _uiState.value = libraryOrBrowsingState(useCases, browsing.gameReference, browsing.gameName)
            }
        }
    }

    /**
     * Shows the viewer immediately (header/chrome, resumed page position already known)
     * before the guide's actual page content has loaded - see [openingViewingStateFor]'s
     * kdoc for why. A guide can never be re-opened while another is still loading (opening
     * one already navigates away from the guide list this is invoked from), so there's
     * nothing to race against once [loadedViewingStateFor] finishes.
     */
    fun openGuide(guide: DownloadedGameGuide) {
        viewModelScope.launch {
            val opening = openingViewingStateFor(useCases, guide)
            _uiState.value = opening
            _uiState.value = loadedViewingStateFor(useCases, opening, clock)
        }
    }

    /**
     * Settings > UI Settings > Game Playing Screen Behavior > Guide: jumps straight to
     * whichever of the current game's downloaded guides was interacted with most recently,
     * resumed at its own last position, skipping the Library picker entirely. Returns false
     * (and shows nothing) when there's no current game or it has no downloaded guides - the
     * caller decides what that means for the rest of the screen (see
     * GameGuidesOverlayState's auto-trigger).
     */
    suspend fun autoOpenLastViewedGuideForCurrentGame(): Boolean {
        val reference = currentGame.value?.first
        val mostRecent = reference?.let { mostRecentlyViewedGuide(useCases, it) }
        if (mostRecent != null) {
            val opening = openingViewingStateFor(useCases, mostRecent)
            _uiState.value = opening
            _uiState.value = loadedViewingStateFor(useCases, opening, clock)
        }
        return mostRecent != null
    }

    fun onReadingPositionChanged(
        guideId: String,
        pageIndex: Int,
        fraction: Float,
    ) {
        viewModelScope.launch {
            useCases.setReadingProgress(GameGuideReadingProgress(guideId, fraction, clock.millis(), pageIndex))
        }
    }

    /** Takes a full [GameGuideDisplayPreferences], not just a changed field - the caller
     * builds it via `.copy(...)` off the currently-showing guide's own preferences, so a
     * font-scale change doesn't clobber an independently-set reflow/monospace preference
     * (or vice versa) the way persisting a fresh default-valued instance would. */
    fun onDisplayPreferencesChanged(preferences: GameGuideDisplayPreferences) {
        viewModelScope.launch {
            useCases.setDisplayPreferences(preferences)
            val viewing = _uiState.value as? GameGuidesUiState.Viewing ?: return@launch
            _uiState.value = viewing.copy(displayPreferences = preferences)
        }
    }

    fun deleteGuide(guideId: String) {
        viewModelScope.launch {
            useCases.deleteGameGuide(guideId)
            open()
        }
    }
}

/**
 * The initial Viewing state for [guide] - resumed scroll fraction/preferences already known
 * (both fast DataStore reads), but [GameGuidesUiState.Viewing.pages] empty and
 * [GameGuidesUiState.Viewing.isLoadingContent] true. Shown immediately, before the guide's
 * actual page content (a real, disk-bound file read - see `FileGameGuideLibraryRepository`)
 * has loaded, the same way [HtmlGuideContent] shows its header/chrome before its WebView has
 * finished loading - confirmed on a large real guide (~1MB of plain text) that loading its
 * content synchronously before showing anything left the tap feeling like it hadn't
 * registered at all. See [loadedViewingStateFor] for the second half of this two-step open.
 */
internal suspend fun openingViewingStateFor(
    useCases: GameGuidesUseCases,
    guide: DownloadedGameGuide,
): GameGuidesUiState.Viewing {
    val progress = useCases.observeReadingProgress(guide.id).first()
    val preferences = useCases.observeDisplayPreferences().first()
    return GameGuidesUiState.Viewing(
        guide = guide,
        pages = emptyList(),
        displayPreferences = preferences,
        initialScrollFraction = progress?.scrollFraction ?: 0f,
        isLoadingContent = true,
    )
}

/**
 * Loads [opening]'s real page content and returns the fully-resolved Viewing state - see
 * [openingViewingStateFor]'s kdoc for why this is a separate second step. Also bumps
 * [GameGuideReadingProgress.lastOpenedAtMillis] to now, immediately - not only once the
 * reading position later changes (see [GameGuidesViewModel.onReadingPositionChanged]).
 * Without this, a guide that's opened but never scrolled far enough to cross that other
 * update's own debounce/threshold never advanced its own "last viewed" timestamp, so
 * [mostRecentlyViewedGuide] compared it against OTHER guides' plain
 * [DownloadedGameGuide.downloadedAtMillis] fallback instead - meaning a guide simply
 * downloaded more recently could wrongly outrank one actually opened more recently,
 * confirmed as "always opens the last downloaded guide, not the last opened one."
 */
internal suspend fun loadedViewingStateFor(
    useCases: GameGuidesUseCases,
    opening: GameGuidesUiState.Viewing,
    clock: Clock,
): GameGuidesUiState.Viewing {
    val guide = opening.guide
    val pages = useCases.loadGameGuideContent(guide.id) ?: emptyList()
    val progress = useCases.observeReadingProgress(guide.id).first()
    val maxPageIndex = (pages.size - 1).coerceAtLeast(0)
    // Coerced in case a stale progress record (saved against a since-re-downloaded,
    // differently-paginated copy of this guide) points past the end of the pages actually on
    // disk now.
    val pageIndex = (progress?.pageIndex ?: 0).coerceIn(0, maxPageIndex)
    useCases.setReadingProgress(
        GameGuideReadingProgress(guide.id, opening.initialScrollFraction, clock.millis(), pageIndex),
    )
    return opening.copy(pages = pages, initialPageIndex = pageIndex, isLoadingContent = false)
}

/** Library-or-Browsing state for [reference]/[name] - Library when at least one guide is
 * already downloaded for this game, Browsing (a fresh GameFAQs search) otherwise. Shared by
 * [GameGuidesViewModel.open] and [GameGuidesViewModel.saveCurrentGuide]'s post-save refresh. */
private suspend fun libraryOrBrowsingState(
    useCases: GameGuidesUseCases,
    reference: GameReference,
    name: String,
): GameGuidesUiState {
    val guides = useCases.observeGameGuides(reference).first()
    return if (guides.isNotEmpty()) {
        val progressByGuideId = guides.associate { guide -> guide.id to readOverallProgressFraction(useCases, guide) }
        GameGuidesUiState.Library(reference, name, guides, progressByGuideId)
    } else {
        browsingStateFor(reference, name)
    }
}

/** The current game's downloaded guide most recently interacted with (by
 * [GameGuideReadingProgress.lastOpenedAtMillis], which every scroll/page-change update
 * refreshes - see [GameGuidesViewModel.onReadingPositionChanged] - so it doubles as "last
 * viewed guide" with no separate tracking needed). A guide with no progress yet (downloaded
 * but never opened) falls back to [DownloadedGameGuide.downloadedAtMillis] so a single
 * freshly-downloaded guide can still be picked over nothing. */
private suspend fun mostRecentlyViewedGuide(
    useCases: GameGuidesUseCases,
    reference: GameReference,
): DownloadedGameGuide? =
    useCases.observeGameGuides(reference).first().maxByOrNull { guide ->
        useCases.observeReadingProgress(guide.id).first()?.lastOpenedAtMillis ?: guide.downloadedAtMillis
    }

/** Overall 0f-1f "percent read" across all of [guide]'s saved pages - a single-page guide
 * (plain-text, or a one-chapter HTML guide) is just its own scroll fraction; a multi-page
 * HTML guide blends in how many whole chapters are already past. */
private suspend fun readOverallProgressFraction(
    useCases: GameGuidesUseCases,
    guide: DownloadedGameGuide,
): Float {
    val progress = useCases.observeReadingProgress(guide.id).first() ?: return 0f
    val pageCount = guide.pageCount.coerceAtLeast(1)
    return ((progress.pageIndex + progress.scrollFraction) / pageCount).coerceIn(0f, 1f)
}

private fun browsingStateFor(
    reference: GameReference,
    name: String,
) = GameGuidesUiState.Browsing(
    gameReference = reference,
    gameName = name,
    searchUrl = GAMEFAQS_SEARCH_URL + URLEncoder.encode(name, "UTF-8"),
    currentPageIsGuide = false,
)

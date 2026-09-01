package com.esde.companion.ui.gameguides

import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esde.companion.data.gameguides.GameFaqsBrowserBridge
import com.esde.companion.domain.gameguides.GuideDownloadProgress
import com.esde.companion.domain.model.DownloadedGameGuide
import com.esde.companion.domain.model.EsdeConnectionState
import com.esde.companion.domain.model.GameGuideDisplayPreferences
import com.esde.companion.domain.model.GameGuideFormat
import com.esde.companion.domain.model.GameGuideReadingProgress
import com.esde.companion.domain.model.GameReference
import com.esde.companion.domain.model.MediaType
import com.esde.companion.domain.model.currentGameName
import com.esde.companion.domain.model.currentGameReference
import com.esde.companion.domain.model.resolveScreensaverAwareGame
import com.esde.companion.domain.repository.GuidePageContent
import com.esde.companion.domain.usecase.ObserveConnectionStateUseCase
import com.esde.companion.domain.usecase.ObserveScreensaverAwareContextUseCase
import com.esde.companion.domain.usecase.ObserveUpdateGameGuidesOnScreensaverEnabledUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.time.Clock

/**
 * Drives the Game Guides FAB overlay: [open] always shows the current game's Library
 * (downloaded guides, plus that game's manual if one resolves - see [libraryStateFor]) -
 * reaching the GameFAQs Browser is only ever a deliberate choice via [openBrowser]/
 * [openBrowserFor] (the "+" dropdown's items), never an automatic fallback for an empty
 * Library. Game context follows the same [ObserveConnectionStateUseCase] ->
 * `currentGameReference()` plumbing `GameManualViewModel` uses to resolve per-game media.
 *
 * Manual resolution here deliberately calls [GameGuidesUseCases.resolveGameMedia] directly
 * rather than reusing `GameManualViewModel`, since a Library shown here can be for an
 * explicitly-picked game (Settings > Game Guides > Add Guide, via [openBrowserFor]/
 * [importGuideFor]) that isn't ES-DE's live current game - `GameManualViewModel.pdfPath`
 * only ever resolves the live one.
 *
 * [currentGame] deliberately stays a plain live `map` - the same one this class used before
 * "Update on Screensaver" existed - rather than being routed through
 * [resolveScreensaverAwareGame] itself: it's what [hasCurrentGame] (the FAB's own visibility)
 * and every imperative call site below (`open()`/`openBrowser()`/`importGuideFor()`/etc.) reads,
 * so keeping it simple and independent of the toggle/visibility machinery means a problem in
 * that machinery can never take the FAB down with it. The screensaver-hold/live-follow behavior
 * is instead layered on top, additively, by the [init] block's own collector below - see its
 * comment for why.
 */
@Suppress("TooManyFunctions")
class GameGuidesViewModel(
    observeConnectionState: ObserveConnectionStateUseCase,
    observeUpdateGameGuidesOnScreensaverEnabled: ObserveUpdateGameGuidesOnScreensaverEnabledUseCase,
    private val useCases: GameGuidesUseCases,
    private val browserBridge: GameFaqsBrowserBridge = GameFaqsBrowserBridge(),
    private val clock: Clock = Clock.systemUTC(),
) : ViewModel() {
    // Set by MainActivity via onOverlayVisibilityChanged, reflecting whether the Library
    // screen is actually on screen right now (its AnimatedVisibility condition) - see
    // resolveScreensaverAwareGame's kdoc for why the screensaver-hold logic needs this.
    private val overlayVisible = MutableStateFlow(false)

    fun onOverlayVisibilityChanged(visible: Boolean) {
        overlayVisible.value = visible
    }

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

    // Keeps an already-open, visible Library following the current game - e.g. scrolling
    // ES-DE's game list while the Library is showing updates it to the newly-highlighted game -
    // and, per resolveScreensaverAwareGame, holds it during a screensaver instead when Settings
    // > Game Guides > "Update on Screensaver" is off. Computes its own resolved value directly
    // (rather than calling open(), which reads the plain ungated [currentGame] above) so a
    // fresh FAB tap always shows what's live right now, and only an already-open, already-
    // visible Library gets the hold treatment - matching resolveScreensaverAwareGame's own
    // [visible] contract. distinctUntilChanged/drop(1) mean only a genuine change after this
    // collector starts triggers a refresh; gated on overlayVisible so this does no work while
    // the overlay is closed, and on uiState being Library so a live game change never
    // interrupts someone actively reading a guide (Viewing) or browsing GameFAQs (Browsing).
    //
    // Deliberately never reacts to a null (no-game) resolution by clearing the Library to
    // NoGame - there was no such reactive-clear behavior before this feature existed, and doing
    // so is actively harmful here: ES-DE fires screensaver-start before screensaver-game-select,
    // so with the toggle on there's a real, if brief, window where the live resolution is
    // legitimately "no game yet" mid-screensaver. Reacting to that by flipping to NoGame would
    // then permanently block this same collector's own uiState-is-Library guard above from ever
    // firing again for the real screensaver-game-select that follows moments later - confirmed
    // as the actual cause of a reported "the Library stops following/updating" regression.
    // Skipping a null resolution just leaves the previous game showing until a real one arrives.
    init {
        viewModelScope.launch {
            val screensaverAwareGame =
                ObserveScreensaverAwareContextUseCase(
                    observeConnectionState,
                    observeUpdateGameGuidesOnScreensaverEnabled::invoke,
                    { overlayVisible },
                    ::resolveScreensaverAwareGame,
                )()
            screensaverAwareGame.drop(1).distinctUntilChanged().collect { resolved ->
                if (!overlayVisible.value || _uiState.value !is GameGuidesUiState.Library) return@collect
                val (reference, name) = resolved ?: return@collect
                _uiState.value = libraryStateFor(useCases, reference, name)
            }
        }
    }

    /** Tracks [loadPage]'s in-flight coroutine so a new page request can cancel a stale one -
     * see [loadPage]'s kdoc. */
    private var pageLoadJob: Job? = null

    /** Tracks [saveCurrentGuide]'s in-flight coroutine so [cancelDownload] can stop it - see
     * [cancelDownload]'s kdoc. */
    private var downloadJob: Job? = null

    /** Reopens to the Library for whichever game is current right now - called when the FAB
     * is tapped, when a FAB-opened Viewer is closed (back to that game's Library), and after
     * deleting a guide/importing one. */
    fun open() {
        viewModelScope.launch {
            val (reference, name) =
                currentGame.value ?: run {
                    _uiState.value = GameGuidesUiState.NoGame
                    return@launch
                }
            _uiState.value = libraryStateFor(useCases, reference, name)
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

    /** Imports a picked file as a new guide for [reference] - the "+" dropdown's Import item,
     * for either the current game (the overlay's own Library) or an explicitly-picked one
     * (Settings > Game Guides > Add Guide). [format] is [GameGuideFormat.PlainText]/[GameGuideFormat.Html]
     * for a text-based import (saved through the same page-list pipeline a GameFAQs download
     * uses) or [GameGuideFormat.Pdf]/[GameGuideFormat.Image] for a binary one (saved as a
     * single raw file - see [GameGuidesUseCases.importGameGuide]). Refreshes to this game's
     * Library once saved, same as [saveCurrentGuide]'s post-save refresh. */
    fun importGuideFor(
        reference: GameReference,
        name: String,
        bytes: ByteArray,
        fileName: String,
        format: GameGuideFormat,
    ) {
        viewModelScope.launch {
            val guide = buildImportedGuide(reference, fileName, format, clock.millis())
            when (format) {
                GameGuideFormat.PlainText, GameGuideFormat.Html ->
                    useCases.saveGameGuide(guide) { GuidePageContent(html = String(bytes, Charsets.UTF_8)) }
                GameGuideFormat.Pdf, GameGuideFormat.Image ->
                    useCases.importGameGuide(guide, bytes, fileName.substringAfterLast('.', ""))
            }
            _uiState.value = libraryStateFor(useCases, reference, name)
        }
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
        downloadJob =
            viewModelScope.launch {
                // GameFaqsBrowserBridge bounds its own slow steps (each chapter-page
                // navigation, image embedding) with timeouts, but this still needs a hard
                // guarantee that the Save dialog clears even if something else in this block
                // throws - otherwise downloadProgress is stuck non-null forever with no way
                // for the UI to recover.
                val deps =
                    GuideDownloadDeps(
                        browserBridge = browserBridge,
                        saveGameGuide = useCases.saveGameGuide,
                        resolveMediaDirectory = useCases.resolveGameGuideMediaDirectory,
                        clock = clock,
                    )
                try {
                    downloadAndSaveGuide(
                        deps = deps,
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
                    // isActive is already false here when this coroutine got here via
                    // cancelDownload() rather than downloadAndSaveGuide finishing/throwing on
                    // its own - libraryStateFor is a suspend call (disk reads), which would
                    // itself immediately throw CancellationException in that case and never
                    // actually clear downloadProgress, so this branches to a plain synchronous
                    // reset back to Browsing instead (cancelDownload's own reset already did
                    // this once, for instant feedback - this is the guaranteed-correct backstop
                    // in case a stray onProgress update above raced it and won).
                    if (isActive) {
                        _uiState.value = libraryStateFor(useCases, browsing.gameReference, browsing.gameName)
                    } else {
                        (_uiState.value as? GameGuidesUiState.Browsing)?.let { current ->
                            _uiState.value = current.copy(downloadProgress = null)
                        }
                    }
                }
            }
    }

    /** Settings > Game Guides FAB > Browse GameFAQs > "Downloading guide" dialog's Cancel
     * button - stops [saveCurrentGuide]'s in-flight download and drops straight back to the
     * Browsing screen the user was already on (never the Library, since nothing was actually
     * saved to show there). Resets [GameGuidesUiState.Browsing.downloadProgress] immediately,
     * for instant feedback, rather than waiting on [downloadJob] to actually unwind - see
     * [saveCurrentGuide]'s own `finally` block for the guaranteed-correct backstop that covers
     * the gap between this call and that cancellation actually being observed. */
    fun cancelDownload() {
        downloadJob?.cancel()
        (_uiState.value as? GameGuidesUiState.Browsing)?.let { current ->
            if (current.downloadProgress != null) _uiState.value = current.copy(downloadProgress = null)
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
        pageLoadJob?.cancel()
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
            pageLoadJob?.cancel()
            val opening = openingViewingStateFor(useCases, mostRecent)
            _uiState.value = opening
            _uiState.value = loadedViewingStateFor(useCases, opening, clock)
        }
        return mostRecent != null
    }

    /**
     * Loads [pageIndex]'s content into the currently-Viewing guide - called whenever
     * [com.esde.companion.ui.gameguides.GuideViewerUiState.currentPageIndex] actually changes
     * (next/previous page-turn, a table-of-contents jump to a different page), never for the
     * page the guide was just opened/resumed on (already loaded by [loadedViewingStateFor]).
     * See [GameGuidesUiState.Viewing]'s kdoc for why pages are loaded one at a time rather
     * than all up front. A no-op if the viewer's already been closed by the time this runs.
     *
     * [pageLoadJob] cancels a still-in-flight previous call before starting this one - rapid
     * page-turns/TOC jumps used to launch overlapping, untracked disk reads with no ordering
     * guarantee, so an earlier-requested page's content could land after a later one and
     * silently overwrite it (confirmed: neither [LoadGameGuidePageUseCase] nor
     * `FileGameGuideLibraryRepository.loadPage` catch/swallow `CancellationException`, so a
     * cancelled job's coroutine never reaches the write-back below). The `guide.id` check on
     * write-back is a second, independent guard for the cross-guide case - see [openGuide]/
     * [autoOpenLastViewedGuideForCurrentGame], which also cancel [pageLoadJob] so a slow load
     * from a previously-viewed guide can never land after a different guide is opened.
     */
    fun loadPage(pageIndex: Int) {
        val viewing = _uiState.value as? GameGuidesUiState.Viewing ?: return
        _uiState.value = viewing.copy(isLoadingContent = true)
        pageLoadJob?.cancel()
        pageLoadJob =
            viewModelScope.launch {
                val content = useCases.loadGameGuidePage(viewing.guide.id, pageIndex) ?: ""
                (_uiState.value as? GameGuidesUiState.Viewing)
                    ?.takeIf { it.guide.id == viewing.guide.id }
                    ?.let { current ->
                        _uiState.value = current.copy(currentPageContent = content, isLoadingContent = false)
                    }
            }
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
 * (both fast DataStore reads), but [GameGuidesUiState.Viewing.currentPageContent] empty and
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
    val mediaDirectoryPath = useCases.resolveGameGuideMediaDirectory(guide.id)
    return GameGuidesUiState.Viewing(
        guide = guide,
        mediaDirectoryPath = mediaDirectoryPath,
        currentPageContent = "",
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
): GameGuidesUiState.Viewing =
    when (opening.guide.format) {
        GameGuideFormat.PlainText, GameGuideFormat.Html -> loadedTextViewingStateFor(useCases, opening, clock)
        GameGuideFormat.Pdf, GameGuideFormat.Image -> loadedBinaryViewingStateFor(useCases, opening, clock)
    }

private suspend fun loadedTextViewingStateFor(
    useCases: GameGuidesUseCases,
    opening: GameGuidesUiState.Viewing,
    clock: Clock,
): GameGuidesUiState.Viewing {
    val guide = opening.guide
    val progress = useCases.observeReadingProgress(guide.id).first()
    val maxPageIndex = (guide.pageCount - 1).coerceAtLeast(0)
    // Coerced in case a stale progress record (saved against a since-re-downloaded,
    // differently-paginated copy of this guide) points past the end of the pages actually on
    // disk now.
    val pageIndex = (progress?.pageIndex ?: 0).coerceIn(0, maxPageIndex)
    // Only this one resumed page is loaded here - see GameGuidesUiState.Viewing's kdoc for
    // why every other page is loaded lazily instead, via loadPage, as the user navigates.
    val content = useCases.loadGameGuidePage(guide.id, pageIndex) ?: ""
    useCases.setReadingProgress(
        GameGuideReadingProgress(guide.id, opening.initialScrollFraction, clock.millis(), pageIndex),
    )
    return opening.copy(currentPageContent = content, initialPageIndex = pageIndex, isLoadingContent = false)
}

/** The Pdf/Image counterpart to [loadedTextViewingStateFor] - resolves the on-disk binary
 * file path instead of text pages. A resumed scroll fraction doesn't apply to either format
 * (a PDF page is either shown or not; an image guide has no concept of "page" progress at
 * all), so this always records 0f rather than [opening]'s own initialScrollFraction. */
private suspend fun loadedBinaryViewingStateFor(
    useCases: GameGuidesUseCases,
    opening: GameGuidesUiState.Viewing,
    clock: Clock,
): GameGuidesUiState.Viewing {
    val guide = opening.guide
    val path = useCases.loadGameGuideBinaryPath(guide.id)
    val progress = useCases.observeReadingProgress(guide.id).first()
    val maxPageIndex = (guide.pageCount - 1).coerceAtLeast(0)
    val pageIndex = (progress?.pageIndex ?: 0).coerceIn(0, maxPageIndex)
    useCases.setReadingProgress(GameGuideReadingProgress(guide.id, 0f, clock.millis(), pageIndex))
    return opening.copy(contentFilePath = path, initialPageIndex = pageIndex, isLoadingContent = false)
}

/** The Library state for [reference]/[name], including that game's downloaded guides (which
 * may be empty - see [GameGuidesUiState.Library]'s kdoc) and its resolved manual, if any.
 * Shared by [GameGuidesViewModel.open], [GameGuidesViewModel.importGuideFor], and
 * [GameGuidesViewModel.saveCurrentGuide]'s post-save refresh. */
private suspend fun libraryStateFor(
    useCases: GameGuidesUseCases,
    reference: GameReference,
    name: String,
): GameGuidesUiState.Library {
    val guides = useCases.observeGameGuides(reference).first()
    val progressByGuideId = guides.associate { guide -> guide.id to readOverallProgressFraction(useCases, guide) }
    val manualPdfPath =
        useCases.resolveGameMedia(
            systemShortName = reference.systemShortName,
            systemPath = reference.systemPath,
            romPath = reference.romPath,
            mediaTypes = setOf(MediaType.Manuals),
        ).path(MediaType.Manuals)
    return GameGuidesUiState.Library(reference, name, guides, progressByGuideId, manualPdfPath)
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

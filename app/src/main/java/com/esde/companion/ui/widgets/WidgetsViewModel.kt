package com.esde.companion.ui.widgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esde.companion.domain.model.AchievementSummaryFetchResult
import com.esde.companion.domain.model.AchievementSummaryPeek
import com.esde.companion.domain.model.AchievementSummaryWidgetState
import com.esde.companion.domain.model.AppState
import com.esde.companion.domain.model.EsdeConnectionState
import com.esde.companion.domain.model.GameReference
import com.esde.companion.domain.model.GridDimensions
import com.esde.companion.domain.model.MediaType
import com.esde.companion.domain.model.NavigationDirection
import com.esde.companion.domain.model.PlacedWidget
import com.esde.companion.domain.model.PlaytimeStatsWidgetState
import com.esde.companion.domain.model.RetroAchievementsGameMatch
import com.esde.companion.domain.model.StateGroup
import com.esde.companion.domain.model.WidgetContent
import com.esde.companion.domain.model.WidgetContentResolver
import com.esde.companion.domain.model.WidgetType
import com.esde.companion.domain.model.currentGameReference
import com.esde.companion.domain.model.navigationDirection
import com.esde.companion.domain.model.stateGroup
import com.esde.companion.domain.usecase.GetGameAchievementSummaryUseCase
import com.esde.companion.domain.usecase.ObserveConnectionStateUseCase
import com.esde.companion.domain.usecase.ObserveRetroAchievementsCredentialsUseCase
import com.esde.companion.domain.usecase.ObserveWidgetCanvasUseCase
import com.esde.companion.domain.usecase.PeekGameAchievementSummaryUseCase
import com.esde.companion.domain.usecase.ResolveBundledSystemLogoUseCase
import com.esde.companion.domain.usecase.ResolveCustomSystemImageUseCase
import com.esde.companion.domain.usecase.ResolveCustomSystemLogoUseCase
import com.esde.companion.domain.usecase.ResolveGameDescriptionUseCase
import com.esde.companion.domain.usecase.ResolveGameMediaUseCase
import com.esde.companion.domain.usecase.ResolveGameRatingUseCase
import com.esde.companion.domain.usecase.ResolveRandomSystemMediaUseCase
import com.esde.companion.domain.usecase.ResolveRetroAchievementsGameUseCase
import com.esde.companion.ui.main.systemLogoAssetName
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Extra attempts [WidgetsViewModel.peekAndMaybeFetchAchievementSummary] makes after an
 * initial [AchievementSummaryFetchResult.NetworkError], before giving up for this gameId. */
private const val ACHIEVEMENT_FETCH_RETRY_COUNT = 2

/** Pause between retries - see [ACHIEVEMENT_FETCH_RETRY_COUNT]'s kdoc. */
private const val ACHIEVEMENT_FETCH_RETRY_DELAY_MILLIS = 1_500L

/** Pause before [WidgetsViewModel.peekAndMaybeFetchAchievementSummary]'s one playtime-stats
 * force-refresh - see its kdoc. */
private const val PLAYTIME_STATS_FORCE_REFRESH_DELAY_MILLIS = 1_500L

/**
 * Resolves the live widget canvas: which StateGroup applies to the current AppState (if
 * any - Idle has none, see [stateGroup]), what's saved for that canvas, and what each
 * placed widget should currently display. [setGridDimensions] must be called once the
 * host composable has measured real screen space - nothing is shown before that, since
 * grid-relative positions are meaningless without it.
 */
class WidgetsViewModel(
    observeConnectionState: ObserveConnectionStateUseCase,
    private val observeWidgetCanvas: ObserveWidgetCanvasUseCase,
    private val resolveGameMedia: ResolveGameMediaUseCase,
    private val resolveRandomSystemMedia: ResolveRandomSystemMediaUseCase,
    private val resolveGameDescription: ResolveGameDescriptionUseCase,
    private val resolveGameRating: ResolveGameRatingUseCase,
    private val resolveCustomSystemImage: ResolveCustomSystemImageUseCase,
    private val resolveCustomSystemLogo: ResolveCustomSystemLogoUseCase,
    private val resolveBundledSystemLogo: ResolveBundledSystemLogoUseCase,
    private val resolveRetroAchievementsGame: ResolveRetroAchievementsGameUseCase,
    private val observeRetroAchievementsCredentials: ObserveRetroAchievementsCredentialsUseCase,
    private val peekAchievementSummary: PeekGameAchievementSummaryUseCase,
    private val getAchievementSummary: GetGameAchievementSummaryUseCase,
) : ViewModel() {
    /** Caches the current system's random picks (per media type), reused as long as
     * [randomSystemMediaCacheSystem] still matches the system being resolved for - so a
     * flatMapLatest restart that doesn't correspond to an actual system change (e.g.
     * canvasState's WhileSubscribed(5_000) dropping and resubscribing the whole upstream
     * chain across a device sleep longer than that timeout) doesn't reroll
     * ResolveRandomSystemMediaUseCase's random pick for a system that's still the one
     * being displayed. The cache is cleared and rerolled the moment a *different* system
     * is resolved for - including returning to a system already visited earlier in the
     * session - so "system A -> system B -> system A" intentionally shows fresh art for
     * A the second time, while repeated resolution without ever leaving A does not. */
    private var randomSystemMediaCacheSystem: String? = null
    private val randomSystemMediaCache = mutableMapOf<MediaType, String?>()

    private suspend fun resolveRandomSystemMediaCached(
        systemShortName: String,
        mediaType: MediaType,
    ): String? {
        if (randomSystemMediaCacheSystem != systemShortName) {
            randomSystemMediaCache.clear()
            randomSystemMediaCacheSystem = systemShortName
        }
        return randomSystemMediaCache.getOrPut(mediaType) { resolveRandomSystemMedia(systemShortName, mediaType) }
    }

    // Same per-key memoization shape as randomSystemMediaCacheSystem/randomSystemMediaCache
    // above - resolveContent can re-run for reasons unrelated to the game actually changing
    // (grid resize, achievementRefreshTrigger bumps below), and ResolveRetroAchievementsGameUseCase's
    // title-matching is real CPU work that shouldn't redo for an unchanged GameReference.
    private var lastResolvedAchievementsGameRef: GameReference? = null
    private var lastResolvedAchievementsGameMatch: RetroAchievementsGameMatch? = null

    private suspend fun resolveRetroAchievementsGameCached(
        gameRef: GameReference,
        gameName: String,
    ): RetroAchievementsGameMatch {
        val cached = lastResolvedAchievementsGameMatch
        if (lastResolvedAchievementsGameRef == gameRef && cached != null) {
            return cached
        }
        val match = resolveRetroAchievementsGame(gameRef, gameName)
        lastResolvedAchievementsGameRef = gameRef
        lastResolvedAchievementsGameMatch = match
        return match
    }

    // Bumped after a background achievement-summary fetch completes (see
    // resolveAchievementSummary), re-triggering canvasState's flatMapLatest so the newly
    // (or freshly) cached value gets peeked and shown - see this ViewModel's class kdoc
    // for why a widget-specific side channel into WidgetCanvas isn't needed for this.
    private val achievementRefreshTrigger = MutableStateFlow(0)

    // One background-fetch *attempt burst* per gameId per ViewModel lifetime, not a periodic
    // poll - deliberately simple, matching this session's "no periodic refresh" limitation.
    // A NetworkError result is retried in place (see peekAndMaybeFetchAchievementSummary's
    // ACHIEVEMENT_FETCH_RETRY_COUNT) rather than left to the pre-existing "browse to a
    // different game and back" workaround for every transient blip - a real fetch normally
    // settles in ~1s, so a couple of quick retries recovers from a momentary network hiccup
    // without the widget ever needing to leave Loading.
    private var lastAchievementFetchGameId: Long? = null

    // One extra force-refresh per gameId, only ever attempted when a PlaytimeStats widget is
    // actually on the canvas - see peekAndMaybeFetchAchievementSummary's kdoc for why a
    // Success result with a null playtimeStats needs this on top of ACHIEVEMENT_FETCH_RETRY_COUNT.
    private var playtimeForceRefreshAttemptedGameId: Long? = null

    // Set (to the same gameId) once that burst finishes, successfully or not -
    // peekAchievementSummary only ever returns non-null for a cached Success (see
    // AchievementSummaryCache.peek's kdoc), so a NotFound (or an exhausted-retries
    // NetworkError) fetch result leaves peek permanently null for that gameId. Without this,
    // resolveAchievementSummary would read that as "still loading" forever instead of "tried,
    // nothing there" - see toWidgetState. A persistent NetworkError still settles on
    // Unavailable once retries are exhausted, same as a confirmed no-match - by that point
    // several real attempts (not just one) have already failed, so it's no longer a
    // premature false negative the way giving up after a single try would be.
    private var completedAchievementFetchGameId: Long? = null

    /**
     * The shared resolution both the AchievementSummary and PlaytimeStats widgets are built
     * from - both consume the exact same underlying [com.esde.companion.domain.model.GameAchievementSummary]
     * fetch (RA's `GetGameInfoAndUserProgress` response already carries achievements and
     * playtime stats together), so there's only ever one match/peek/fetch per resolve pass
     * regardless of how many of the two widget types are on the canvas. `null` means "not
     * eligible at all" (neither widget type on this canvas, no current game, or signed out).
     * [gameId] is null for a game with no RetroAchievements match at all (as opposed to a
     * match with nothing useful cached yet, which is [peeked] being null with a non-null
     * [gameId]).
     */
    private data class AchievementDataResolution(
        val gameId: Long?,
        val peeked: AchievementSummaryPeek?,
        val fetchCompleted: Boolean,
    )

    private suspend fun resolveAchievementData(
        widgets: List<PlacedWidget>,
        identity: ContentIdentity,
    ): AchievementDataResolution? {
        val match = resolveEligibleAchievementsMatch(widgets, identity) ?: return null
        val gameId = (match as? RetroAchievementsGameMatch.Found)?.gameId
        val needsPlaytimeStats = widgets.any { it.widgetType is WidgetType.PlaytimeStats }
        val peeked = gameId?.let { peekAndMaybeFetchAchievementSummary(it, needsPlaytimeStats) }
        val fetchCompleted = gameId != null && completedAchievementFetchGameId == gameId
        return AchievementDataResolution(gameId, peeked, fetchCompleted)
    }

    /** The `null`-returning eligibility gates for [resolveAchievementData], split out so
     * neither function trips detekt's ReturnCount limit. */
    private suspend fun resolveEligibleAchievementsMatch(
        widgets: List<PlacedWidget>,
        identity: ContentIdentity,
    ): RetroAchievementsGameMatch? {
        val gameRef = identity.gameRef
        val gameName = identity.gameName
        val eligible =
            widgets.any {
                it.widgetType is WidgetType.AchievementSummary || it.widgetType is WidgetType.PlaytimeStats
            } &&
                gameRef != null &&
                gameName != null &&
                observeRetroAchievementsCredentials().first() != null
        if (!eligible || gameRef == null || gameName == null) return null

        return resolveRetroAchievementsGameCached(gameRef, gameName)
    }

    /**
     * Cache-only peek, kicking off a background fetch (never blocking this function) when
     * nothing useful is cached yet - see [resolveAchievementData]'s kdoc. A
     * [AchievementSummaryFetchResult.NetworkError] is retried in place, up to
     * [ACHIEVEMENT_FETCH_RETRY_COUNT] additional times with a short
     * [ACHIEVEMENT_FETCH_RETRY_DELAY_MILLIS] pause between attempts, before giving up - a real
     * fetch normally settles in about a second, so a couple of quick retries is enough to ride
     * out a momentary connectivity blip.
     *
     * A [AchievementSummaryFetchResult.Success] whose `playtimeStats` is null is a narrower,
     * separate case worth its own one-shot recovery when [needsPlaytimeStats] (a PlaytimeStats
     * widget is actually on the canvas): confirmed on-device as RA's own `GetGameProgression`
     * sub-call (see `RetroClientRetroAchievementsApi.getGameInfoAndUserProgress`) occasionally
     * degrading to nothing even after its own retry, while the main achievement data in the
     * same response loads fine - a plain NetworkError retry doesn't catch this, since the
     * overall result genuinely is a Success, just missing this one piece. A manual "Refresh"
     * in the achievement screen reliably recovers it (a fresh, unburdened request), so one
     * automatic force-refresh attempt here does the same thing without the user having to
     * leave the game and do it by hand.
     */
    private suspend fun peekAndMaybeFetchAchievementSummary(
        gameId: Long,
        needsPlaytimeStats: Boolean,
    ): AchievementSummaryPeek? {
        val peeked = peekAchievementSummary(gameId)
        if ((peeked == null || peeked.isStale) && lastAchievementFetchGameId != gameId) {
            lastAchievementFetchGameId = gameId
            viewModelScope.launch {
                var result = getAchievementSummary(gameId)
                var attempt = 0
                while (result is AchievementSummaryFetchResult.NetworkError &&
                    attempt < ACHIEVEMENT_FETCH_RETRY_COUNT
                ) {
                    delay(ACHIEVEMENT_FETCH_RETRY_DELAY_MILLIS)
                    result = getAchievementSummary(gameId)
                    attempt++
                }
                val successResult = result as? AchievementSummaryFetchResult.Success
                val missingPlaytimeStats =
                    needsPlaytimeStats && successResult != null && successResult.summary.playtimeStats == null
                if (missingPlaytimeStats && playtimeForceRefreshAttemptedGameId != gameId) {
                    playtimeForceRefreshAttemptedGameId = gameId
                    delay(PLAYTIME_STATS_FORCE_REFRESH_DELAY_MILLIS)
                    getAchievementSummary(gameId, forceRefresh = true)
                }
                completedAchievementFetchGameId = gameId
                achievementRefreshTrigger.update { it + 1 }
            }
        }
        return peeked
    }

    private fun AchievementDataResolution.toAchievementSummaryWidgetState(): AchievementSummaryWidgetState =
        when {
            gameId == null -> AchievementSummaryWidgetState.Unavailable
            peeked == null ->
                if (fetchCompleted) AchievementSummaryWidgetState.Unavailable else AchievementSummaryWidgetState.Loading
            peeked.summary.achievements.isEmpty() -> AchievementSummaryWidgetState.Unavailable
            else ->
                AchievementSummaryWidgetState.Loaded(
                    unlockedCount = peeked.summary.achievements.count { it.unlocked },
                    totalCount = peeked.summary.achievements.size,
                    earnedPoints = peeked.summary.earnedPoints,
                    totalPoints = peeked.summary.totalPoints,
                    completionPercent = peeked.summary.completionPercent,
                    isRefreshing = peeked.isStale,
                )
        }

    private fun AchievementDataResolution.toPlaytimeStatsWidgetState(): PlaytimeStatsWidgetState =
        when {
            gameId == null -> PlaytimeStatsWidgetState.Unavailable
            peeked == null ->
                if (fetchCompleted) PlaytimeStatsWidgetState.Unavailable else PlaytimeStatsWidgetState.Loading
            peeked.summary.playtimeStats == null -> PlaytimeStatsWidgetState.Unavailable
            else ->
                PlaytimeStatsWidgetState.Loaded(stats = peeked.summary.playtimeStats, isRefreshing = peeked.isStale)
        }

    private val gridDimensions = MutableStateFlow<GridDimensions?>(null)

    fun setGridDimensions(grid: GridDimensions) {
        gridDimensions.value = grid
    }

    // Distilled from raw AppState down to just the identity that actually matters for
    // widget content - same reasoning as MainViewModel's ImageSource. Without
    // distinctUntilChanged() here, any AppState field change irrelevant to widget content
    // (or the documented spurious game-select re-fire after game-start) still retriggers
    // flatMapLatest below, cancelling an in-flight resolution/decode and restarting the
    // whole chain - under a burst of same-target events that never converges quickly.
    private val contentIdentity: Flow<ContentIdentity?> =
        observeConnectionState()
            .map { connection ->
                val appState = (connection as? EsdeConnectionState.Connected)?.appState ?: return@map null
                val group = appState.stateGroup() ?: return@map null
                ContentIdentity(
                    stateGroup = group,
                    gameRef = appState.currentGameReference(),
                    isBrowsingGame = appState is AppState.BrowsingGame,
                    // System Logo/System Image widgets are offered on both canvases (see
                    // EditWidgetsOverlay's widgetCatalogFor), so this must resolve the
                    // system currently in play/browse-game context too, not just
                    // BrowsingSystem - otherwise those widgets would silently never
                    // render on the Playing canvas.
                    systemShortName =
                        when (appState) {
                            is AppState.BrowsingSystem -> appState.systemShortName
                            is AppState.BrowsingGame -> appState.systemShortName
                            is AppState.PlayingGame -> appState.systemShortName
                            is AppState.Screensaver -> appState.currentGame?.systemShortName
                            is AppState.Idle -> null
                        },
                    systemFullName =
                        when (appState) {
                            is AppState.BrowsingSystem -> appState.systemFullName
                            is AppState.BrowsingGame -> appState.systemFullName
                            is AppState.PlayingGame -> appState.systemFullName
                            is AppState.Screensaver -> appState.currentGame?.systemFullName
                            is AppState.Idle -> null
                        },
                    gameName =
                        when (appState) {
                            is AppState.BrowsingGame -> appState.gameName
                            is AppState.PlayingGame -> appState.gameName
                            is AppState.Screensaver -> appState.currentGame?.gameName
                            is AppState.Idle, is AppState.BrowsingSystem -> null
                        },
                    navigationDirection = appState.navigationDirection(),
                )
            }
            .distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    val canvasState: StateFlow<WidgetCanvasState> =
        combine(contentIdentity, gridDimensions.filterNotNull(), achievementRefreshTrigger) { identity, grid, _ ->
            identity to grid
        }
            .flatMapLatest { (identity, grid) ->
                if (identity == null) {
                    flowOf(WidgetCanvasState.Disconnected)
                } else {
                    observeWidgetCanvas(identity.stateGroup, grid).map { widgets ->
                        WidgetCanvasState.Showing(
                            widgets,
                            resolveContent(widgets, identity),
                            identity.navigationDirection,
                        )
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = WidgetCanvasState.Unmeasured,
            )

    /**
     * Pre-resolves every lookup a widget on this canvas could need, once, before building
     * the per-widget content map - keeps WidgetContentResolver's lookup lambdas cheap and
     * synchronous (see its kdoc), rather than each widget independently triggering a
     * suspend media lookup.
     */
    private suspend fun resolveContent(
        widgets: List<PlacedWidget>,
        identity: ContentIdentity,
    ): Map<String, WidgetContent> {
        // Includes each widget's own fallbackMediaType, not just its primary mediaType -
        // otherwise a lone FanArt widget's Screenshots fallback would only ever resolve
        // by coincidence, when some other widget on the same canvas also happens to need
        // Screenshots. See WidgetContentResolver/resolveMediaWidgetContent's kdoc.
        val hasVideoWidget = widgets.any { it.widgetType is WidgetType.Video }
        val neededGameMediaTypes =
            widgets.mapNotNull { it.widgetType as? WidgetType.GameMedia }
                .flatMap { listOfNotNull(it.mediaType, it.fallbackMediaType) }
                .toSet() + if (hasVideoWidget) setOf(MediaType.Videos) else emptySet()
        val gameMedia =
            identity.gameRef?.let {
                resolveGameMedia(
                    systemShortName = it.systemShortName,
                    systemPath = it.systemPath,
                    romPath = it.romPath,
                    mediaTypes = neededGameMediaTypes,
                )
            }
        val gameDescription = identity.gameRef?.let { resolveGameDescription(it.systemShortName, it.romPath) }
        val gameRating = identity.gameRef?.let { resolveGameRating(it.systemShortName, it.romPath) }
        val systemShortName = identity.systemShortName

        val hasSystemImageWidget = widgets.any { it.widgetType is WidgetType.SystemImage }
        val neededSystemMediaTypes =
            (
                widgets.mapNotNull { it.widgetType as? WidgetType.SystemMedia }
                    .flatMap { listOfNotNull(it.mediaType, it.fallbackMediaType) } +
                    if (hasSystemImageWidget) listOf(MediaType.FanArt, MediaType.Screenshots) else emptyList()
            ).distinct()
        val systemMediaByType: Map<MediaType, String?> =
            systemShortName?.let { shortName ->
                neededSystemMediaTypes.associateWith { mediaType ->
                    resolveRandomSystemMediaCached(shortName, mediaType)
                }
            } ?: emptyMap()

        val systemLogoAssetPath = systemShortName?.let { resolveBundledSystemLogo(systemLogoAssetName(it)) }

        val needsCustomLogo = widgets.any { it.widgetType is WidgetType.SystemLogo }
        val needsCustomImage = widgets.any { it.widgetType is WidgetType.SystemImage }
        val customSystemLogoPath =
            if (needsCustomLogo) {
                systemShortName?.let {
                    resolveCustomSystemLogo(
                        systemLogoAssetName(it),
                    )
                }
            } else {
                null
            }
        val customSystemImagePath =
            if (needsCustomImage) {
                systemShortName?.let {
                    resolveCustomSystemImage(
                        systemLogoAssetName(it),
                    )
                }
            } else {
                null
            }

        val achievementData = resolveAchievementData(widgets, identity)
        val achievementSummaryState = achievementData?.toAchievementSummaryWidgetState()
        val playtimeStatsState = achievementData?.toPlaytimeStatsWidgetState()

        return widgets.associate { widget ->
            widget.id to
                WidgetContentResolver.resolve(
                    widgetType = widget.widgetType,
                    systemLogoAssetPath = { systemLogoAssetPath },
                    customSystemLogoLookup = { customSystemLogoPath },
                    customSystemImageLookup = { customSystemImagePath },
                    systemMediaLookup = { mediaType -> systemMediaByType[mediaType] },
                    gameMediaLookup = { mediaType -> gameMedia?.path(mediaType) },
                    gameDescriptionLookup = { gameDescription?.text },
                    gameRatingLookup = { gameRating?.value },
                    // null in EditWidgetsViewModel, as today
                    fallbackBackgroundAssetPath = FALLBACK_BACKGROUND_ASSET,
                    systemNameLookup = { identity.systemFullName },
                    gameNameLookup = { identity.gameName },
                    videoLookup = { gameMedia?.path(MediaType.Videos).takeIf { identity.isBrowsingGame } },
                    achievementSummaryLookup = { achievementSummaryState },
                    playtimeStatsLookup = { playtimeStatsState },
                )
        }
    }

    private data class ContentIdentity(
        val stateGroup: StateGroup,
        val gameRef: GameReference?,
        val isBrowsingGame: Boolean,
        val systemShortName: String?,
        val systemFullName: String?,
        val gameName: String?,
        val navigationDirection: NavigationDirection?,
    )
}

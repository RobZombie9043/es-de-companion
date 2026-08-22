package com.esde.companion.ui.retroachievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esde.companion.domain.model.AchievementDisplayField
import com.esde.companion.domain.model.AchievementFilterOption
import com.esde.companion.domain.model.AchievementSortOrder
import com.esde.companion.domain.model.GameMatchOverride
import com.esde.companion.domain.model.GameReference
import com.esde.companion.domain.model.LeaderboardSortOrder
import com.esde.companion.domain.model.RetroAchievementsCandidateGame
import com.esde.companion.domain.model.RetroAchievementsGameMatch
import com.esde.companion.domain.model.resolveAchievementsGame
import com.esde.companion.domain.usecase.GetAchievementCommentsUseCase
import com.esde.companion.domain.usecase.GetLeaderboardEntriesUseCase
import com.esde.companion.domain.usecase.ObserveConnectionStateUseCase
import com.esde.companion.domain.usecase.ObserveRetroAchievementsCredentialsUseCase
import com.esde.companion.domain.usecase.ObserveScreensaverAwareContextUseCase
import com.esde.companion.domain.usecase.ObserveUpdateAchievementsOnScreensaverEnabledUseCase
import com.esde.companion.domain.usecase.ResolveRetroAchievementsGameUseCase
import com.esde.companion.domain.usecase.SearchRetroAchievementsGamesUseCase
import com.esde.companion.domain.usecase.SetGameMatchOverrideUseCase
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private data class CurrentGame(val reference: GameReference, val name: String)

// Fast-scrolling ES-DE's game list drives a distinct CurrentGame through this ViewModel for every
// momentarily-highlighted game, each of which would otherwise kick off its own resolve + two
// concurrent RetroAchievements fetches (achievement summary, leaderboards) - almost all of them
// abandoned within milliseconds as scrolling continues. Debouncing before resolveAndFetch ever
// runs means only the game the user actually settles on triggers a fetch, rather than a burst of
// concurrent in-flight requests per scrolled-past game - confirmed on-device as the trigger for a
// transient "Couldn't load achievements: ...malformed JSON..." error that a manual Refresh always
// recovered from (consistent with an abandoned-but-still-executing request racing a live one).
private const val RESOLVE_DEBOUNCE_MILLIS = 400L

/**
 * Drives the RetroAchievements FAB/summary view. Re-resolves whenever the signed-in state
 * or the current game changes (via [ObserveConnectionStateUseCase], the same source
 * [com.esde.companion.ui.manual.GameManualViewModel] follows), then - only once resolution
 * succeeds - fetches that game's achievement summary. Exposes independent state for each
 * stage (see [RetroAchievementsUiState.kt]) so a fetch failure is never confused with a
 * resolution problem.
 *
 * Also owns the low-key manual-correction picker's state ([searchQuery]/[searchResults])
 * and [onGameCorrected] - kept on this same ViewModel rather than a separate one, since
 * both need "which game/system is currently showing", which only this ViewModel already
 * tracks (see [lastKnownGame]).
 *
 * [currentGame] is folded through [com.esde.companion.domain.model.resolveAchievementsGame]
 * rather than a plain `map`, so that when Settings > RetroAchievements > "Update on
 * Screensaver" is off, a screensaver starting on a different game holds the previously
 * displayed game instead of redirecting to it - see that function's kdoc.
 *
 * [collectLatest] cancels an in-flight resolve/fetch as soon as the game or sign-in state
 * moves on, rather than letting a stale network call finish and overwrite newer state.
 * [currentGame] is additionally debounced ([RESOLVE_DEBOUNCE_MILLIS]) before reaching that
 * pipeline - see the constant's own kdoc for why fast list-scrolling needs this.
 */
@OptIn(FlowPreview::class)
@Suppress("LongParameterList", "TooManyFunctions")
class RetroAchievementsViewModel(
    observeConnectionState: ObserveConnectionStateUseCase,
    observeCredentials: ObserveRetroAchievementsCredentialsUseCase,
    observeUpdateAchievementsOnScreensaverEnabled: ObserveUpdateAchievementsOnScreensaverEnabledUseCase,
    private val resolveGame: ResolveRetroAchievementsGameUseCase,
    private val detailUseCases: RetroAchievementsDetailUseCases,
    private val searchGames: SearchRetroAchievementsGamesUseCase,
    private val setGameMatchOverride: SetGameMatchOverrideUseCase,
    private val getAchievementComments: GetAchievementCommentsUseCase,
    private val getLeaderboardEntries: GetLeaderboardEntriesUseCase,
) : ViewModel() {
    // Set by MainActivity via onOverlayVisibilityChanged, reflecting whether this screen is
    // actually on screen right now (its AnimatedVisibility condition) - see
    // resolveAchievementsGame's kdoc for why the screensaver-hold logic needs this rather than
    // just tracking [currentGame]'s previous value.
    private val overlayVisible = MutableStateFlow(false)

    private val currentGameContext =
        ObserveScreensaverAwareContextUseCase(
            observeConnectionState,
            observeUpdateAchievementsOnScreensaverEnabled,
            { overlayVisible },
            ::resolveAchievementsGame,
        )

    private val currentGame =
        currentGameContext()
            .map { resolved -> resolved?.let { (reference, name) -> CurrentGame(reference, name) } }
            .distinctUntilChanged()
            .debounce(RESOLVE_DEBOUNCE_MILLIS)

    private val _resolution =
        MutableStateFlow<RetroAchievementsResolutionState>(RetroAchievementsResolutionState.NoGame)
    val resolution: StateFlow<RetroAchievementsResolutionState> = _resolution

    private val _fetch = MutableStateFlow<RetroAchievementsFetchState>(RetroAchievementsFetchState.Idle)
    val fetch: StateFlow<RetroAchievementsFetchState> = _fetch

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchResults = MutableStateFlow<List<RetroAchievementsCandidateGame>>(emptyList())
    val searchResults: StateFlow<List<RetroAchievementsCandidateGame>> = _searchResults

    // Sort/filter/display-field controls and the tap-to-expand comments accordion - see
    // AchievementDisplayController's kdoc. Shared with RetroAchievementsSystemGamesViewModel,
    // which owns its own instance for its own achievement list.
    private val achievementDisplay = AchievementDisplayController(getAchievementComments, viewModelScope)
    val sortOrder: StateFlow<AchievementSortOrder> = achievementDisplay.sortOrder
    val filter: StateFlow<Set<AchievementFilterOption>> = achievementDisplay.filter
    val displayField: StateFlow<AchievementDisplayField> = achievementDisplay.displayField
    val expanded: StateFlow<ExpandedAchievementComments?> = achievementDisplay.expanded

    // The Achievements/Leaderboards chip toggle and its own sort/tap-to-expand controls - see
    // LeaderboardDisplayController's kdoc. Shared with RetroAchievementsSystemGamesViewModel,
    // which owns its own instance for its own leaderboard list.
    private val _mode = MutableStateFlow(RetroAchievementsMode.Achievements)
    val mode: StateFlow<RetroAchievementsMode> = _mode

    private val _leaderboardsFetch = MutableStateFlow<LeaderboardsFetchState>(LeaderboardsFetchState.Idle)
    val leaderboardsFetch: StateFlow<LeaderboardsFetchState> = _leaderboardsFetch

    private val leaderboardDisplay = LeaderboardDisplayController(getLeaderboardEntries, viewModelScope)
    val leaderboardSortOrder: StateFlow<LeaderboardSortOrder> = leaderboardDisplay.sortOrder
    val leaderboardExpanded: StateFlow<ExpandedLeaderboardEntries?> = leaderboardDisplay.expanded

    private val _hashSupport = MutableStateFlow<HashSupportState>(HashSupportState.Hidden)
    val hashSupport: StateFlow<HashSupportState> = _hashSupport

    // Set at the start of every resolveAndFetch call (a single collectLatest coroutine, so
    // no concurrent-write race) - the correction picker's search scope and onGameCorrected's
    // target both need "which game is this screen currently about", which resolution/fetch
    // alone don't expose to the UI layer.
    private var lastKnownGame: CurrentGame? = null

    // Set alongside resolution whenever it lands on Found - the "Supported Hashes" dialog
    // needs the resolved RA gameId, which RetroAchievementsResolutionState.Found itself
    // doesn't carry (it only exposes the match method).
    private var lastGameId: Long? = null

    // Bumped after onGameCorrected persists an override, or after onRefreshRequested, to force
    // a re-resolve even though neither currentGame nor the credentials flow actually changed.
    private val forceRefresh = ForceRefreshCoordinator()

    init {
        viewModelScope.launch {
            combine(observeCredentials(), currentGame, forceRefresh.trigger) { credentials, game, _ ->
                (credentials != null) to game
            }.collectLatest { (signedIn, game) -> resolveAndFetch(signedIn, game) }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        val game = lastKnownGame ?: return
        viewModelScope.launch {
            _searchResults.value = searchGames(game.reference.systemShortName, query, game.name)
        }
    }

    fun onSortOrderChanged(order: AchievementSortOrder) = achievementDisplay.onSortOrderChanged(order)

    fun onFilterChanged(filter: Set<AchievementFilterOption>) = achievementDisplay.onFilterChanged(filter)

    fun onDisplayFieldChanged(displayField: AchievementDisplayField) {
        achievementDisplay.onDisplayFieldChanged(displayField)
    }

    fun onGameCorrected(candidate: RetroAchievementsCandidateGame) {
        val reference = lastKnownGame?.reference ?: return
        viewModelScope.launch {
            setGameMatchOverride(GameMatchOverride(reference.systemShortName, reference.romPath, candidate.gameId))
            forceRefresh.retrigger()
        }
    }

    fun onRequestHashSupport() {
        val reference = lastKnownGame?.reference ?: return
        val gameId = lastGameId ?: return
        _hashSupport.value = HashSupportState.Loading
        viewModelScope.launch {
            _hashSupport.value = HashSupportState.Loaded(detailUseCases.getHashSupport(reference, gameId))
        }
    }

    fun onHashSupportDismissed() {
        _hashSupport.value = HashSupportState.Hidden
    }

    fun onAchievementTapped(achievementId: Long) = achievementDisplay.onAchievementTapped(achievementId)

    fun onModeChanged(mode: RetroAchievementsMode) {
        _mode.value = mode
    }

    fun onLeaderboardSortOrderChanged(order: LeaderboardSortOrder) = leaderboardDisplay.onSortOrderChanged(order)

    fun onLeaderboardTapped(leaderboardId: Long) = leaderboardDisplay.onLeaderboardTapped(leaderboardId)

    /** Forces the next fetch to bypass [RetroAchievementsDetailUseCases.getAchievementSummary]'s cache. */
    fun onRefreshRequested() = forceRefresh.request()

    /** Called by MainActivity whenever this screen's own on-screen visibility changes. */
    fun onOverlayVisibilityChanged(visible: Boolean) {
        overlayVisible.value = visible
    }

    private suspend fun resolveAndFetch(
        signedIn: Boolean,
        game: CurrentGame?,
    ) {
        lastKnownGame = game
        lastGameId = null
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _hashSupport.value = HashSupportState.Hidden
        achievementDisplay.onTargetChanged()
        leaderboardDisplay.onTargetChanged()
        _leaderboardsFetch.value = LeaderboardsFetchState.Idle

        if (!signedIn) {
            _resolution.value = RetroAchievementsResolutionState.NotSignedIn
            _fetch.value = RetroAchievementsFetchState.Idle
            return
        }
        if (game == null) {
            _resolution.value = RetroAchievementsResolutionState.NoGame
            _fetch.value = RetroAchievementsFetchState.Idle
            return
        }
        when (val match = resolveGame(game.reference, game.name)) {
            RetroAchievementsGameMatch.UnsupportedSystem -> {
                _resolution.value = RetroAchievementsResolutionState.UnsupportedSystem
                _fetch.value = RetroAchievementsFetchState.Idle
            }
            RetroAchievementsGameMatch.NoMatch -> {
                _resolution.value = RetroAchievementsResolutionState.NoMatch
                _fetch.value = RetroAchievementsFetchState.Idle
            }
            is RetroAchievementsGameMatch.Found -> {
                lastGameId = match.gameId
                _resolution.value = RetroAchievementsResolutionState.Found(match.method)
                _fetch.value = RetroAchievementsFetchState.Loading
                _leaderboardsFetch.value = LeaderboardsFetchState.Loading
                // Both fetched concurrently, and both consume the same forceRefresh flag - see
                // this ViewModel's kdoc/CLAUDE.md for why leaderboards are fetched eagerly
                // (the chip toggle needs a real leaderboard count immediately) and why one
                // consume() call feeds both (so the kebab's "Refresh" bypasses both caches).
                val refresh = forceRefresh.consume()
                coroutineScope {
                    val achievementsDeferred = async { detailUseCases.getAchievementSummary(match.gameId, refresh) }
                    val leaderboardsDeferred = async { detailUseCases.getGameLeaderboards(match.gameId, refresh) }
                    _fetch.value = achievementsDeferred.await().toFetchState()
                    _leaderboardsFetch.value = leaderboardsDeferred.await().toFetchState()
                }
            }
        }
    }
}

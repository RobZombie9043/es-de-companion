package com.esde.companion.ui.retroachievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esde.companion.domain.model.AchievementDisplayField
import com.esde.companion.domain.model.AchievementFilterOption
import com.esde.companion.domain.model.AchievementSortOrder
import com.esde.companion.domain.model.EsdeConnectionState
import com.esde.companion.domain.model.GameMatchOverride
import com.esde.companion.domain.model.GameReference
import com.esde.companion.domain.model.RetroAchievementsCandidateGame
import com.esde.companion.domain.model.RetroAchievementsGameMatch
import com.esde.companion.domain.model.currentGameName
import com.esde.companion.domain.model.currentGameReference
import com.esde.companion.domain.usecase.GetGameAchievementSummaryUseCase
import com.esde.companion.domain.usecase.ObserveConnectionStateUseCase
import com.esde.companion.domain.usecase.ObserveRetroAchievementsCredentialsUseCase
import com.esde.companion.domain.usecase.ResolveRetroAchievementsGameUseCase
import com.esde.companion.domain.usecase.SearchRetroAchievementsGamesUseCase
import com.esde.companion.domain.usecase.SetGameMatchOverrideUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private data class CurrentGame(val reference: GameReference, val name: String)

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
 * [collectLatest] cancels an in-flight resolve/fetch as soon as the game or sign-in state
 * moves on, rather than letting a stale network call finish and overwrite newer state.
 */
class RetroAchievementsViewModel(
    observeConnectionState: ObserveConnectionStateUseCase,
    observeCredentials: ObserveRetroAchievementsCredentialsUseCase,
    private val resolveGame: ResolveRetroAchievementsGameUseCase,
    private val getAchievementSummary: GetGameAchievementSummaryUseCase,
    private val searchGames: SearchRetroAchievementsGamesUseCase,
    private val setGameMatchOverride: SetGameMatchOverrideUseCase,
) : ViewModel() {
    private val currentGame =
        observeConnectionState()
            .map { connection -> (connection as? EsdeConnectionState.Connected)?.appState }
            .map { appState ->
                val reference = appState?.currentGameReference() ?: return@map null
                val name = appState.currentGameName() ?: return@map null
                CurrentGame(reference, name)
            }
            .distinctUntilChanged()

    private val _resolution =
        MutableStateFlow<RetroAchievementsResolutionState>(RetroAchievementsResolutionState.NoGame)
    val resolution: StateFlow<RetroAchievementsResolutionState> = _resolution

    private val _fetch = MutableStateFlow<RetroAchievementsFetchState>(RetroAchievementsFetchState.Idle)
    val fetch: StateFlow<RetroAchievementsFetchState> = _fetch

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchResults = MutableStateFlow<List<RetroAchievementsCandidateGame>>(emptyList())
    val searchResults: StateFlow<List<RetroAchievementsCandidateGame>> = _searchResults

    private val _sortOrder = MutableStateFlow(AchievementSortOrder.DisplayOrderFirst)
    val sortOrder: StateFlow<AchievementSortOrder> = _sortOrder

    private val _filter = MutableStateFlow<Set<AchievementFilterOption>>(emptySet())
    val filter: StateFlow<Set<AchievementFilterOption>> = _filter

    private val _displayField = MutableStateFlow(AchievementDisplayField.UnlockRate)
    val displayField: StateFlow<AchievementDisplayField> = _displayField

    // Set at the start of every resolveAndFetch call (a single collectLatest coroutine, so
    // no concurrent-write race) - the correction picker's search scope and onGameCorrected's
    // target both need "which game is this screen currently about", which resolution/fetch
    // alone don't expose to the UI layer.
    private var lastKnownGame: CurrentGame? = null

    // Bumped after onGameCorrected persists an override, to force a re-resolve even though
    // neither currentGame nor the credentials flow actually changed.
    private val refreshTrigger = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            combine(observeCredentials(), currentGame, refreshTrigger) { credentials, game, _ ->
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

    fun onSortOrderChanged(order: AchievementSortOrder) {
        _sortOrder.value = order
    }

    fun onFilterChanged(filter: Set<AchievementFilterOption>) {
        _filter.value = filter
    }

    fun onDisplayFieldChanged(displayField: AchievementDisplayField) {
        _displayField.value = displayField
    }

    fun onGameCorrected(candidate: RetroAchievementsCandidateGame) {
        val reference = lastKnownGame?.reference ?: return
        viewModelScope.launch {
            setGameMatchOverride(GameMatchOverride(reference.systemShortName, reference.romPath, candidate.gameId))
            refreshTrigger.value += 1
        }
    }

    private suspend fun resolveAndFetch(
        signedIn: Boolean,
        game: CurrentGame?,
    ) {
        lastKnownGame = game
        _searchQuery.value = ""
        _searchResults.value = emptyList()

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
                _resolution.value = RetroAchievementsResolutionState.Found(match.method)
                _fetch.value = RetroAchievementsFetchState.Loading
                _fetch.value = getAchievementSummary(match.gameId).toFetchState()
            }
        }
    }
}

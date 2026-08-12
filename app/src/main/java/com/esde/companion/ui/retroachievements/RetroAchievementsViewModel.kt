package com.esde.companion.ui.retroachievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esde.companion.domain.model.AchievementSummaryFetchResult
import com.esde.companion.domain.model.EsdeConnectionState
import com.esde.companion.domain.model.GameReference
import com.esde.companion.domain.model.RetroAchievementsGameMatch
import com.esde.companion.domain.model.currentGameName
import com.esde.companion.domain.model.currentGameReference
import com.esde.companion.domain.usecase.GetGameAchievementSummaryUseCase
import com.esde.companion.domain.usecase.ObserveConnectionStateUseCase
import com.esde.companion.domain.usecase.ObserveRetroAchievementsCredentialsUseCase
import com.esde.companion.domain.usecase.ResolveRetroAchievementsGameUseCase
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
 * [collectLatest] cancels an in-flight resolve/fetch as soon as the game or sign-in state
 * moves on, rather than letting a stale network call finish and overwrite newer state.
 */
class RetroAchievementsViewModel(
    observeConnectionState: ObserveConnectionStateUseCase,
    observeCredentials: ObserveRetroAchievementsCredentialsUseCase,
    private val resolveGame: ResolveRetroAchievementsGameUseCase,
    private val getAchievementSummary: GetGameAchievementSummaryUseCase,
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

    init {
        viewModelScope.launch {
            combine(observeCredentials(), currentGame) { credentials, game -> (credentials != null) to game }
                .distinctUntilChanged()
                .collectLatest { (signedIn, game) -> resolveAndFetch(signedIn, game) }
        }
    }

    private suspend fun resolveAndFetch(
        signedIn: Boolean,
        game: CurrentGame?,
    ) {
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
                _fetch.value = fetchStateFor(getAchievementSummary(match.gameId))
            }
        }
    }

    private fun fetchStateFor(result: AchievementSummaryFetchResult): RetroAchievementsFetchState =
        when (result) {
            is AchievementSummaryFetchResult.Success -> RetroAchievementsFetchState.Loaded(result.summary)
            AchievementSummaryFetchResult.NotFound -> RetroAchievementsFetchState.NotFound
            is AchievementSummaryFetchResult.NetworkError -> RetroAchievementsFetchState.NetworkError(result.message)
        }
}

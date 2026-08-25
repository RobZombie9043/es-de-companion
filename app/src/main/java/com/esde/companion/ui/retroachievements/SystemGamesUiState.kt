package com.esde.companion.ui.retroachievements

/**
 * State for the system-wide games browser - shown while ES-DE is browsing a system,
 * independent of any single game, via the shared contextually-aware
 * [com.esde.companion.domain.model.FabType.RetroAchievements] FAB (see MainActivity's
 * `RetroAchievementsFabMode`). [Loaded] carries the full unfiltered list; search filtering
 * is applied separately in the ViewModel (see
 * [RetroAchievementsSystemGamesViewModel.filteredGames]) so switching the query never needs
 * to re-fetch.
 */
sealed class SystemGamesUiState {
    data object NotSignedIn : SystemGamesUiState()

    data object NoSystem : SystemGamesUiState()

    data object UnsupportedSystem : SystemGamesUiState()

    data object Loading : SystemGamesUiState()

    data class Loaded(val systemFullName: String) : SystemGamesUiState()
}

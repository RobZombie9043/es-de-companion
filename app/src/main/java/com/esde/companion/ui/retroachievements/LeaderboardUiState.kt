package com.esde.companion.ui.retroachievements

import com.esde.companion.domain.model.GameLeaderboardsSummary
import com.esde.companion.domain.model.LeaderboardEntriesFetchResult
import com.esde.companion.domain.model.LeaderboardEntry
import com.esde.companion.domain.model.LeaderboardsFetchResult

/**
 * Which of a game's two RetroAchievements facets is currently showing - the chip toggle's value,
 * shared by both [RetroAchievementsScreen] and [RetroAchievementsSystemGamesScreen]'s `GameDetailPage`.
 */
enum class RetroAchievementsMode {
    Achievements,
    Leaderboards,
}

/** Outcome of fetching a resolved game's leaderboard list - mirrors [RetroAchievementsFetchState]'s shape. */
sealed class LeaderboardsFetchState {
    data object Idle : LeaderboardsFetchState()

    data object Loading : LeaderboardsFetchState()

    /** [isRefreshing] mirrors [RetroAchievementsFetchState.Loaded]'s field - see its kdoc for why
     * this is a flag on [Loaded] rather than a separate sealed state. */
    data class Loaded(val summary: GameLeaderboardsSummary, val isRefreshing: Boolean = false) :
        LeaderboardsFetchState()

    data object NotFound : LeaderboardsFetchState()

    data class NetworkError(val message: String) : LeaderboardsFetchState()
}

/** Shared by both ViewModels' leaderboard-fetch logic - mirrors [AchievementSummaryFetchResult.toFetchState]. */
internal fun LeaderboardsFetchResult.toFetchState(): LeaderboardsFetchState =
    when (this) {
        is LeaderboardsFetchResult.Success -> LeaderboardsFetchState.Loaded(summary)
        LeaderboardsFetchResult.NotFound -> LeaderboardsFetchState.NotFound
        is LeaderboardsFetchResult.NetworkError -> LeaderboardsFetchState.NetworkError(message)
    }

/**
 * Backs the tap-to-expand entries section under an expanded [com.esde.companion.domain.model.LeaderboardSummary]
 * row - only meaningful while paired with a leaderboardId inside [ExpandedLeaderboardEntries].
 */
sealed class LeaderboardEntriesFetchState {
    data object Loading : LeaderboardEntriesFetchState()

    data class Loaded(val entries: List<LeaderboardEntry>) : LeaderboardEntriesFetchState()

    data class NetworkError(val message: String) : LeaderboardEntriesFetchState()
}

/**
 * Atomically pairs which leaderboard's entries section is expanded with that leaderboard's fetch
 * state, in a single value - same torn-read rationale as [ExpandedAchievementComments]'s kdoc.
 */
data class ExpandedLeaderboardEntries(
    val leaderboardId: Long,
    val entries: LeaderboardEntriesFetchState,
)

/** Shared by both ViewModels' entries-fetch logic - see [ExpandedLeaderboardEntries]'s kdoc. */
internal fun LeaderboardEntriesFetchResult.toEntriesFetchState(): LeaderboardEntriesFetchState =
    when (this) {
        is LeaderboardEntriesFetchResult.Success -> LeaderboardEntriesFetchState.Loaded(entries)
        is LeaderboardEntriesFetchResult.NetworkError -> LeaderboardEntriesFetchState.NetworkError(message)
    }

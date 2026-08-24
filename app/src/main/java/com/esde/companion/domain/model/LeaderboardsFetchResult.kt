package com.esde.companion.domain.model

/** Outcome of fetching a game's leaderboard list - mirrors [AchievementSummaryFetchResult]'s shape. */
sealed class LeaderboardsFetchResult {
    data class Success(val summary: GameLeaderboardsSummary) : LeaderboardsFetchResult()

    data object NotFound : LeaderboardsFetchResult()

    data class NetworkError(val message: String) : LeaderboardsFetchResult()
}

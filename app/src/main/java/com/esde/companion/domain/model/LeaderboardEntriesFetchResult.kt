package com.esde.companion.domain.model

/** Outcome of fetching one leaderboard's entries - mirrors [AchievementCommentsFetchResult]'s shape. */
sealed class LeaderboardEntriesFetchResult {
    data class Success(val entries: List<LeaderboardEntry>) : LeaderboardEntriesFetchResult()

    data class NetworkError(val message: String) : LeaderboardEntriesFetchResult()
}

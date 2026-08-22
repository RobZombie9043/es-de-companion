package com.esde.companion.domain.model

/** A game's full list of RetroAchievements leaderboards, already merged with the signed-in user's own entries. */
data class GameLeaderboardsSummary(
    val gameId: Long,
    val leaderboards: List<LeaderboardSummary>,
)

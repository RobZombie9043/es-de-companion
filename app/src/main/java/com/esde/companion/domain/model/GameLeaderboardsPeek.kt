package com.esde.companion.domain.model

/**
 * A cache-only "do we already have something to show" read for a game's leaderboard list -
 * never triggers a network fetch, see `RetroAchievementsRepository.peekGameLeaderboards`. Mirrors
 * [AchievementSummaryPeek]'s shape/reasoning for the Leaderboards facet.
 */
data class GameLeaderboardsPeek(
    val summary: GameLeaderboardsSummary,
    val isStale: Boolean,
)

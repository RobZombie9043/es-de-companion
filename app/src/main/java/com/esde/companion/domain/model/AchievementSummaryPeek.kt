package com.esde.companion.domain.model

/**
 * A cache-only "do we already have something to show" read for a game's achievement summary -
 * never triggers a network fetch, see `RetroAchievementsRepository.peekAchievementSummary`.
 * [isStale] tells the caller whether to also kick off a real fetch in the background after
 * displaying [summary] immediately (stale-while-revalidate), matching the same TTL
 * `AchievementSummaryCache.getSummary` itself would use to decide whether to refetch.
 */
data class AchievementSummaryPeek(
    val summary: GameAchievementSummary,
    val isStale: Boolean,
)

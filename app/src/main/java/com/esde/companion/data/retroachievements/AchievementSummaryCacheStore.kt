package com.esde.companion.data.retroachievements

import com.esde.companion.domain.model.GameAchievementSummary

/** A cached [GameAchievementSummary] for one (username, gameId), and when it was fetched. */
data class CachedAchievementSummary(
    val fetchedAtMillis: Long,
    val summary: GameAchievementSummary,
)

/**
 * Persists [AchievementSummaryCache]'s per-(username, gameId) entries. A seam purely for
 * testability - same reasoning as [GameListCacheStore] - so [AchievementSummaryCache]'s
 * TTL/stale-fallback/peek logic can be unit-tested against a hand-rolled fake instead of a real
 * [FileAchievementSummaryCacheStore], which needs a real Android [android.content.Context] to
 * construct.
 */
interface AchievementSummaryCacheStore {
    suspend fun read(
        username: String,
        gameId: Long,
    ): CachedAchievementSummary?

    suspend fun write(
        username: String,
        gameId: Long,
        cached: CachedAchievementSummary,
    )
}

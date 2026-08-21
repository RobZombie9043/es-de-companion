package com.esde.companion.data.retroachievements

import com.esde.companion.domain.model.AchievementCommentsFetchResult
import java.time.Clock
import java.util.concurrent.TimeUnit

private const val CACHE_TTL_MINUTES = 30L
private val CACHE_TTL_MILLIS = TimeUnit.MINUTES.toMillis(CACHE_TTL_MINUTES)

private data class CachedComments(val fetchedAtMillis: Long, val result: AchievementCommentsFetchResult)

/**
 * One cached [RetroAchievementsApi.getAchievementComments] fetch per achievementId - unlike
 * [AchievementSummaryCache], this is public wall data (not per-user), so it's keyed by
 * achievementId alone. In-memory only, same "no natural bound, not worth a disk eviction
 * policy" reasoning as [AchievementSummaryCache], with a longer 30-minute TTL since comments
 * change far less often than unlock status and this is only ever fetched on-demand (a row
 * being expanded), not for the whole visible list.
 */
class AchievementCommentsCache(
    private val clock: Clock = Clock.systemUTC(),
) {
    private val memoryCache = mutableMapOf<Long, CachedComments>()

    suspend fun getComments(
        achievementId: Long,
        fetch: suspend () -> AchievementCommentsFetchResult,
    ): AchievementCommentsFetchResult {
        val cached = memoryCache[achievementId]
        if (cached != null && !isStale(cached.fetchedAtMillis)) {
            return cached.result
        }

        val fresh = fetch()
        return if (fresh is AchievementCommentsFetchResult.NetworkError) {
            cached?.result ?: fresh
        } else {
            memoryCache[achievementId] = CachedComments(clock.millis(), fresh)
            fresh
        }
    }

    private fun isStale(fetchedAtMillis: Long) = clock.millis() - fetchedAtMillis > CACHE_TTL_MILLIS
}

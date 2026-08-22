package com.esde.companion.data.retroachievements

import com.esde.companion.domain.model.LeaderboardEntriesFetchResult
import java.time.Clock
import java.util.concurrent.TimeUnit

private const val CACHE_TTL_MINUTES = 15L
private val CACHE_TTL_MILLIS = TimeUnit.MINUTES.toMillis(CACHE_TTL_MINUTES)

private data class CachedEntries(val fetchedAtMillis: Long, val result: LeaderboardEntriesFetchResult)

/**
 * One cached [RetroAchievementsApi.getLeaderboardEntries] fetch per leaderboardId - unlike
 * [GameLeaderboardsCache], a leaderboard's top entries are public data (not per-user), so this is
 * keyed by leaderboardId alone, same shape as [AchievementCommentsCache]. In-memory only, same
 * "no natural bound" reasoning as the other caches in this package, only ever fetched on-demand
 * (a leaderboard row being expanded). A shorter 15-minute TTL than [AchievementCommentsCache]'s 30
 * - ranks shift more often than wall comments accumulate - and no `forceRefresh` param, same as
 * [AchievementCommentsCache].
 */
class LeaderboardEntriesCache(
    private val clock: Clock = Clock.systemUTC(),
) {
    private val memoryCache = mutableMapOf<Long, CachedEntries>()

    suspend fun getEntries(
        leaderboardId: Long,
        fetch: suspend () -> LeaderboardEntriesFetchResult,
    ): LeaderboardEntriesFetchResult {
        val cached = memoryCache[leaderboardId]
        if (cached != null && !isStale(cached.fetchedAtMillis)) {
            return cached.result
        }

        val fresh = fetch()
        return if (fresh is LeaderboardEntriesFetchResult.NetworkError) {
            cached?.result ?: fresh
        } else {
            memoryCache[leaderboardId] = CachedEntries(clock.millis(), fresh)
            fresh
        }
    }

    private fun isStale(fetchedAtMillis: Long) = clock.millis() - fetchedAtMillis > CACHE_TTL_MILLIS
}

package com.esde.companion.data.retroachievements

import com.esde.companion.domain.model.GameLeaderboardsPeek
import com.esde.companion.domain.model.LeaderboardsFetchResult
import java.time.Clock
import java.util.concurrent.TimeUnit

private const val CACHE_TTL_MINUTES = 15L
private val CACHE_TTL_MILLIS = TimeUnit.MINUTES.toMillis(CACHE_TTL_MINUTES)

private data class LeaderboardsCacheKey(val username: String, val gameId: Long)

private data class CachedLeaderboards(val fetchedAtMillis: Long, val result: LeaderboardsFetchResult)

/**
 * One cached [RetroAchievementsApi.getGameLeaderboards] fetch per (username, gameId) - keyed by
 * username too, same as [AchievementSummaryCache], since each leaderboard row's `myEntry` carries
 * per-user rank/score data that must never be served to a different signed-in account.
 *
 * In-memory only - unlike [AchievementSummaryCache], this cache isn't disk-backed. Its key space
 * has the same "no natural bound" shape [AchievementSummaryCache] used to have before it grew a
 * disk store, but this one hasn't been given the same treatment (deliberately out of scope, see
 * that class's kdoc history) since the pane it backs doesn't need to survive a process restart
 * the way the achievement screen's primary content does. Same 15-minute TTL and `forceRefresh`
 * bypass wired to the achievement screen's kebab "Refresh" entry (refreshed alongside the
 * achievement summary in a single fetch).
 *
 * Only [LeaderboardsFetchResult.Success]/[LeaderboardsFetchResult.NotFound] are ever written to
 * the cache - a [LeaderboardsFetchResult.NetworkError] falls back to the last cached entry for
 * that key if one exists, otherwise is returned as-is and left uncached, same as
 * [AchievementSummaryCache].
 */
class GameLeaderboardsCache(
    private val clock: Clock = Clock.systemUTC(),
) {
    private val memoryCache = mutableMapOf<LeaderboardsCacheKey, CachedLeaderboards>()

    suspend fun getLeaderboards(
        username: String,
        gameId: Long,
        forceRefresh: Boolean = false,
        fetch: suspend () -> LeaderboardsFetchResult,
    ): LeaderboardsFetchResult {
        val key = LeaderboardsCacheKey(username, gameId)
        val cached = memoryCache[key]
        if (!forceRefresh && cached != null && !isStale(cached.fetchedAtMillis)) {
            return cached.result
        }

        val fresh = fetch()
        return if (fresh is LeaderboardsFetchResult.NetworkError) {
            cached?.result ?: fresh
        } else {
            memoryCache[key] = CachedLeaderboards(clock.millis(), fresh)
            fresh
        }
    }

    /**
     * A cache-only peek - never calls the network, `null` only if nothing is cached in memory
     * for this key. Backs stale-while-revalidate display, mirroring
     * [AchievementSummaryCache.peek].
     */
    suspend fun peek(
        username: String,
        gameId: Long,
    ): GameLeaderboardsPeek? {
        val cached = memoryCache[LeaderboardsCacheKey(username, gameId)]
        val result = cached?.result as? LeaderboardsFetchResult.Success ?: return null
        return GameLeaderboardsPeek(result.summary, isStale(cached.fetchedAtMillis))
    }

    private fun isStale(fetchedAtMillis: Long) = clock.millis() - fetchedAtMillis > CACHE_TTL_MILLIS
}

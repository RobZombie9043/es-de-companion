package com.esde.companion.data.retroachievements

import com.esde.companion.domain.model.AchievementSummaryFetchResult
import com.esde.companion.domain.model.AchievementSummaryPeek
import java.time.Clock
import java.util.concurrent.TimeUnit

private const val CACHE_TTL_MINUTES = 15L
private val CACHE_TTL_MILLIS = TimeUnit.MINUTES.toMillis(CACHE_TTL_MINUTES)

private data class CacheKey(val username: String, val gameId: Long)

private data class CachedSummary(val fetchedAtMillis: Long, val result: AchievementSummaryFetchResult)

/**
 * One cached [RetroAchievementsApi.getGameInfoAndUserProgress] (plus the `GetGameExtended`
 * achievement-type workaround) fetch per (username, gameId) - unlike [GameListCache], this is
 * keyed by username too, since [com.esde.companion.domain.model.GameAchievementSummary] carries
 * per-user unlock/points data that must never be served to a different signed-in account.
 *
 * Disk-backed via [store] ([FileAchievementSummaryCacheStore] in production - one flat file per
 * key rather than DataStore Preferences, since this cache's key space, unlike [GameListCache]/
 * [UserProgressCache]'s, has no natural bound - see that class's kdoc for why and its pruning
 * policy). In-memory caching for the process lifetime sits on top of [store]'s disk-backed
 * cache, same shape as [GameListCache]. A 15-minute TTL - shorter than [UserProgressCache]'s
 * 1 hour, since this is the screen a user is actively watching right after playing/unlocking
 * something - keeps it feeling current while still absorbing rapid repeat navigation.
 *
 * Only [AchievementSummaryFetchResult.Success] is ever written to [store] - a
 * [AchievementSummaryFetchResult.NotFound] is cached in memory only (same as before disk-backing
 * was added), since there's no summary to usefully persist for [peek] to return, and a
 * [AchievementSummaryFetchResult.NetworkError] falls back to the last cached entry for that key
 * if one exists (checking memory, then disk), otherwise is returned as-is and left uncached, so
 * a transient failure doesn't pin an error state for the full TTL.
 */
class AchievementSummaryCache(
    private val store: AchievementSummaryCacheStore,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val memoryCache = mutableMapOf<CacheKey, CachedSummary>()

    suspend fun getSummary(
        username: String,
        gameId: Long,
        forceRefresh: Boolean = false,
        fetch: suspend () -> AchievementSummaryFetchResult,
    ): AchievementSummaryFetchResult {
        val key = CacheKey(username, gameId)
        val cached = memoryCache[key] ?: readThroughDisk(username, gameId, key)
        if (!forceRefresh && cached != null && !isStale(cached.fetchedAtMillis)) {
            return cached.result
        }

        val fresh = fetch()
        return if (fresh is AchievementSummaryFetchResult.NetworkError) {
            cached?.result ?: fresh
        } else {
            val fetchedAtMillis = clock.millis()
            memoryCache[key] = CachedSummary(fetchedAtMillis, fresh)
            if (fresh is AchievementSummaryFetchResult.Success) {
                store.write(username, gameId, CachedAchievementSummary(fetchedAtMillis, fresh.summary))
            }
            fresh
        }
    }

    /**
     * A cache-only peek - never calls [store]/the network, `null` only if genuinely nothing is
     * cached anywhere for this key (memory or disk). Backs stale-while-revalidate display: a
     * caller shows [AchievementSummaryPeek.summary] immediately, then decides whether to also
     * call [getSummary] based on [AchievementSummaryPeek.isStale].
     */
    suspend fun peek(
        username: String,
        gameId: Long,
    ): AchievementSummaryPeek? {
        val key = CacheKey(username, gameId)
        val cached = memoryCache[key] ?: readThroughDisk(username, gameId, key)
        val result = cached?.result as? AchievementSummaryFetchResult.Success ?: return null
        return AchievementSummaryPeek(result.summary, isStale(cached.fetchedAtMillis))
    }

    private suspend fun readThroughDisk(
        username: String,
        gameId: Long,
        key: CacheKey,
    ): CachedSummary? {
        val onDisk = store.read(username, gameId) ?: return null
        val entry = CachedSummary(onDisk.fetchedAtMillis, AchievementSummaryFetchResult.Success(onDisk.summary))
        memoryCache[key] = entry
        return entry
    }

    private fun isStale(fetchedAtMillis: Long) = clock.millis() - fetchedAtMillis > CACHE_TTL_MILLIS
}

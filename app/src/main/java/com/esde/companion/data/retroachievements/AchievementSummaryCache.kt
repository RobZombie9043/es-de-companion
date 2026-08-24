package com.esde.companion.data.retroachievements

import com.esde.companion.domain.model.AchievementSummaryFetchResult
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
 * In-memory only, no disk store - unlike [GameListCache]/[UserProgressCache], this cache has no
 * natural bound (one entry per game ever viewed), so persisting it indefinitely would need its
 * own eviction policy that isn't justified by the problem being solved: repeated views of the
 * same game within a session (switching games back and forth in ES-DE, browsing a system's game
 * list). A 15-minute TTL - shorter than [UserProgressCache]'s 1 hour, since this is the screen a
 * user is actively watching right after playing/unlocking something - keeps it feeling current
 * while still absorbing rapid repeat navigation.
 *
 * Only [AchievementSummaryFetchResult.Success]/[AchievementSummaryFetchResult.NotFound] are ever
 * written to the cache. A [AchievementSummaryFetchResult.NetworkError] falls back to the last
 * cached entry for that key if one exists (same stale-fallback reasoning as [GameListCache]/
 * [UserProgressCache]), otherwise is returned as-is and left uncached, so a transient failure
 * doesn't pin an error state for the full TTL.
 */
class AchievementSummaryCache(
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
        val cached = memoryCache[key]
        if (!forceRefresh && cached != null && !isStale(cached.fetchedAtMillis)) {
            return cached.result
        }

        val fresh = fetch()
        return if (fresh is AchievementSummaryFetchResult.NetworkError) {
            cached?.result ?: fresh
        } else {
            memoryCache[key] = CachedSummary(clock.millis(), fresh)
            fresh
        }
    }

    private fun isStale(fetchedAtMillis: Long) = clock.millis() - fetchedAtMillis > CACHE_TTL_MILLIS
}

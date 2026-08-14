package com.esde.companion.data.retroachievements

import com.esde.companion.domain.model.UserGameProgress
import java.time.Clock
import java.util.concurrent.TimeUnit

private const val PROGRESS_CACHE_TTL_MINUTES = 60L
private val PROGRESS_CACHE_TTL_MILLIS = TimeUnit.MINUTES.toMillis(PROGRESS_CACHE_TTL_MINUTES)
private const val PROGRESS_PAGE_SIZE = 500
private const val MAX_PROGRESS_PAGES = 40

/**
 * One cached [RetroAchievementsApi.getUserCompletionProgress] fetch, paginated until
 * exhausted, for the signed-in username - a single global, cross-console dataset (unlike
 * [GameListCache], which is keyed per console). 1-hour TTL, not [GameListCache]'s 24h -
 * completion data changes every time the user plays, where the game catalogue itself rarely
 * does. In-memory caching *also* respects this TTL (unlike [GameListCache]'s, which is
 * intentionally memory-permanent for the process lifetime) - wrong here, since this data goes
 * stale within a single session. [MAX_PROGRESS_PAGES] is a hard cap on pagination - an
 * unbounded loop against a remote endpoint is a real hang/rate-limit risk if the server-reported
 * total is ever wrong.
 *
 * A refresh failure (offline, rate-limited, or a page failing mid-pagination) falls back to
 * serving a stale cached result rather than failing outright - a possibly-outdated guess beats
 * no guess at all, same reasoning as [GameListCache].
 */
class UserProgressCache(
    private val store: UserProgressCacheStore,
    private val clock: Clock = Clock.systemUTC(),
) {
    private var memoryCache: Pair<String, CachedUserProgress>? = null

    suspend fun getProgress(
        username: String,
        api: RetroAchievementsApi,
    ): Map<Long, UserGameProgress> {
        val inMemory = memoryCache
        if (inMemory != null && inMemory.first == username && !isStale(inMemory.second.fetchedAtMillis)) {
            return inMemory.second.progressByGameId
        }

        val cached = store.read(username)
        val progress =
            if (cached != null && !isStale(cached.fetchedAtMillis)) {
                cached
            } else {
                refresh(username, api) ?: cached ?: CachedUserProgress(clock.millis(), emptyMap())
            }
        memoryCache = username to progress
        return progress.progressByGameId
    }

    private suspend fun refresh(
        username: String,
        api: RetroAchievementsApi,
    ): CachedUserProgress? {
        val allEntries = mutableListOf<UserGameProgress>()
        var offset = 0
        var total = Int.MAX_VALUE
        var page = 0
        while (offset < total && page < MAX_PROGRESS_PAGES) {
            val result = api.getUserCompletionProgress(username, offset, PROGRESS_PAGE_SIZE)
            if (result !is RetroAchievementsApiResult.Success) return null
            allEntries += result.data.entries
            total = result.data.total
            if (result.data.entries.isEmpty()) break
            offset += result.data.entries.size
            page++
        }
        val cached = CachedUserProgress(clock.millis(), allEntries.associateBy { it.gameId })
        store.write(username, cached)
        return cached
    }

    private fun isStale(fetchedAtMillis: Long) = clock.millis() - fetchedAtMillis > PROGRESS_CACHE_TTL_MILLIS
}

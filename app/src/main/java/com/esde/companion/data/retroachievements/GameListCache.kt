package com.esde.companion.data.retroachievements

import com.esde.companion.domain.model.RetroAchievementsCandidateGame
import com.esde.companion.domain.model.RetroAchievementsConsole
import java.time.Clock
import java.util.concurrent.TimeUnit

private const val CACHE_TTL_HOURS = 24L
private val CACHE_TTL_MILLIS = TimeUnit.HOURS.toMillis(CACHE_TTL_HOURS)

/**
 * One cached [RetroAchievementsApi.getGameList] fetch per console - api-kotlin itself flags
 * this endpoint as rate-limited/response-heavy (`@RequiresCache` on `RetroInterface`), so
 * caching is baseline for this integration, not a later optimization. Backs both the
 * automatic title matcher (`ResolveRetroAchievementsGameUseCase`) and the manual search
 * picker - both draw from the same cached list.
 *
 * A refresh failure (offline, rate-limited) falls back to serving a stale cached list rather
 * than failing identification outright - a possibly-outdated guess beats no guess at all.
 * In-memory caching for the process lifetime sits on top of [store]'s disk-backed cache.
 */
class GameListCache(
    private val store: GameListCacheStore,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val memoryCache = mutableMapOf<Long, List<RetroAchievementsCandidateGame>>()

    suspend fun getGames(
        console: RetroAchievementsConsole,
        api: RetroAchievementsApi,
    ): List<RetroAchievementsCandidateGame> {
        val inMemory = memoryCache[console.consoleId]
        if (inMemory != null) return inMemory

        val cached = store.read(console.consoleId)
        val games =
            if (cached != null && !isStale(cached.fetchedAtMillis)) {
                cached.games
            } else {
                refresh(console, api) ?: cached?.games ?: emptyList()
            }
        memoryCache[console.consoleId] = games
        return games
    }

    private suspend fun refresh(
        console: RetroAchievementsConsole,
        api: RetroAchievementsApi,
    ): List<RetroAchievementsCandidateGame>? {
        val result = api.getGameList(console.consoleId)
        if (result !is RetroAchievementsApiResult.Success) return null
        store.write(console.consoleId, CachedGameList(clock.millis(), result.data))
        return result.data
    }

    private fun isStale(fetchedAtMillis: Long) = clock.millis() - fetchedAtMillis > CACHE_TTL_MILLIS
}

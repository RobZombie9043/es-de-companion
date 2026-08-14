package com.esde.companion.data.retroachievements

import com.esde.companion.domain.model.RetroAchievementsCandidateGame
import com.esde.companion.domain.model.RetroAchievementsConsole
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

class GameListCacheTest {
    private class FakeGameListCacheStore(
        initial: CachedGameList? = null,
    ) : GameListCacheStore {
        var stored: CachedGameList? = initial

        override suspend fun read(consoleId: Long): CachedGameList? = stored

        override suspend fun write(
            consoleId: Long,
            cached: CachedGameList,
        ) {
            stored = cached
        }
    }

    private class FakeRetroAchievementsApi(
        private val result: GameListResult,
    ) : RetroAchievementsApi {
        var callCount = 0

        override suspend fun getUserSummary(username: String) = error("not used in this test")

        override suspend fun getGameList(consoleId: Long): GameListResult {
            callCount++
            return result
        }

        override suspend fun getGameInfoAndUserProgress(
            username: String,
            gameId: Long,
        ) = error("not used in this test")

        override suspend fun getUserCompletionProgress(
            username: String,
            offset: Int,
            count: Int,
        ) = error("not used in this test")
    }

    private val console = RetroAchievementsConsole.Snes
    private val now = Instant.parse("2026-08-12T12:00:00Z")
    private val fixedClock: Clock = Clock.fixed(now, ZoneOffset.UTC)
    private val oneDay = TimeUnit.DAYS.toMillis(1)

    private fun candidate(title: String) = RetroAchievementsCandidateGame(1L, title, iconUrl = null)

    @Test
    fun `a fresh cached list is served without calling the api`() =
        runTest {
            val cached =
                CachedGameList(fetchedAtMillis = now.toEpochMilli() - 1000, games = listOf(candidate("Cached")))
            val store = FakeGameListCacheStore(cached)
            val api = FakeRetroAchievementsApi(RetroAchievementsApiResult.Success(listOf(candidate("Fresh"))))
            val cache = GameListCache(store, fixedClock)

            val result = cache.getGames(console, api)

            assertEquals(cached.games, result)
            assertEquals(0, api.callCount)
        }

    @Test
    fun `a missing cache triggers a refresh and writes the result to the store`() =
        runTest {
            val store = FakeGameListCacheStore(initial = null)
            val fresh = listOf(candidate("Fresh"))
            val api = FakeRetroAchievementsApi(RetroAchievementsApiResult.Success(fresh))
            val cache = GameListCache(store, fixedClock)

            val result = cache.getGames(console, api)

            assertEquals(fresh, result)
            assertEquals(1, api.callCount)
            assertEquals(fresh, store.stored?.games)
            assertEquals(now.toEpochMilli(), store.stored?.fetchedAtMillis)
        }

    @Test
    fun `a cached list older than the TTL triggers a refresh rather than being served stale`() =
        runTest {
            val staleCached =
                CachedGameList(fetchedAtMillis = now.toEpochMilli() - oneDay - 1, games = listOf(candidate("Stale")))
            val store = FakeGameListCacheStore(staleCached)
            val fresh = listOf(candidate("Fresh"))
            val api = FakeRetroAchievementsApi(RetroAchievementsApiResult.Success(fresh))
            val cache = GameListCache(store, fixedClock)

            val result = cache.getGames(console, api)

            assertEquals(fresh, result)
            assertEquals(1, api.callCount)
        }

    @Test
    fun `a refresh failure falls back to the stale cached list instead of failing outright`() =
        runTest {
            val staleCached =
                CachedGameList(fetchedAtMillis = now.toEpochMilli() - oneDay - 1, games = listOf(candidate("Stale")))
            val store = FakeGameListCacheStore(staleCached)
            val api = FakeRetroAchievementsApi(RetroAchievementsApiResult.Error("offline"))
            val cache = GameListCache(store, fixedClock)

            val result = cache.getGames(console, api)

            assertEquals(staleCached.games, result)
        }

    @Test
    fun `a refresh failure with no cache at all returns an empty list rather than throwing`() =
        runTest {
            val store = FakeGameListCacheStore(initial = null)
            val api = FakeRetroAchievementsApi(RetroAchievementsApiResult.Error("offline"))
            val cache = GameListCache(store, fixedClock)

            val result = cache.getGames(console, api)

            assertEquals(emptyList<RetroAchievementsCandidateGame>(), result)
        }

    @Test
    fun `repeated calls within the same process only hit the api once, via the in-memory cache`() =
        runTest {
            val store = FakeGameListCacheStore(initial = null)
            val api = FakeRetroAchievementsApi(RetroAchievementsApiResult.Success(listOf(candidate("Fresh"))))
            val cache = GameListCache(store, fixedClock)

            cache.getGames(console, api)
            cache.getGames(console, api)

            assertEquals(1, api.callCount)
        }
}

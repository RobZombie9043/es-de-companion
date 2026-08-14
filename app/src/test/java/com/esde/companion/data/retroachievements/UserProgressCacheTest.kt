package com.esde.companion.data.retroachievements

import com.esde.companion.domain.model.ProgressStatus
import com.esde.companion.domain.model.UserGameProgress
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

class UserProgressCacheTest {
    private class FakeUserProgressCacheStore(
        private val initialUsername: String = "player1",
        initial: CachedUserProgress? = null,
    ) : UserProgressCacheStore {
        private val storedByUsername = mutableMapOf<String, CachedUserProgress>()

        init {
            if (initial != null) storedByUsername[initialUsername] = initial
        }

        val stored: CachedUserProgress?
            get() = storedByUsername[initialUsername]

        override suspend fun read(username: String): CachedUserProgress? = storedByUsername[username]

        override suspend fun write(
            username: String,
            cached: CachedUserProgress,
        ) {
            storedByUsername[username] = cached
        }
    }

    private class FakeRetroAchievementsApi(
        private val pages: List<RetroAchievementsApiResult<UserCompletionProgressPage>>,
    ) : RetroAchievementsApi {
        var callCount = 0
            private set

        override suspend fun getUserSummary(username: String) = error("not used in this test")

        override suspend fun getGameList(consoleId: Long) = error("not used in this test")

        override suspend fun getGameInfoAndUserProgress(
            username: String,
            gameId: Long,
        ) = error("not used in this test")

        override suspend fun getUserCompletionProgress(
            username: String,
            offset: Int,
            count: Int,
        ): RetroAchievementsApiResult<UserCompletionProgressPage> {
            val result = pages.getOrElse(callCount) { error("no more pages queued") }
            callCount++
            return result
        }
    }

    private val now = Instant.parse("2026-08-12T12:00:00Z")
    private val fixedClock: Clock = Clock.fixed(now, ZoneOffset.UTC)
    private val oneHour = TimeUnit.HOURS.toMillis(1)

    private fun progress(gameId: Long) = UserGameProgress(gameId, numAwarded = 1, maxPossible = 10, ProgressStatus.Some)

    private fun page(
        total: Int,
        vararg gameIds: Long,
    ) = UserCompletionProgressPage(total, gameIds.map { progress(it) })

    @Test
    fun `a fresh cached result is served without calling the api`() =
        runTest {
            val cached = CachedUserProgress(now.toEpochMilli() - 1000, mapOf(1L to progress(1L)))
            val store = FakeUserProgressCacheStore(initial = cached)
            val api = FakeRetroAchievementsApi(listOf(RetroAchievementsApiResult.Success(page(1, 99))))
            val cache = UserProgressCache(store, fixedClock)

            val result = cache.getProgress("player1", api)

            assertEquals(cached.progressByGameId, result)
            assertEquals(0, api.callCount)
        }

    @Test
    fun `a missing cache triggers a refresh and paginates until the reported total is reached`() =
        runTest {
            val store = FakeUserProgressCacheStore(initial = null)
            val pages =
                listOf(
                    RetroAchievementsApiResult.Success(page(total = 3, 1L, 2L)),
                    RetroAchievementsApiResult.Success(page(total = 3, 3L)),
                )
            val api = FakeRetroAchievementsApi(pages)
            val cache = UserProgressCache(store, fixedClock)

            val result = cache.getProgress("player1", api)

            assertEquals(setOf(1L, 2L, 3L), result.keys)
            assertEquals(2, api.callCount)
            assertEquals(now.toEpochMilli(), store.stored?.fetchedAtMillis)
        }

    @Test
    fun `a cached result older than the TTL triggers a refresh rather than being served stale`() =
        runTest {
            val staleCached = CachedUserProgress(now.toEpochMilli() - oneHour - 1, mapOf(9L to progress(9L)))
            val store = FakeUserProgressCacheStore(initial = staleCached)
            val api = FakeRetroAchievementsApi(listOf(RetroAchievementsApiResult.Success(page(total = 1, 1L))))
            val cache = UserProgressCache(store, fixedClock)

            val result = cache.getProgress("player1", api)

            assertEquals(setOf(1L), result.keys)
            assertEquals(1, api.callCount)
        }

    @Test
    fun `a refresh failure falls back to the stale cached result instead of failing outright`() =
        runTest {
            val staleCached = CachedUserProgress(now.toEpochMilli() - oneHour - 1, mapOf(9L to progress(9L)))
            val store = FakeUserProgressCacheStore(initial = staleCached)
            val api = FakeRetroAchievementsApi(listOf(RetroAchievementsApiResult.Error("offline")))
            val cache = UserProgressCache(store, fixedClock)

            val result = cache.getProgress("player1", api)

            assertEquals(staleCached.progressByGameId, result)
        }

    @Test
    fun `a refresh failure with no cache at all returns an empty map rather than throwing`() =
        runTest {
            val store = FakeUserProgressCacheStore(initial = null)
            val api = FakeRetroAchievementsApi(listOf(RetroAchievementsApiResult.Error("offline")))
            val cache = UserProgressCache(store, fixedClock)

            val result = cache.getProgress("player1", api)

            assertEquals(emptyMap<Long, UserGameProgress>(), result)
        }

    @Test
    fun `a failure on a later page falls back to the stale cached result rather than a partial page`() =
        runTest {
            val staleCached = CachedUserProgress(now.toEpochMilli() - oneHour - 1, mapOf(9L to progress(9L)))
            val store = FakeUserProgressCacheStore(initial = staleCached)
            val pages =
                listOf(
                    RetroAchievementsApiResult.Success(page(total = 3, 1L, 2L)),
                    RetroAchievementsApiResult.Error("rate limited"),
                )
            val api = FakeRetroAchievementsApi(pages)
            val cache = UserProgressCache(store, fixedClock)

            val result = cache.getProgress("player1", api)

            assertEquals(staleCached.progressByGameId, result)
        }

    @Test
    fun `repeated calls for the same username within the same process only hit the api once`() =
        runTest {
            val store = FakeUserProgressCacheStore(initial = null)
            val api = FakeRetroAchievementsApi(listOf(RetroAchievementsApiResult.Success(page(total = 1, 1L))))
            val cache = UserProgressCache(store, fixedClock)

            cache.getProgress("player1", api)
            cache.getProgress("player1", api)

            assertEquals(1, api.callCount)
        }

    @Test
    fun `a different username is not served from another user's in-memory cache`() =
        runTest {
            val store = FakeUserProgressCacheStore(initial = null)
            val cache = UserProgressCache(store, fixedClock)
            val firstUserPages = listOf(RetroAchievementsApiResult.Success(page(total = 1, 1L)))
            val secondUserPages = listOf(RetroAchievementsApiResult.Success(page(total = 1, 2L)))
            val firstUserApi = FakeRetroAchievementsApi(firstUserPages)
            val secondUserApi = FakeRetroAchievementsApi(secondUserPages)

            cache.getProgress("player1", firstUserApi)
            val secondUserResult = cache.getProgress("player2", secondUserApi)

            assertEquals(setOf(2L), secondUserResult.keys)
            assertEquals(1, secondUserApi.callCount)
        }
}

package com.esde.companion.data.retroachievements

import com.esde.companion.domain.model.AchievementSummaryFetchResult
import com.esde.companion.domain.model.GameAchievementSummary
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

class AchievementSummaryCacheTest {
    /** Lets a single test advance time mid-test, unlike [Clock.fixed] - needed for the TTL-advance
     * cases below. */
    private class MutableClock(private var instant: Instant) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock = this

        override fun instant(): Instant = instant

        fun advanceBy(millis: Long) {
            instant = instant.plusMillis(millis)
        }
    }

    /** Keyed by (username, gameId), same as the real [FileAchievementSummaryCacheStore] - a
     * single shared `stored` field (like [GameListCacheTest]'s fake) would wrongly let one
     * username's entry leak into another's read. */
    private class FakeAchievementSummaryCacheStore(
        initial: CachedAchievementSummary? = null,
        private val initialUsername: String = "player1",
        private val initialGameId: Long = 1L,
    ) : AchievementSummaryCacheStore {
        private val entries = mutableMapOf<Pair<String, Long>, CachedAchievementSummary>()
        var writeCount = 0

        init {
            initial?.let { entries[initialUsername to initialGameId] = it }
        }

        val stored: CachedAchievementSummary?
            get() = entries[initialUsername to initialGameId]

        override suspend fun read(
            username: String,
            gameId: Long,
        ): CachedAchievementSummary? = entries[username to gameId]

        override suspend fun write(
            username: String,
            gameId: Long,
            cached: CachedAchievementSummary,
        ) {
            entries[username to gameId] = cached
            writeCount++
        }
    }

    private val now = Instant.parse("2026-08-12T12:00:00Z")
    private val fixedClock: Clock = Clock.fixed(now, ZoneOffset.UTC)
    private val fifteenMinutes = TimeUnit.MINUTES.toMillis(15)

    private fun summary(title: String): AchievementSummaryFetchResult.Success {
        val game =
            GameAchievementSummary(
                gameId = 1L,
                gameTitle = title,
                totalPoints = 100,
                earnedPoints = 0,
                completionPercent = 0f,
                achievements = emptyList(),
            )
        return AchievementSummaryFetchResult.Success(game)
    }

    @Test
    fun `a fresh cached result is served without calling fetch`() =
        runTest {
            val cache = AchievementSummaryCache(FakeAchievementSummaryCacheStore(), fixedClock)
            var callCount = 0
            cache.getSummary("player1", 1L) {
                callCount++
                summary("First")
            }

            val result =
                cache.getSummary("player1", 1L) {
                    callCount++
                    summary("Second")
                }

            assertEquals(summary("First"), result)
            assertEquals(1, callCount)
        }

    @Test
    fun `a cached result older than the TTL triggers a refresh rather than being served stale`() =
        runTest {
            val clock = MutableClock(now)
            val cache = AchievementSummaryCache(FakeAchievementSummaryCacheStore(), clock)
            cache.getSummary("player1", 1L) { summary("First") }

            clock.advanceBy(fifteenMinutes + 1)
            var callCount = 0
            val result =
                cache.getSummary("player1", 1L) {
                    callCount++
                    summary("Second")
                }

            assertEquals(summary("Second"), result)
            assertEquals(1, callCount)
        }

    @Test
    fun `forceRefresh bypasses a fresh cached result`() =
        runTest {
            val cache = AchievementSummaryCache(FakeAchievementSummaryCacheStore(), fixedClock)
            cache.getSummary("player1", 1L) { summary("First") }

            var callCount = 0
            val result =
                cache.getSummary("player1", 1L, forceRefresh = true) {
                    callCount++
                    summary("Second")
                }

            assertEquals(summary("Second"), result)
            assertEquals(1, callCount)
        }

    @Test
    fun `a NetworkError falls back to a prior cached Success instead of overwriting it`() =
        runTest {
            val cache = AchievementSummaryCache(FakeAchievementSummaryCacheStore(), fixedClock)
            cache.getSummary("player1", 1L) { summary("First") }

            val result =
                cache.getSummary("player1", 1L, forceRefresh = true) {
                    AchievementSummaryFetchResult.NetworkError("offline")
                }

            assertEquals(summary("First"), result)
        }

    @Test
    fun `a NetworkError with no prior cache is returned as-is`() =
        runTest {
            val cache = AchievementSummaryCache(FakeAchievementSummaryCacheStore(), fixedClock)

            val result = cache.getSummary("player1", 1L) { AchievementSummaryFetchResult.NetworkError("offline") }

            assertEquals(AchievementSummaryFetchResult.NetworkError("offline"), result)
        }

    @Test
    fun `NotFound is cached the same as Success and served without refetching`() =
        runTest {
            val cache = AchievementSummaryCache(FakeAchievementSummaryCacheStore(), fixedClock)
            var callCount = 0
            cache.getSummary("player1", 1L) {
                callCount++
                AchievementSummaryFetchResult.NotFound
            }

            val result =
                cache.getSummary("player1", 1L) {
                    callCount++
                    summary("Second")
                }

            assertEquals(AchievementSummaryFetchResult.NotFound, result)
            assertEquals(1, callCount)
        }

    @Test
    fun `different usernames for the same gameId do not share a cache entry`() =
        runTest {
            val cache = AchievementSummaryCache(FakeAchievementSummaryCacheStore(), fixedClock)
            cache.getSummary("player1", 1L) { summary("Player1Game") }

            var callCount = 0
            val result =
                cache.getSummary("player2", 1L) {
                    callCount++
                    summary("Player2Game")
                }

            assertEquals(summary("Player2Game"), result)
            assertEquals(1, callCount)
        }

    @Test
    fun `a fresh Success fetch is written through to the store`() =
        runTest {
            val store = FakeAchievementSummaryCacheStore()
            val cache = AchievementSummaryCache(store, fixedClock)

            cache.getSummary("player1", 1L) { summary("First") }

            assertEquals(now.toEpochMilli(), store.stored?.fetchedAtMillis)
            assertEquals("First", store.stored?.summary?.gameTitle)
        }

    @Test
    fun `NotFound is never written to the store`() =
        runTest {
            val store = FakeAchievementSummaryCacheStore()
            val cache = AchievementSummaryCache(store, fixedClock)

            cache.getSummary("player1", 1L) { AchievementSummaryFetchResult.NotFound }

            assertNull(store.stored)
            assertEquals(0, store.writeCount)
        }

    @Test
    fun `a memory miss reads through the store instead of calling fetch`() =
        runTest {
            val onDisk = CachedAchievementSummary(now.toEpochMilli(), summary("FromDisk").summary)
            val store = FakeAchievementSummaryCacheStore(onDisk)
            val cache = AchievementSummaryCache(store, fixedClock)

            var callCount = 0
            val result =
                cache.getSummary("player1", 1L) {
                    callCount++
                    summary("FromNetwork")
                }

            assertEquals("FromDisk", (result as AchievementSummaryFetchResult.Success).summary.gameTitle)
            assertEquals(0, callCount)
        }

    @Test
    fun `peek returns null when nothing is cached anywhere`() =
        runTest {
            val cache = AchievementSummaryCache(FakeAchievementSummaryCacheStore(), fixedClock)

            assertNull(cache.peek("player1", 1L))
        }

    @Test
    fun `peek returns a fresh in-memory hit as not stale`() =
        runTest {
            val cache = AchievementSummaryCache(FakeAchievementSummaryCacheStore(), fixedClock)
            cache.getSummary("player1", 1L) { summary("First") }

            val peeked = cache.peek("player1", 1L)

            assertEquals("First", peeked?.summary?.gameTitle)
            assertEquals(false, peeked?.isStale)
        }

    @Test
    fun `peek reports a TTL-expired entry as stale without triggering a fetch`() =
        runTest {
            val clock = MutableClock(now)
            val cache = AchievementSummaryCache(FakeAchievementSummaryCacheStore(), clock)
            cache.getSummary("player1", 1L) { summary("First") }
            clock.advanceBy(fifteenMinutes + 1)

            val peeked = cache.peek("player1", 1L)

            assertEquals("First", peeked?.summary?.gameTitle)
            assertTrue(peeked?.isStale == true)
        }

    @Test
    fun `peek populates the memory cache from a disk-only entry`() =
        runTest {
            val onDisk = CachedAchievementSummary(now.toEpochMilli(), summary("FromDisk").summary)
            val store = FakeAchievementSummaryCacheStore(onDisk)
            val cache = AchievementSummaryCache(store, fixedClock)

            val peeked = cache.peek("player1", 1L)

            assertEquals("FromDisk", peeked?.summary?.gameTitle)
            var callCount = 0
            cache.getSummary("player1", 1L) {
                callCount++
                summary("FromNetwork")
            }
            assertEquals(0, callCount)
        }

    @Test
    fun `peek returns null for a cached NotFound result`() =
        runTest {
            val cache = AchievementSummaryCache(FakeAchievementSummaryCacheStore(), fixedClock)
            cache.getSummary("player1", 1L) { AchievementSummaryFetchResult.NotFound }

            assertNull(cache.peek("player1", 1L))
        }
}

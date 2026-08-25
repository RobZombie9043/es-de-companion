package com.esde.companion.data.retroachievements

import com.esde.companion.domain.model.AchievementSummaryFetchResult
import com.esde.companion.domain.model.GameAchievementSummary
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

class AchievementSummaryCacheTest {
    /** Lets a single test advance time mid-test, unlike [Clock.fixed] - needed since this cache
     * has no external store to seed with an already-stale entry (see [GameListCacheTest] for
     * that pattern), only an in-memory map written by the cache itself. */
    private class MutableClock(private var instant: Instant) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock = this

        override fun instant(): Instant = instant

        fun advanceBy(millis: Long) {
            instant = instant.plusMillis(millis)
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
            val cache = AchievementSummaryCache(fixedClock)
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
            val cache = AchievementSummaryCache(clock)
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
            val cache = AchievementSummaryCache(fixedClock)
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
            val cache = AchievementSummaryCache(fixedClock)
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
            val cache = AchievementSummaryCache(fixedClock)

            val result = cache.getSummary("player1", 1L) { AchievementSummaryFetchResult.NetworkError("offline") }

            assertEquals(AchievementSummaryFetchResult.NetworkError("offline"), result)
        }

    @Test
    fun `NotFound is cached the same as Success and served without refetching`() =
        runTest {
            val cache = AchievementSummaryCache(fixedClock)
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
            val cache = AchievementSummaryCache(fixedClock)
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
}

package com.esde.companion.data.retroachievements

import com.esde.companion.domain.model.GameLeaderboardsSummary
import com.esde.companion.domain.model.LeaderboardsFetchResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

class GameLeaderboardsCacheTest {
    /** Same "advance time mid-test" pattern as [AchievementSummaryCacheTest]'s equivalent. */
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

    private fun leaderboards(gameId: Long = 1L) =
        LeaderboardsFetchResult.Success(GameLeaderboardsSummary(gameId = gameId, leaderboards = emptyList()))

    @Test
    fun `a fresh cached result is served without calling fetch`() =
        runTest {
            val cache = GameLeaderboardsCache(fixedClock)
            var callCount = 0
            cache.getLeaderboards("player1", 1L) {
                callCount++
                leaderboards()
            }

            val result =
                cache.getLeaderboards("player1", 1L) {
                    callCount++
                    leaderboards()
                }

            assertEquals(leaderboards(), result)
            assertEquals(1, callCount)
        }

    @Test
    fun `a cached result older than the TTL triggers a refresh rather than being served stale`() =
        runTest {
            val clock = MutableClock(now)
            val cache = GameLeaderboardsCache(clock)
            cache.getLeaderboards("player1", 1L) { leaderboards() }

            clock.advanceBy(fifteenMinutes + 1)
            var callCount = 0
            val result =
                cache.getLeaderboards("player1", 1L) {
                    callCount++
                    leaderboards()
                }

            assertEquals(leaderboards(), result)
            assertEquals(1, callCount)
        }

    @Test
    fun `forceRefresh bypasses a fresh cached result`() =
        runTest {
            val cache = GameLeaderboardsCache(fixedClock)
            cache.getLeaderboards("player1", 1L) { leaderboards() }

            var callCount = 0
            val result =
                cache.getLeaderboards("player1", 1L, forceRefresh = true) {
                    callCount++
                    leaderboards()
                }

            assertEquals(leaderboards(), result)
            assertEquals(1, callCount)
        }

    @Test
    fun `a NetworkError falls back to a prior cached Success instead of overwriting it`() =
        runTest {
            val cache = GameLeaderboardsCache(fixedClock)
            cache.getLeaderboards("player1", 1L) { leaderboards() }

            val result =
                cache.getLeaderboards("player1", 1L, forceRefresh = true) {
                    LeaderboardsFetchResult.NetworkError("offline")
                }

            assertEquals(leaderboards(), result)
        }

    @Test
    fun `a NetworkError with no prior cache is returned as-is`() =
        runTest {
            val cache = GameLeaderboardsCache(fixedClock)

            val result = cache.getLeaderboards("player1", 1L) { LeaderboardsFetchResult.NetworkError("offline") }

            assertEquals(LeaderboardsFetchResult.NetworkError("offline"), result)
        }

    @Test
    fun `different usernames for the same gameId do not share a cache entry`() =
        runTest {
            val cache = GameLeaderboardsCache(fixedClock)
            cache.getLeaderboards("player1", 1L) { leaderboards() }

            var callCount = 0
            val result =
                cache.getLeaderboards("player2", 1L) {
                    callCount++
                    leaderboards()
                }

            assertEquals(leaderboards(), result)
            assertEquals(1, callCount)
        }
}

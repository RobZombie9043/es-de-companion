package com.esde.companion.data.retroachievements

import com.esde.companion.domain.model.LeaderboardEntriesFetchResult
import com.esde.companion.domain.model.LeaderboardEntry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

class LeaderboardEntriesCacheTest {
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

    private fun entries(user: String): LeaderboardEntriesFetchResult {
        val entry =
            LeaderboardEntry(rank = 1L, user = user, formattedScore = "100", submittedAtMillis = now.toEpochMilli())
        return LeaderboardEntriesFetchResult.Success(listOf(entry))
    }

    @Test
    fun `a fresh cached result is served without calling fetch`() =
        runTest {
            val cache = LeaderboardEntriesCache(fixedClock)
            var callCount = 0
            cache.getEntries(1L) {
                callCount++
                entries("First")
            }

            val result =
                cache.getEntries(1L) {
                    callCount++
                    entries("Second")
                }

            assertEquals(entries("First"), result)
            assertEquals(1, callCount)
        }

    @Test
    fun `a cached result older than the TTL triggers a refresh rather than being served stale`() =
        runTest {
            val clock = MutableClock(now)
            val cache = LeaderboardEntriesCache(clock)
            cache.getEntries(1L) { entries("First") }

            clock.advanceBy(fifteenMinutes + 1)
            var callCount = 0
            val result =
                cache.getEntries(1L) {
                    callCount++
                    entries("Second")
                }

            assertEquals(entries("Second"), result)
            assertEquals(1, callCount)
        }

    @Test
    fun `a NetworkError falls back to a prior cached Success instead of overwriting it`() =
        runTest {
            val cache = LeaderboardEntriesCache(fixedClock)
            cache.getEntries(1L) { entries("First") }

            val result = cache.getEntries(1L) { LeaderboardEntriesFetchResult.NetworkError("offline") }

            assertEquals(entries("First"), result)
        }

    @Test
    fun `a NetworkError with no prior cache is returned as-is`() =
        runTest {
            val cache = LeaderboardEntriesCache(fixedClock)

            val result = cache.getEntries(1L) { LeaderboardEntriesFetchResult.NetworkError("offline") }

            assertEquals(LeaderboardEntriesFetchResult.NetworkError("offline"), result)
        }

    @Test
    fun `different leaderboardIds do not share a cache entry`() =
        runTest {
            val cache = LeaderboardEntriesCache(fixedClock)
            cache.getEntries(1L) { entries("Leaderboard1Entry") }

            var callCount = 0
            val result =
                cache.getEntries(2L) {
                    callCount++
                    entries("Leaderboard2Entry")
                }

            assertEquals(entries("Leaderboard2Entry"), result)
            assertEquals(1, callCount)
        }
}

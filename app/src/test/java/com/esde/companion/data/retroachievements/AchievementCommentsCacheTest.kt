package com.esde.companion.data.retroachievements

import com.esde.companion.domain.model.AchievementComment
import com.esde.companion.domain.model.AchievementCommentsFetchResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

class AchievementCommentsCacheTest {
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
    private val thirtyMinutes = TimeUnit.MINUTES.toMillis(30)

    private fun comments(text: String) =
        AchievementCommentsFetchResult.Success(listOf(AchievementComment("player1", now.toEpochMilli(), text)))

    @Test
    fun `a fresh cached result is served without calling fetch`() =
        runTest {
            val cache = AchievementCommentsCache(fixedClock)
            var callCount = 0
            cache.getComments(1L) {
                callCount++
                comments("First")
            }

            val result =
                cache.getComments(1L) {
                    callCount++
                    comments("Second")
                }

            assertEquals(comments("First"), result)
            assertEquals(1, callCount)
        }

    @Test
    fun `a cached result older than the TTL triggers a refresh rather than being served stale`() =
        runTest {
            val clock = MutableClock(now)
            val cache = AchievementCommentsCache(clock)
            cache.getComments(1L) { comments("First") }

            clock.advanceBy(thirtyMinutes + 1)
            var callCount = 0
            val result =
                cache.getComments(1L) {
                    callCount++
                    comments("Second")
                }

            assertEquals(comments("Second"), result)
            assertEquals(1, callCount)
        }

    @Test
    fun `a NetworkError falls back to a prior cached Success instead of overwriting it`() =
        runTest {
            val cache = AchievementCommentsCache(fixedClock)
            cache.getComments(1L) { comments("First") }

            val result = cache.getComments(1L) { AchievementCommentsFetchResult.NetworkError("offline") }

            assertEquals(comments("First"), result)
        }

    @Test
    fun `a NetworkError with no prior cache is returned as-is`() =
        runTest {
            val cache = AchievementCommentsCache(fixedClock)

            val result = cache.getComments(1L) { AchievementCommentsFetchResult.NetworkError("offline") }

            assertEquals(AchievementCommentsFetchResult.NetworkError("offline"), result)
        }

    @Test
    fun `different achievementIds do not share a cache entry`() =
        runTest {
            val cache = AchievementCommentsCache(fixedClock)
            cache.getComments(1L) { comments("Achievement1Comment") }

            var callCount = 0
            val result =
                cache.getComments(2L) {
                    callCount++
                    comments("Achievement2Comment")
                }

            assertEquals(comments("Achievement2Comment"), result)
            assertEquals(1, callCount)
        }
}

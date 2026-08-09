package com.esde.companion.data.video

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProcessVideoPlaybackStateRepositoryTest {
    @Test
    fun `observeIsPlaying defaults to false`() =
        runTest {
            val repository = ProcessVideoPlaybackStateRepository()

            repository.observeIsPlaying().test {
                assertFalse(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `setIsPlaying(true) is reflected by observeIsPlaying`() =
        runTest {
            val repository = ProcessVideoPlaybackStateRepository()

            repository.observeIsPlaying().test {
                assertFalse(awaitItem())
                repository.setIsPlaying(true)
                assertTrue(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `setIsPlaying toggles back to false`() =
        runTest {
            val repository = ProcessVideoPlaybackStateRepository()
            repository.setIsPlaying(true)

            repository.observeIsPlaying().test {
                assertTrue(awaitItem())
                repository.setIsPlaying(false)
                assertFalse(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
}

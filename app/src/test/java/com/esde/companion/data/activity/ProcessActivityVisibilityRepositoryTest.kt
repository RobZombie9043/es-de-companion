package com.esde.companion.data.activity

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProcessActivityVisibilityRepositoryTest {
    @Test
    fun `observeIsVisible defaults to true`() =
        runTest {
            val repository = ProcessActivityVisibilityRepository()

            repository.observeIsVisible().test {
                assertTrue(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `setVisible(false) is reflected by observeIsVisible`() =
        runTest {
            val repository = ProcessActivityVisibilityRepository()

            repository.observeIsVisible().test {
                assertTrue(awaitItem())
                repository.setVisible(false)
                assertFalse(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `setVisible toggles back to true`() =
        runTest {
            val repository = ProcessActivityVisibilityRepository()
            repository.setVisible(false)

            repository.observeIsVisible().test {
                assertFalse(awaitItem())
                repository.setVisible(true)
                assertTrue(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
}

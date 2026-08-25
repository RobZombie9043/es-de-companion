package com.esde.companion.ui.retroachievements

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForceRefreshCoordinatorTest {
    @Test
    fun `consume is false before any request or retrigger`() {
        val coordinator = ForceRefreshCoordinator()

        assertFalse(coordinator.consume())
    }

    @Test
    fun `request bumps the trigger and consume reports true exactly once`() {
        val coordinator = ForceRefreshCoordinator()

        coordinator.request()

        assertEquals(1, coordinator.trigger.value)
        assertTrue(coordinator.consume())
        assertFalse(coordinator.consume())
    }

    @Test
    fun `retrigger bumps the trigger without forcing a cache bypass`() {
        val coordinator = ForceRefreshCoordinator()

        coordinator.retrigger()

        assertEquals(1, coordinator.trigger.value)
        assertFalse(coordinator.consume())
    }

    @Test
    fun `each call bumps the trigger further`() {
        val coordinator = ForceRefreshCoordinator()

        coordinator.retrigger()
        coordinator.request()
        coordinator.retrigger()

        assertEquals(3, coordinator.trigger.value)
    }
}

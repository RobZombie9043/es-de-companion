package com.esde.companion.domain.thor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LidWakeGuardDecisionTest {
    @Test
    fun `reading closer to closed value is treated as closed`() {
        assertTrue(LidWakeGuardDecision.isReadingClosed(reading = 1.1f, closedValue = 1.0f, openValue = 5.0f))
    }

    @Test
    fun `reading closer to open value is treated as open`() {
        assertFalse(LidWakeGuardDecision.isReadingClosed(reading = 4.9f, closedValue = 1.0f, openValue = 5.0f))
    }

    @Test
    fun `reading exactly equidistant counts as closed`() {
        assertTrue(LidWakeGuardDecision.isReadingClosed(reading = 3.0f, closedValue = 1.0f, openValue = 5.0f))
    }

    @Test
    fun `shouldLock is true only when enabled, calibrated, and reading closed`() {
        assertTrue(LidWakeGuardDecision.shouldLock(guardEnabled = true, isCalibrated = true, sensorReadsClosed = true))
    }

    @Test
    fun `shouldLock is false when guard disabled`() {
        val result =
            LidWakeGuardDecision.shouldLock(
                guardEnabled = false,
                isCalibrated = true,
                sensorReadsClosed = true,
            )
        assertFalse(result)
    }

    @Test
    fun `shouldLock is false when not calibrated`() {
        val result =
            LidWakeGuardDecision.shouldLock(
                guardEnabled = true,
                isCalibrated = false,
                sensorReadsClosed = true,
            )
        assertFalse(result)
    }

    @Test
    fun `shouldLock is false when the reading is open`() {
        val result =
            LidWakeGuardDecision.shouldLock(
                guardEnabled = true,
                isCalibrated = true,
                sensorReadsClosed = false,
            )
        assertFalse(result)
    }
}

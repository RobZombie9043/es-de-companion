package com.esde.companion.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryTierTest {
    @Test
    fun `low tier below 20 percent`() {
        assertEquals(BatteryTier.Low, batteryTierFor(0))
        assertEquals(BatteryTier.Low, batteryTierFor(19))
    }

    @Test
    fun `medium tier from 20 to 49 percent`() {
        assertEquals(BatteryTier.Medium, batteryTierFor(20))
        assertEquals(BatteryTier.Medium, batteryTierFor(35))
        assertEquals(BatteryTier.Medium, batteryTierFor(49))
    }

    @Test
    fun `high tier from 50 to 84 percent`() {
        assertEquals(BatteryTier.High, batteryTierFor(50))
        assertEquals(BatteryTier.High, batteryTierFor(70))
        assertEquals(BatteryTier.High, batteryTierFor(84))
    }

    @Test
    fun `full tier from 85 percent`() {
        assertEquals(BatteryTier.Full, batteryTierFor(85))
        assertEquals(BatteryTier.Full, batteryTierFor(100))
    }
}

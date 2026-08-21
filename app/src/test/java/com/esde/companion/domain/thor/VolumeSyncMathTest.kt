package com.esde.companion.domain.thor

import org.junit.Assert.assertEquals
import org.junit.Test

class VolumeSyncMathTest {
    @Test
    fun `maps main index onto the fixed 0-15 secondary range by relative percentage`() {
        assertEquals(15, proportionalSecondaryVolumeTarget(mainCurrent = 15, mainMax = 15))
        assertEquals(0, proportionalSecondaryVolumeTarget(mainCurrent = 0, mainMax = 15))
        assertEquals(8, proportionalSecondaryVolumeTarget(mainCurrent = 8, mainMax = 15))
    }

    @Test
    fun `scales correctly when main's max isn't 15`() {
        // 12/25 of the way up should land at the same relative position on the 0-15 range.
        assertEquals(7, proportionalSecondaryVolumeTarget(mainCurrent = 12, mainMax = 25))
    }

    @Test
    fun `clamps to the valid range even with an out-of-bounds main index`() {
        assertEquals(15, proportionalSecondaryVolumeTarget(mainCurrent = 999, mainMax = 15))
        assertEquals(0, proportionalSecondaryVolumeTarget(mainCurrent = -5, mainMax = 15))
    }

    @Test
    fun `never divides by zero when main's max is reported as zero`() {
        assertEquals(0, proportionalSecondaryVolumeTarget(mainCurrent = 0, mainMax = 0))
    }
}

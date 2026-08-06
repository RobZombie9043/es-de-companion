package com.esde.companion.ui.widgets

import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PanZoomImageTest {

    private val size = IntSize(width = 200, height = 100)

    @Test
    fun `no translation at scale 1 regardless of direction`() {
        // Compared with a delta tolerance, not Pair equality: a negative direction at
        // scale 1 yields -0.0f, which is numerically equal to 0.0f but not equals()-equal
        // when boxed inside a Pair, so a plain assertEquals(0f to 0f, ...) would spuriously fail.
        val (tx1, ty1) = panZoomTranslation(scale = 1f, size = size, direction = PanZoomDirection(1, 1))
        assertEquals(0f, tx1, 0.001f)
        assertEquals(0f, ty1, 0.001f)

        val (tx2, ty2) = panZoomTranslation(scale = 1f, size = size, direction = PanZoomDirection(-1, -1))
        assertEquals(0f, tx2, 0.001f)
        assertEquals(0f, ty2, 0.001f)
    }

    @Test
    fun `translation matches half the overflow on each axis`() {
        // scale = 1.12f -> overflowX = 200 * 0.12 / 2 = 12, overflowY = 100 * 0.12 / 2 = 6
        val (tx, ty) = panZoomTranslation(scale = 1.12f, size = size, direction = PanZoomDirection(1, 1))
        assertEquals(12f, tx, 0.001f)
        assertEquals(6f, ty, 0.001f)
    }

    @Test
    fun `direction sign flips the translation sign per axis independently`() {
        val (tx, ty) = panZoomTranslation(scale = 1.12f, size = size, direction = PanZoomDirection(-1, 1))
        assertEquals(-12f, tx, 0.001f)
        assertEquals(6f, ty, 0.001f)
    }

    @Test
    fun `randomPanZoomDirection always returns exactly plus or minus one on each axis`() {
        repeat(50) {
            val direction = randomPanZoomDirection()
            assertTrue(direction.dirX == 1 || direction.dirX == -1)
            assertTrue(direction.dirY == 1 || direction.dirY == -1)
        }
    }
}

package com.esde.companion.ui.widgets

import androidx.compose.ui.unit.IntSize
import com.esde.companion.domain.model.NavigationDirection
import org.junit.Assert.assertEquals
import org.junit.Test

class AnimatedLogoImageTest {

    private val boxSize = IntSize(width = 200, height = 100)

    @Test
    fun `travelling left enters from the right - the opposite side`() {
        assertEquals(200f to 0f, slideStartOffset(NavigationDirection.Left, boxSize))
    }

    @Test
    fun `travelling right enters from the left - the opposite side`() {
        assertEquals(-200f to 0f, slideStartOffset(NavigationDirection.Right, boxSize))
    }

    @Test
    fun `travelling up enters from the bottom - the opposite side`() {
        assertEquals(0f to 100f, slideStartOffset(NavigationDirection.Up, boxSize))
    }

    @Test
    fun `travelling down enters from the top - the opposite side`() {
        assertEquals(0f to -100f, slideStartOffset(NavigationDirection.Down, boxSize))
    }

    @Test
    fun `unknown direction falls back to entering from the right`() {
        assertEquals(200f to 0f, slideStartOffset(null, boxSize))
    }
}

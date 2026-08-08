package com.esde.companion.ui.widgets

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import com.esde.companion.domain.model.NavigationDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimatedLogoImageTest {
    private val boxSize = IntSize(width = 200, height = 100)

    @Test
    fun `travelling left enters from the left - the same side`() {
        assertEquals(-200f to 0f, slideStartOffset(NavigationDirection.Left, boxSize))
    }

    @Test
    fun `travelling right enters from the right - the same side`() {
        assertEquals(200f to 0f, slideStartOffset(NavigationDirection.Right, boxSize))
    }

    @Test
    fun `travelling up enters from the top - the same side`() {
        assertEquals(0f to -100f, slideStartOffset(NavigationDirection.Up, boxSize))
    }

    @Test
    fun `travelling down enters from the bottom - the same side`() {
        assertEquals(0f to 100f, slideStartOffset(NavigationDirection.Down, boxSize))
    }

    // --- glintBandOffsets ------------------------------------------------------------------

    private val glintSize = Size(width = 200f, height = 100f)

    @Test
    fun `glint band is a fixed-width segment regardless of progress`() {
        // |end - start| must always equal GLINT_BAND_WIDTH_PX (300f) - this is what keeps
        // the visible streak's width decoupled from the widget's height, unlike the old
        // implementation which spanned the full height between anchor points.
        for (progress in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
            val (start, end) = glintBandOffsets(progress, glintSize)
            val length = kotlin.math.hypot((end.x - start.x).toDouble(), (end.y - start.y).toDouble())
            assertEquals(300.0, length, 0.01)
        }
    }

    @Test
    fun `glint band is centered at progress 0_5`() {
        val (start, end) = glintBandOffsets(progress = 0.5f, size = glintSize)
        assertEquals(-29.904f, start.x, 0.01f)
        assertEquals(-25f, start.y, 0.01f)
        assertEquals(229.904f, end.x, 0.01f)
        assertEquals(125f, end.y, 0.01f)
    }

    @Test
    fun `no corner of the widget falls inside the gradient's visible range at rest`() {
        // The actual regression this guards: between sweeps (progress 0, before the first
        // sweep, and progress 1, during the pause after one), every corner of the widget -
        // not just its edges at a single y - must project outside the gradient's [0, 1]
        // range, or a residual sliver of brightness leaks through (the bug this replaced).
        val corners =
            listOf(
                Offset(0f, 0f),
                Offset(glintSize.width, 0f),
                Offset(0f, glintSize.height),
                Offset(glintSize.width, glintSize.height),
            )
        for (progress in listOf(0f, 1f)) {
            val (start, end) = glintBandOffsets(progress, glintSize)
            for (corner in corners) {
                val t = projectionT(corner, start, end)
                assertTrue(
                    "corner $corner at progress $progress had t=$t, expected outside [0,1]",
                    t <= 0f || t >= 1f,
                )
            }
        }
    }

    /** Mirrors how Brush.linearGradient projects a point onto the [start]-[end] axis: 0f
     * at [start], 1f at [end], values outside [0,1] clamp to the boundary color (both of
     * which are Transparent here - see [glintBandOffsets]'s colorStops usage). */
    private fun projectionT(
        point: Offset,
        start: Offset,
        end: Offset,
    ): Float {
        val directionX = end.x - start.x
        val directionY = end.y - start.y
        val toPointX = point.x - start.x
        val toPointY = point.y - start.y
        val lengthSquared = directionX * directionX + directionY * directionY
        return (toPointX * directionX + toPointY * directionY) / lengthSquared
    }
}

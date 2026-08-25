package com.esde.companion.ui.widgets.edit

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import com.esde.companion.domain.model.StateGroup
import com.esde.companion.domain.model.WidgetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditWidgetsOverlayTest {
    // widgetCatalogFor

    @Test
    fun `widgetCatalogFor(System) excludes game-only widget types`() {
        val catalog = widgetCatalogFor(StateGroup.System)

        assertTrue(catalog.none { it is WidgetType.GameMedia })
        assertTrue(catalog.none { it is WidgetType.GameDescription })
        assertTrue(catalog.none { it is WidgetType.Rating })
        assertTrue(catalog.none { it is WidgetType.Video })
    }

    @Test
    fun `widgetCatalogFor(Playing) includes Video`() {
        val catalog = widgetCatalogFor(StateGroup.Playing)

        assertTrue(catalog.any { it is WidgetType.Video })
    }

    @Test
    fun `widgetCatalogFor(Playing) includes Rating`() {
        val catalog = widgetCatalogFor(StateGroup.Playing)

        assertTrue(catalog.any { it is WidgetType.Rating })
    }

    @Test
    fun `widgetCatalogFor(Playing) excludes system-only widget types`() {
        val catalog = widgetCatalogFor(StateGroup.Playing)

        assertTrue(catalog.none { it is WidgetType.SystemImage })
        assertTrue(catalog.none { it is WidgetType.SystemMedia })
    }

    @Test
    fun `widgetCatalogFor(System) and widgetCatalogFor(Playing) both include CustomImage and ColorBackground`() {
        val systemCatalog = widgetCatalogFor(StateGroup.System)
        val playingCatalog = widgetCatalogFor(StateGroup.Playing)

        assertTrue(systemCatalog.any { it is WidgetType.CustomImage })
        assertTrue(systemCatalog.any { it is WidgetType.ColorBackground })
        assertTrue(playingCatalog.any { it is WidgetType.CustomImage })
        assertTrue(playingCatalog.any { it is WidgetType.ColorBackground })
    }

    // toHexRgbString / parseHexColor

    @Test
    fun `toHexRgbString formats a color as uppercase 6-digit hex, dropping alpha`() {
        assertEquals("3A7BD5", 0xFF3A7BD5L.toHexRgbString())
    }

    @Test
    fun `toHexRgbString pads short values with leading zeros`() {
        assertEquals("0000FF", 0xFF0000FFL.toHexRgbString())
    }

    @Test
    fun `parseHexColor accepts a 6-digit hex string and forces full alpha`() {
        assertEquals(0xFF3A7BD5L, parseHexColor("3A7BD5"))
    }

    @Test
    fun `parseHexColor accepts a leading hash`() {
        assertEquals(0xFF3A7BD5L, parseHexColor("#3A7BD5"))
    }

    @Test
    fun `parseHexColor accepts lowercase hex digits`() {
        assertEquals(0xFF3A7BD5L, parseHexColor("3a7bd5"))
    }

    @Test
    fun `parseHexColor trims surrounding whitespace`() {
        assertEquals(0xFF3A7BD5L, parseHexColor("  3A7BD5  "))
    }

    @Test
    fun `parseHexColor rejects input shorter than 6 digits`() {
        assertNull(parseHexColor("12345"))
    }

    @Test
    fun `parseHexColor rejects input longer than 6 digits`() {
        assertNull(parseHexColor("1234567"))
    }

    @Test
    fun `parseHexColor rejects non-hex characters`() {
        assertNull(parseHexColor("gg0000"))
    }

    // dragMaxCell / nextDragCell

    private val cell100 = Offset(100f, 100f)

    @Test
    fun `dragMaxCell clamps a widget's own span within the grid`() {
        assertEquals(7, dragMaxCell(gridExtent = 10, span = 3))
    }

    @Test
    fun `dragMaxCell clamps to 0 when a saved span exceeds a smaller live grid`() {
        assertEquals(0, dragMaxCell(gridExtent = 5, span = 8))
    }

    @Test
    fun `sub-cell drag accumulates without moving`() {
        val step = nextDragCell(Offset(50f, 0f), Offset.Zero, cell100, IntOffset(0, 0), IntOffset(5, 5))

        assertTrue(!step.moved)
        assertEquals(0, step.cell.x)
        assertEquals(50f, step.accum.x)
    }

    @Test
    fun `crossing one full cell width moves one column and keeps the sub-cell remainder`() {
        val step = nextDragCell(Offset(20f, 0f), Offset(90f, 0f), cell100, IntOffset(0, 0), IntOffset(5, 5))

        assertTrue(step.moved)
        assertEquals(1, step.cell.x)
        assertEquals(10f, step.accum.x)
    }

    @Test
    fun `diagonal drag moves both column and row independently in one tick`() {
        val step = nextDragCell(Offset(250f, 150f), Offset.Zero, cell100, IntOffset(0, 0), IntOffset(5, 5))

        assertEquals(2, step.cell.x)
        assertEquals(1, step.cell.y)
    }

    @Test
    fun `negative drag amount moves column left`() {
        val step = nextDragCell(Offset(-150f, 0f), Offset.Zero, cell100, IntOffset(3, 0), IntOffset(5, 5))

        assertEquals(2, step.cell.x)
        assertEquals(-50f, step.accum.x)
    }

    @Test
    fun `hitBoundary is true on the tick a column transitions onto the max boundary`() {
        val step = nextDragCell(Offset(150f, 0f), Offset.Zero, cell100, IntOffset(4, 0), IntOffset(5, 5))

        assertEquals(5, step.cell.x)
        assertTrue(step.hitBoundary)
    }

    @Test
    fun `hitBoundary is false on a further tick while already pinned at the boundary`() {
        val step = nextDragCell(Offset(150f, 0f), Offset.Zero, cell100, IntOffset(5, 0), IntOffset(5, 5))

        assertEquals(5, step.cell.x)
        assertTrue(!step.hitBoundary)
    }

    @Test
    fun `a single-column grid never reports hitBoundary since it never transitions`() {
        val step = nextDragCell(Offset(150f, 0f), Offset.Zero, cell100, IntOffset(0, 0), IntOffset(0, 0))

        assertTrue(!step.hitBoundary)
    }

    // resizeMaxSpan / nextResizeSpan

    private fun farEdgeGeometry(initialFarEdgePx: Float) =
        ResizeGeometry(
            cellPx = 100f,
            gridExtentPx = 1000f,
            edgeSnapThresholdPx = 24f,
            initialStartPx = 200f,
            initialFarEdgePx = initialFarEdgePx,
        )

    private fun nearEdgeGeometry() =
        ResizeGeometry(
            cellPx = 100f,
            gridExtentPx = 1000f,
            edgeSnapThresholdPx = 24f,
            initialStartPx = 200f,
            initialFarEdgePx = 500f,
        )

    @Test
    fun `resizeMaxSpan for a far edge is the room remaining after start`() {
        assertEquals(8, resizeMaxSpan(gridExtentCells = 10, start = 2, span = 3, isFarEdge = true))
    }

    @Test
    fun `resizeMaxSpan for a far edge clamps to MIN_SPAN when start exceeds a smaller live grid`() {
        assertEquals(1, resizeMaxSpan(gridExtentCells = 5, start = 8, span = 3, isFarEdge = true))
    }

    @Test
    fun `resizeMaxSpan for a near edge is start plus span`() {
        assertEquals(5, resizeMaxSpan(gridExtentCells = 10, start = 2, span = 3, isFarEdge = false))
    }

    @Test
    fun `far edge sub-cell delta accumulates without changing span`() {
        val step =
            nextResizeSpan(
                true,
                30f,
                ResizeAccumulator(accum = 0f, totalDrag = 0f),
                ResizeBounds(start = 2, span = 3, maxSpan = 8),
                farEdgeGeometry(initialFarEdgePx = 500f),
            )

        assertTrue(!step.changed)
        assertEquals(3, step.span)
        assertEquals(30f, step.accumulator.accum)
    }

    @Test
    fun `far edge crossing one cell grows span by one`() {
        val step =
            nextResizeSpan(
                true,
                20f,
                ResizeAccumulator(accum = 90f, totalDrag = 90f),
                ResizeBounds(start = 2, span = 3, maxSpan = 8),
                farEdgeGeometry(initialFarEdgePx = 500f),
            )

        assertTrue(step.changed)
        assertEquals(4, step.span)
        assertEquals(10f, step.accumulator.accum)
    }

    @Test
    fun `far edge crossing multiple cells at once grows span by that many`() {
        val step =
            nextResizeSpan(
                true,
                250f,
                ResizeAccumulator(accum = 0f, totalDrag = 0f),
                ResizeBounds(start = 2, span = 3, maxSpan = 8),
                farEdgeGeometry(initialFarEdgePx = 500f),
            )

        assertEquals(5, step.span)
        assertEquals(50f, step.accumulator.accum)
    }

    @Test
    fun `far edge shrinking is clamped at MIN_SPAN even with a large negative delta`() {
        val step =
            nextResizeSpan(
                true,
                -1000f,
                ResizeAccumulator(accum = 0f, totalDrag = 0f),
                ResizeBounds(start = 2, span = 3, maxSpan = 8),
                farEdgeGeometry(initialFarEdgePx = 500f),
            )

        assertEquals(1, step.span)
    }

    @Test
    fun `far edge nearing the grid's boundary snaps span straight to maxSpan`() {
        val step =
            nextResizeSpan(
                true,
                10f,
                ResizeAccumulator(accum = 0f, totalDrag = 470f),
                ResizeBounds(start = 2, span = 3, maxSpan = 8),
                farEdgeGeometry(initialFarEdgePx = 500f),
            )

        assertEquals(8, step.span)
        assertTrue(step.snapped)
    }

    @Test
    fun `far edge already at maxSpan near the boundary does not re-snap or report changed`() {
        val step =
            nextResizeSpan(
                true,
                10f,
                ResizeAccumulator(accum = 0f, totalDrag = 470f),
                ResizeBounds(start = 2, span = 8, maxSpan = 8),
                farEdgeGeometry(initialFarEdgePx = 500f),
            )

        assertTrue(!step.changed)
        assertTrue(!step.snapped)
    }

    @Test
    fun `near edge sub-cell delta accumulates without changing start`() {
        val step =
            nextResizeSpan(
                false,
                15f,
                ResizeAccumulator(accum = 0f, totalDrag = 0f),
                ResizeBounds(start = 2, span = 3, maxSpan = 5),
                nearEdgeGeometry(),
            )

        assertTrue(!step.changed)
        assertEquals(2, step.start)
    }

    @Test
    fun `near edge crossing one cell inward moves start and shrinks span to compensate`() {
        val step =
            nextResizeSpan(
                false,
                20f,
                ResizeAccumulator(accum = 90f, totalDrag = 90f),
                ResizeBounds(start = 2, span = 3, maxSpan = 5),
                nearEdgeGeometry(),
            )

        assertEquals(3, step.start)
        assertEquals(2, step.span)
        assertEquals(10f, step.accumulator.accum)
    }

    @Test
    fun `near edge start is clamped so span never drops below MIN_SPAN`() {
        val step =
            nextResizeSpan(
                false,
                1000f,
                ResizeAccumulator(accum = 0f, totalDrag = 0f),
                ResizeBounds(start = 2, span = 3, maxSpan = 5),
                nearEdgeGeometry(),
            )

        assertEquals(4, step.start)
        assertEquals(1, step.span)
    }

    @Test
    fun `near edge nearing the grid's boundary snaps start to 0 and span to maxSpan`() {
        val step =
            nextResizeSpan(
                false,
                -10f,
                ResizeAccumulator(accum = 0f, totalDrag = -170f),
                ResizeBounds(start = 2, span = 3, maxSpan = 5),
                nearEdgeGeometry(),
            )

        assertEquals(0, step.start)
        assertEquals(5, step.span)
        assertTrue(step.snapped)
    }

    @Test
    fun `near edge already at start 0 near the boundary does not re-snap or report changed`() {
        val step =
            nextResizeSpan(
                false,
                -10f,
                ResizeAccumulator(accum = 0f, totalDrag = -170f),
                ResizeBounds(start = 0, span = 5, maxSpan = 5),
                nearEdgeGeometry(),
            )

        assertTrue(!step.changed)
        assertTrue(!step.snapped)
    }
}

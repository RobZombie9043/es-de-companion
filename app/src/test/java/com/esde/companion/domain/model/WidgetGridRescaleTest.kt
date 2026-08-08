package com.esde.companion.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetGridRescaleTest {
    private fun widget(
        gridColumn: Int,
        gridRow: Int,
        columnSpan: Int,
        rowSpan: Int,
    ) = PlacedWidget(
        id = "widget",
        widgetType = WidgetType.ColorBackground(colorArgb = 0xFF000000, alpha = 1f),
        gridColumn = gridColumn,
        gridRow = gridRow,
        columnSpan = columnSpan,
        rowSpan = rowSpan,
        zIndex = 0,
    )

    @Test
    fun `identical grid dimensions return the same widgets unchanged`() {
        val widgets = listOf(widget(gridColumn = 3, gridRow = 4, columnSpan = 5, rowSpan = 6))
        val grid = GridDimensions(columns = 10, rows = 10)

        val result = rescaleWidgetsToGrid(widgets, from = grid, to = grid)

        assertEquals(widgets, result)
    }

    @Test
    fun `widening the grid scales column placement and span proportionally`() {
        // A widget occupying the right half of a 10-column grid (columns 5-9) should
        // occupy the right half of a 20-column grid (columns 10-19) after rescaling.
        val widgets = listOf(widget(gridColumn = 5, gridRow = 0, columnSpan = 5, rowSpan = 10))

        val result =
            rescaleWidgetsToGrid(
                widgets,
                from = GridDimensions(columns = 10, rows = 10),
                to = GridDimensions(columns = 20, rows = 10),
            )

        val rescaled = result.single()
        assertEquals(10, rescaled.gridColumn)
        assertEquals(10, rescaled.columnSpan)
        assertEquals(0, rescaled.gridRow)
        assertEquals(10, rescaled.rowSpan)
    }

    @Test
    fun `narrowing the grid never leaves a widget hanging past the new edge`() {
        val widgets = listOf(widget(gridColumn = 8, gridRow = 0, columnSpan = 2, rowSpan = 1))

        val result =
            rescaleWidgetsToGrid(
                widgets,
                from = GridDimensions(columns = 10, rows = 1),
                to = GridDimensions(columns = 5, rows = 1),
            )

        val rescaled = result.single()
        assertEquals(true, rescaled.gridColumn + rescaled.columnSpan <= 5)
    }

    @Test
    fun `span never shrinks below one cell even on a drastic downscale`() {
        val widgets = listOf(widget(gridColumn = 0, gridRow = 0, columnSpan = 1, rowSpan = 1))

        val result =
            rescaleWidgetsToGrid(
                widgets,
                from = GridDimensions(columns = 100, rows = 100),
                to = GridDimensions(columns = 2, rows = 2),
            )

        val rescaled = result.single()
        assertEquals(1, rescaled.columnSpan)
        assertEquals(1, rescaled.rowSpan)
    }

    @Test
    fun `full-bleed background widgets stay full-bleed after rescaling`() {
        val widgets = listOf(widget(gridColumn = 0, gridRow = 0, columnSpan = 10, rowSpan = 10))

        val result =
            rescaleWidgetsToGrid(
                widgets,
                from = GridDimensions(columns = 10, rows = 10),
                to = GridDimensions(columns = 16, rows = 9),
            )

        val rescaled = result.single()
        assertEquals(0, rescaled.gridColumn)
        assertEquals(16, rescaled.columnSpan)
        assertEquals(0, rescaled.gridRow)
        assertEquals(9, rescaled.rowSpan)
    }
}

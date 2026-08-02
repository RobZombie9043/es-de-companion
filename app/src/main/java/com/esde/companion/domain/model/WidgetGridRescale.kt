package com.esde.companion.domain.model

import kotlin.math.roundToInt

/**
 * Remaps [widgets]' grid-cell placements from one grid size to another, preserving each
 * widget's proportional position/size rather than its raw cell coordinates. The widget
 * grid's column/row count is derived from the screen's actual dp size (see WidgetGrid.kt),
 * so running the app on a different-resolution display (e.g. moving from a 1240x1080 to a
 * 1920x1080 screen) yields a differently-sized grid entirely - without this, a canvas
 * authored on the narrower grid stays at its original absolute cell coordinates, which now
 * only cover the left portion of the wider grid, leaving blank space beyond them.
 *
 * Returns [widgets] unchanged when [from] == [to] (the common case - same screen every
 * launch), so nothing here introduces rounding drift on every load.
 */
fun rescaleWidgetsToGrid(widgets: List<PlacedWidget>, from: GridDimensions, to: GridDimensions): List<PlacedWidget> {
    if (from == to) return widgets

    val columnScale = to.columns.toFloat() / from.columns
    val rowScale = to.rows.toFloat() / from.rows

    return widgets.map { widget ->
        val columnSpan = (widget.columnSpan * columnScale).roundToInt().coerceIn(1, to.columns)
        val rowSpan = (widget.rowSpan * rowScale).roundToInt().coerceIn(1, to.rows)
        val gridColumn = (widget.gridColumn * columnScale).roundToInt().coerceIn(0, to.columns - columnSpan)
        val gridRow = (widget.gridRow * rowScale).roundToInt().coerceIn(0, to.rows - rowSpan)
        widget.copy(gridColumn = gridColumn, gridRow = gridRow, columnSpan = columnSpan, rowSpan = rowSpan)
    }
}

package com.esde.companion.ui.widgets.edit

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset

/** The furthest column/row a widget with [span] cells can be dragged to within a grid
 * spanning [gridExtent] cells. coerceAtLeast(0) keeps callers' coerceIn from throwing on
 * an empty range when a saved widget's span exceeds a live-measured smaller grid -
 * pinning to the origin instead of crashing. */
internal fun dragMaxCell(
    gridExtent: Int,
    span: Int,
): Int = (gridExtent - span).coerceAtLeast(0)

/** The result of feeding one drag tick's delta into [nextDragCell]. */
internal data class DragStep(
    val cell: IntOffset,
    val accum: Offset,
    val moved: Boolean,
    val hitBoundary: Boolean,
)

/** Grid-snapped drag-to-move math for a single [detectDragGestures] tick: accumulates
 * sub-cell drag distance in [accum] until it crosses a full cell (sized [cellSizePx]),
 * then advances [currentCell] by that many cells (clamped to [maxCell]), carrying the
 * leftover sub-cell remainder forward rather than discarding it. [DragStep.hitBoundary]
 * is true only on the tick a coordinate transitions onto a boundary, not on every
 * subsequent tick spent pinned against it. */
internal fun nextDragCell(
    dragAmount: Offset,
    accum: Offset,
    cellSizePx: Offset,
    currentCell: IntOffset,
    maxCell: IntOffset,
): DragStep {
    val newAccum = accum + dragAmount
    val columnDelta = (newAccum.x / cellSizePx.x).toInt()
    val rowDelta = (newAccum.y / cellSizePx.y).toInt()

    if (columnDelta == 0 && rowDelta == 0) {
        return DragStep(currentCell, newAccum, moved = false, hitBoundary = false)
    }

    val newColumn = (currentCell.x + columnDelta).coerceIn(0, maxCell.x)
    val newRow = (currentCell.y + rowDelta).coerceIn(0, maxCell.y)
    val hitBoundary =
        (newColumn != currentCell.x && (newColumn == 0 || newColumn == maxCell.x)) ||
            (newRow != currentCell.y && (newRow == 0 || newRow == maxCell.y))
    return DragStep(
        cell = IntOffset(newColumn, newRow),
        accum = Offset(newAccum.x - columnDelta * cellSizePx.x, newAccum.y - rowDelta * cellSizePx.y),
        moved = true,
        hitBoundary = hitBoundary,
    )
}

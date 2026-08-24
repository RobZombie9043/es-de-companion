package com.esde.companion.ui.widgets.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.esde.companion.domain.model.GridDimensions
import com.esde.companion.domain.model.PlacedWidget

/** A widget can never be resized smaller than one grid cell in either dimension. */
private const val MIN_SPAN = 1

/** Touch target size for a resize handle - offsets/positioning throughout EditWidgetsOverlay
 * are all in terms of this, so the grab zone stays full-size regardless of [HANDLE_DOT_SIZE]. */
internal val HANDLE_SIZE = 32.dp

/** Visual size of the dot drawn inside a resize handle's touch target - smaller than
 * [HANDLE_SIZE] so the dot reads as a compact grab indicator without shrinking the
 * actual (harder-to-hit-precisely) touch target to match. */
private val HANDLE_DOT_SIZE = 24.dp

/** Once the resize handle's actual on-screen position gets this close to the grid's true
 * edge, snap straight to the grid boundary rather than requiring further delta-based
 * drag distance - see ResizeHandle's kdoc for why this is needed at all. */
private val EDGE_SNAP_THRESHOLD = 24.dp

/** The largest span a resize handle on [isFarEdge] can grow a widget to, given the
 * anchored [start] cell, current [span], and a grid spanning [gridExtentCells] cells.
 * coerceAtLeast(MIN_SPAN) keeps callers' coerceIn from throwing on an empty range when a
 * saved widget's position leaves less room than MIN_SPAN on a live-measured smaller grid. */
internal fun resizeMaxSpan(
    gridExtentCells: Int,
    start: Int,
    span: Int,
    isFarEdge: Boolean,
): Int =
    if (isFarEdge) {
        (gridExtentCells - start).coerceAtLeast(MIN_SPAN)
    } else {
        (start + span).coerceAtLeast(MIN_SPAN)
    }

/** A resize handle's position/size state: the anchored [start] cell, current [span], and
 * [maxSpan] it can grow to (see [resizeMaxSpan]). */
internal data class ResizeBounds(
    val start: Int,
    val span: Int,
    val maxSpan: Int,
)

/** The on-screen pixel geometry a resize handle needs to detect edge proximity: the size
 * of one grid cell, the grid's total extent, how close counts as "near" ([edgeSnapThresholdPx]),
 * and the gesture's starting pixel position for the near/far edge respectively. */
internal data class ResizeGeometry(
    val cellPx: Float,
    val gridExtentPx: Float,
    val edgeSnapThresholdPx: Float,
    val initialStartPx: Float,
    val initialFarEdgePx: Float,
)

/** A resize gesture's running sub-cell drag remainder ([accum], reset on each whole-cell
 * step) and total on-screen displacement since the gesture began ([totalDrag], never
 * reset - see [nextResizeSpan]). */
internal data class ResizeAccumulator(
    val accum: Float,
    val totalDrag: Float,
)

/** The result of feeding one drag tick's delta into [nextResizeSpan]. [changed] is true
 * whenever [start]/[span] moved, whether by a whole-cell step or an edge snap;
 * [snapped] narrows that to just the edge-snap case, which fires its own haptic. */
internal data class ResizeStep(
    val start: Int,
    val span: Int,
    val accumulator: ResizeAccumulator,
    val changed: Boolean,
    val snapped: Boolean,
)

/** Grid-snapped drag-to-resize math for a single [detectDragGestures] tick, covering both
 * a far-edge handle (Right/Bottom: [ResizeBounds.start] fixed, span grows/shrinks away
 * from it) and a near-edge handle (Left/Top: span's far edge fixed, start moves and span
 * compensates). Sub-cell [delta] accumulates until it crosses a full cell, same mechanism
 * as [nextDragCell]. [ResizeAccumulator.totalDrag] is the gesture's total on-screen
 * displacement - once it puts the moving edge within [ResizeGeometry.edgeSnapThresholdPx]
 * of the grid boundary, this snaps straight to the limit rather than requiring the exact
 * remaining per-cell delta to accumulate, since delta-based dragging alone can never reach
 * a physical screen edge (no more room for the finger to keep moving). */
internal fun nextResizeSpan(
    isFarEdge: Boolean,
    delta: Float,
    accumulator: ResizeAccumulator,
    bounds: ResizeBounds,
    geometry: ResizeGeometry,
): ResizeStep {
    val newAccumulator = ResizeAccumulator(accumulator.accum + delta, accumulator.totalDrag + delta)
    val resolved =
        if (isFarEdge) {
            resolveFarEdgeResize(bounds, geometry, newAccumulator)
        } else {
            resolveNearEdgeResize(bounds, geometry, newAccumulator)
        }
    val resolvedAccumulator = newAccumulator.copy(accum = resolved.accum)
    return ResizeStep(resolved.start, resolved.span, resolvedAccumulator, resolved.changed, resolved.snapped)
}

private data class ResolvedResize(
    val start: Int,
    val span: Int,
    val accum: Float,
    val changed: Boolean,
    val snapped: Boolean,
)

/** Right/Bottom: the far edge moves with the drag, [ResizeBounds.start] is fixed. */
private fun resolveFarEdgeResize(
    bounds: ResizeBounds,
    geometry: ResizeGeometry,
    accumulator: ResizeAccumulator,
): ResolvedResize {
    val farEdgePx = geometry.initialFarEdgePx + accumulator.totalDrag
    val nearGridBoundary = geometry.gridExtentPx - farEdgePx <= geometry.edgeSnapThresholdPx
    val cellDelta = if (nearGridBoundary) 0 else (accumulator.accum / geometry.cellPx).toInt()
    return when {
        nearGridBoundary && bounds.span != bounds.maxSpan ->
            ResolvedResize(bounds.start, bounds.maxSpan, accumulator.accum, changed = true, snapped = true)
        cellDelta != 0 ->
            ResolvedResize(
                bounds.start,
                (bounds.span + cellDelta).coerceIn(MIN_SPAN, bounds.maxSpan),
                accumulator.accum - cellDelta * geometry.cellPx,
                changed = true,
                snapped = false,
            )
        else -> ResolvedResize(bounds.start, bounds.span, accumulator.accum, changed = false, snapped = false)
    }
}

/** Left/Top: the near edge (start) moves with the drag, the far edge is fixed - start and
 * span must update together. */
private fun resolveNearEdgeResize(
    bounds: ResizeBounds,
    geometry: ResizeGeometry,
    accumulator: ResizeAccumulator,
): ResolvedResize {
    val startPx = geometry.initialStartPx + accumulator.totalDrag
    val nearGridBoundary = startPx <= geometry.edgeSnapThresholdPx
    val cellDelta = if (nearGridBoundary) 0 else (accumulator.accum / geometry.cellPx).toInt()
    return when {
        nearGridBoundary && bounds.start != 0 ->
            ResolvedResize(0, bounds.maxSpan, accumulator.accum, changed = true, snapped = true)
        cellDelta != 0 -> {
            val newStart = (bounds.start + cellDelta).coerceIn(0, bounds.maxSpan - MIN_SPAN)
            ResolvedResize(
                newStart,
                bounds.maxSpan - newStart,
                accumulator.accum - cellDelta * geometry.cellPx,
                changed = true,
                snapped = false,
            )
        }
        else -> ResolvedResize(bounds.start, bounds.span, accumulator.accum, changed = false, snapped = false)
    }
}

/** Which side of the selected widget a [ResizeHandle] sits on and drags along. Left/Right
 * affect the column axis, Top/Bottom the row axis - each handle only ever touches one
 * axis, unlike the single bottom-right corner handle this replaced, which resized both
 * at once. */
internal enum class ResizeEdge { Left, Right, Top, Bottom }

/**
 * Grid-snapped drag-to-resize for one edge of the selected widget - four render around a
 * selected widget (Left, Right, Top, Bottom). Right/Bottom keep the widget's own
 * gridColumn/gridRow fixed and only grow/shrink columnSpan/rowSpan, same as the original
 * corner handle. Left/Top instead keep the *opposite* edge fixed - dragging them moves
 * gridColumn/gridRow while columnSpan/rowSpan shrinks or grows to match, so the anchored
 * far edge never visibly jumps. Each handle only ever touches one axis (Left/Right:
 * columns; Top/Bottom: rows) - [onResize] always reports the full new bounds regardless,
 * with the untouched axis read fresh from [currentWidget] so it reflects whatever the
 * other three handles (or a move drag) most recently applied.
 *
 * Delta-based drag alone (grow/shrink once the finger has moved a full cell-width) cannot
 * reach the grid's true edge: right at the physical screen boundary there's no more room
 * left for the finger to keep moving, so the required delta for the last cell(s) can
 * never accumulate. [totalDrag] (tracked separately from the per-cell accumulator, never
 * reset within a gesture) gives the handle's actual on-screen position, so once that gets
 * within [EDGE_SNAP_THRESHOLD] of the grid boundary, this snaps straight to the limit
 * instead - reaching the edge only requires getting near it, not traveling the exact
 * remaining distance.
 *
 * Same stable-key + rememberUpdatedState + compute-in-onDragStart pattern as
 * PlaceholderWidgetBox - see its kdoc for why. Without this, resizing the same widget a
 * second time without deselecting in between would use whichever position/size was true
 * when the handle first appeared, not the result of the first resize.
 *
 * Deliberately a sibling of PlaceholderWidgetBox rather than nested inside it - see
 * EditWidgetsOverlay's kdoc for why.
 */
@Composable
internal fun ResizeHandle(
    edge: ResizeEdge,
    widget: PlacedWidget,
    grid: GridDimensions,
    cellWidth: Dp,
    cellHeight: Dp,
    onResize: (widgetId: String, gridColumn: Int, gridRow: Int, columnSpan: Int, rowSpan: Int) -> Unit,
    onDragStateChanged: (Boolean) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val currentWidget by rememberUpdatedState(widget)
    val isColumnAxis = edge == ResizeEdge.Left || edge == ResizeEdge.Right
    // Right/Bottom: the anchor is this widget's own near/start edge (gridColumn/gridRow),
    // and this handle is on the far edge, growing span away from it. Left/Top: the anchor
    // is the opposite (far) edge instead, so this handle - on the near/start edge - moves
    // gridColumn/gridRow itself as span adjusts to compensate.
    val isFarEdge = edge == ResizeEdge.Right || edge == ResizeEdge.Bottom

    Box(
        modifier =
            modifier
                .size(HANDLE_SIZE) // touch target - see HANDLE_SIZE's kdoc for why this stays full-size
                .pointerInput(widget.id, edge, cellWidth, cellHeight, grid) {
                    val cellWidthPx = with(density) { cellWidth.toPx() }
                    val cellHeightPx = with(density) { cellHeight.toPx() }
                    val cellPx = if (isColumnAxis) cellWidthPx else cellHeightPx
                    val gridExtentCells = if (isColumnAxis) grid.columns else grid.rows
                    val edgeSnapThresholdPx = with(density) { EDGE_SNAP_THRESHOLD.toPx() }

                    var bounds = ResizeBounds(start = 0, span = 0, maxSpan = 0)
                    var geometry = ResizeGeometry(cellPx, cellPx * gridExtentCells, edgeSnapThresholdPx, 0f, 0f)
                    var accumulator = ResizeAccumulator(accum = 0f, totalDrag = 0f)

                    detectDragGestures(
                        onDragStart = {
                            val liveWidget = currentWidget
                            val start = if (isColumnAxis) liveWidget.gridColumn else liveWidget.gridRow
                            val span = if (isColumnAxis) liveWidget.columnSpan else liveWidget.rowSpan
                            // maxSpan means different things per anchor: for a far-edge
                            // (Right/Bottom) handle it's the largest span can grow to; for
                            // a near-edge (Left/Top) handle it's the anchored far edge's
                            // fixed cell position, since span there is derived as maxSpan - start.
                            val maxSpan = resizeMaxSpan(gridExtentCells, start, span, isFarEdge)
                            bounds = ResizeBounds(start, span, maxSpan)
                            geometry =
                                geometry.copy(
                                    initialStartPx = start * cellPx,
                                    initialFarEdgePx = (start + span) * cellPx,
                                )
                            accumulator = ResizeAccumulator(accum = 0f, totalDrag = 0f)
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDragStateChanged(true)
                        },
                        onDragEnd = {
                            onDragStateChanged(false)
                            onDragEnd()
                        },
                        onDragCancel = {
                            onDragStateChanged(false)
                            onDragEnd()
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        val delta = if (isColumnAxis) dragAmount.x else dragAmount.y

                        val step = nextResizeSpan(isFarEdge, delta, accumulator, bounds, geometry)
                        accumulator = step.accumulator

                        if (step.changed) {
                            bounds = bounds.copy(start = step.start, span = step.span)
                            if (step.snapped) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            val liveWidget = currentWidget
                            onResize(
                                widget.id,
                                if (isColumnAxis) bounds.start else liveWidget.gridColumn,
                                if (isColumnAxis) liveWidget.gridRow else bounds.start,
                                if (isColumnAxis) bounds.span else liveWidget.columnSpan,
                                if (isColumnAxis) liveWidget.rowSpan else bounds.span,
                            )
                        }
                    }
                },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(HANDLE_DOT_SIZE)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
        )
    }
}

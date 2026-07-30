package com.esde.companion.ui.widgets.edit

import android.graphics.Rect
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.esde.companion.domain.model.GridDimensions
import com.esde.companion.domain.model.PlacedWidget
import com.esde.companion.domain.model.StateGroup
import com.esde.companion.domain.model.WidgetType
import com.esde.companion.ui.widgets.gridDimensionsFor

/** Options button opacity at rest - translucent so it's unobtrusive but still visible. */
private const val OPTIONS_BUTTON_IDLE_ALPHA = 0.75f

/** Options button opacity while a drag is in progress - fades further so it never
 * distracts from placement/sizing happening elsewhere on screen. */
private const val OPTIONS_BUTTON_DRAGGING_ALPHA = 0.15f

/** A widget can never be resized smaller than one grid cell in either dimension. */
private const val MIN_SPAN = 1

private val HANDLE_SIZE = 32.dp

/** Once the resize handle's actual on-screen position gets this close to the grid's true
 * edge, snap straight to the grid boundary rather than requiring further delta-based
 * drag distance - see ResizeHandle's kdoc for why this is needed at all. */
private val EDGE_SNAP_THRESHOLD = 24.dp

/**
 * Full-screen edit-mode overlay - not a nav destination, same pattern as the App Drawer
 * and Settings. Opaque background, so whatever's happening on the live canvas underneath
 * (WidgetOverlay, still composed and reacting to AppState) is simply never visible while
 * editing - no separate state-freezing needed for that to hold true.
 *
 * The widget grid is measured at full screen size and has no persistent chrome floating
 * over it - a small options button (top-right) opens a menu with the canvas switcher and
 * Done, rather than a full-width header. Earlier this used a persistent floating header,
 * which meant a widget resized/moved to sit entirely behind it became untappable (visible
 * through translucency, but touch couldn't reach it). A small button occupying a fixed
 * corner removes that problem architecturally rather than needing to clamp widget
 * placement around it.
 *
 * Covers entry/exit, the canvas switcher, drag-to-move, and tap-to-select + drag-to-resize.
 * Add, configure, and remove are later slices on top of this one.
 */
@Composable
fun EditWidgetsOverlay(
    viewModel: EditWidgetsViewModel,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        // System gesture-navigation edge zones (back-swipe areas along the bottom/sides)
        // intercept touch before Compose sees it, which made dragging a widget/handle
        // toward the screen edge lose tracking right at the boundary. Excluding the
        // whole surface from gesture navigation for as long as this overlay is visible
        // fixes that; cleared in onDispose so it doesn't affect anything once back out
        // of edit mode (including normal back-gesture behavior elsewhere in the app).
        val view = LocalView.current
        DisposableEffect(Unit) {
            view.systemGestureExclusionRects = listOf(Rect(0, 0, view.width, view.height))
            onDispose { view.systemGestureExclusionRects = emptyList() }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            val widgets by viewModel.widgets.collectAsStateWithLifecycle()
            val isDragging by viewModel.isDragging.collectAsStateWithLifecycle()
            val selectedWidgetId by viewModel.selectedWidgetId.collectAsStateWithLifecycle()
            val selectedCanvas by viewModel.selectedCanvas.collectAsStateWithLifecycle()

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    // Background tap deselects. Only fires when the touch didn't land on
                    // a widget - a widget's own tap-select consumes the event first (see
                    // PlaceholderWidgetBox), so this correctly no-ops for taps on widgets
                    // rather than deselecting in the same gesture that selected.
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { viewModel.selectWidget(null) })
                    },
            ) {
                val grid = remember(maxWidth, maxHeight) { gridDimensionsFor(maxWidth, maxHeight) }
                LaunchedEffect(grid) { viewModel.setGridDimensions(grid) }

                val cellWidth = maxWidth / grid.columns
                val cellHeight = maxHeight / grid.rows

                if (widgets.isEmpty()) {
                    Text(text = "No widgets yet - tap + to add one", modifier = Modifier.padding(24.dp))
                }

                for (widget in widgets) {
                    PlaceholderWidgetBox(
                        widget = widget,
                        isSelected = widget.id == selectedWidgetId,
                        grid = grid,
                        cellWidth = cellWidth,
                        cellHeight = cellHeight,
                        onSelect = { viewModel.selectWidget(widget.id) },
                        onMove = viewModel::updateWidgetPosition,
                        onDragStateChanged = viewModel::setDragging,
                        onDragEnd = viewModel::persistWidgets,
                        modifier = Modifier
                            .offset(x = cellWidth * widget.gridColumn, y = cellHeight * widget.gridRow)
                            .size(width = cellWidth * widget.columnSpan, height = cellHeight * widget.rowSpan),
                    )
                }

                // Rendered as a sibling of the widget boxes above, not nested inside the
                // selected widget's own Box - a nested handle would have its drag gesture
                // and the widget's own move-drag both react to the same touch. As a
                // sibling positioned over just the corner, standard hit-testing routes
                // each touch to exactly one of them.
                widgets.firstOrNull { it.id == selectedWidgetId }?.let { selectedWidget ->
                    ResizeHandle(
                        widget = selectedWidget,
                        grid = grid,
                        cellWidth = cellWidth,
                        cellHeight = cellHeight,
                        onResize = viewModel::updateWidgetSize,
                        onDragStateChanged = viewModel::setDragging,
                        onDragEnd = viewModel::persistWidgets,
                        modifier = Modifier.offset(
                            x = cellWidth * (selectedWidget.gridColumn + selectedWidget.columnSpan) - HANDLE_SIZE / 2,
                            y = cellHeight * (selectedWidget.gridRow + selectedWidget.rowSpan) - HANDLE_SIZE / 2,
                        ),
                    )
                }
            }

            val optionsButtonAlpha by animateFloatAsState(
                targetValue = if (isDragging) OPTIONS_BUTTON_DRAGGING_ALPHA else OPTIONS_BUTTON_IDLE_ALPHA,
                animationSpec = tween(durationMillis = 150),
                label = "editWidgetsOptionsButtonAlpha",
            )

            var menuExpanded by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .alpha(optionsButtonAlpha),
            ) {
                FloatingActionButton(onClick = { menuExpanded = true }) {
                    Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "Widget options")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    StateGroup.entries.forEach { group ->
                        DropdownMenuItem(
                            text = { Text(group.name) },
                            leadingIcon = if (group == selectedCanvas) {
                                { Icon(imageVector = Icons.Filled.Check, contentDescription = null) }
                            } else {
                                null
                            },
                            onClick = {
                                viewModel.selectCanvas(group)
                                menuExpanded = false
                            },
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Done") },
                        onClick = {
                            viewModel.selectWidget(null)
                            menuExpanded = false
                            onDone()
                        },
                    )
                }
            }
        }
    }
}

/**
 * Grid-snapped drag-to-move, plus tap-to-select (shows the resize handle, see
 * ResizeHandle). Tracks its own cumulative logical position and sub-cell drag remainder
 * as local vars scoped to the gesture coroutine, independent of the [widget] prop's
 * value at any given recomposition - same "local state drives the gesture, external
 * state drives the visuals" split MainScreen's drawer drag uses.
 *
 * The pointerInput key is deliberately narrow (id, cellWidth, cellHeight only) - it must
 * NOT include position/size, since those change on every cell crossing during the very
 * drag this block is running, and a pointerInput key change cancels and restarts its
 * coroutine, which would end the gesture mid-drag (requiring a new finger-down to
 * resume) rather than tracking continuously. [currentWidget] (via rememberUpdatedState)
 * is how this still avoids stale position/size data despite that stable key - it always
 * reflects the latest [widget] value, read fresh in onDragStart for each new gesture,
 * without needing the coroutine itself to restart.
 *
 * Tap-to-select and drag-to-move are two separate pointerInput blocks on the same node -
 * they coexist the same way MainScreen's long-press and drawer-drag do: a stationary
 * touch never triggers the drag detector's touch-slop consumption, so the tap detector
 * fires cleanly; a real drag does consume, which correctly cancels tap recognition
 * instead of both firing for the same gesture.
 */
@Composable
private fun PlaceholderWidgetBox(
    widget: PlacedWidget,
    isSelected: Boolean,
    grid: GridDimensions,
    cellWidth: Dp,
    cellHeight: Dp,
    onSelect: () -> Unit,
    onMove: (widgetId: String, gridColumn: Int, gridRow: Int) -> Unit,
    onDragStateChanged: (Boolean) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val currentWidget by rememberUpdatedState(widget)

    Box(
        modifier = modifier
            .padding(2.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                } else {
                    Modifier
                },
            )
            .pointerInput(widget.id) {
                detectTapGestures(onTap = { onSelect() })
            }
            .pointerInput(widget.id, cellWidth, cellHeight) {
                val cellWidthPx = with(density) { cellWidth.toPx() }
                val cellHeightPx = with(density) { cellHeight.toPx() }
                var currentColumn = 0
                var currentRow = 0
                var maxColumn = 0
                var maxRow = 0
                var accumX = 0f
                var accumY = 0f

                detectDragGestures(
                    onDragStart = {
                        val liveWidget = currentWidget
                        currentColumn = liveWidget.gridColumn
                        currentRow = liveWidget.gridRow
                        maxColumn = grid.columns - liveWidget.columnSpan
                        maxRow = grid.rows - liveWidget.rowSpan
                        accumX = 0f
                        accumY = 0f
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
                    accumX += dragAmount.x
                    accumY += dragAmount.y

                    val columnDelta = (accumX / cellWidthPx).toInt()
                    val rowDelta = (accumY / cellHeightPx).toInt()

                    if (columnDelta != 0 || rowDelta != 0) {
                        currentColumn = (currentColumn + columnDelta).coerceIn(0, maxColumn)
                        currentRow = (currentRow + rowDelta).coerceIn(0, maxRow)
                        accumX -= columnDelta * cellWidthPx
                        accumY -= rowDelta * cellHeightPx
                        onMove(widget.id, currentColumn, currentRow)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = widget.widgetType.label(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Grid-snapped drag-to-resize, anchored at the widget's fixed top-left (gridColumn/
 * gridRow never change here) - only columnSpan/rowSpan grow or shrink, clamped to
 * [MIN_SPAN, grid.columns/rows - the widget's fixed anchor position].
 *
 * Delta-based drag alone (grow the span once the finger has moved a full cell-width)
 * cannot reach the grid's true edge: right at the physical screen boundary there's no
 * more room left for the finger to keep moving, so the required delta for the last
 * cell(s) can never accumulate. [totalDragX]/[totalDragY] (tracked separately from the
 * per-cell accumulator, never reset within a gesture) give the handle's actual on-screen
 * position, so once that gets within [EDGE_SNAP_THRESHOLD] of the grid boundary, this
 * snaps straight to the max span instead - reaching the edge only requires getting near
 * it, not traveling the exact remaining distance.
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
private fun ResizeHandle(
    widget: PlacedWidget,
    grid: GridDimensions,
    cellWidth: Dp,
    cellHeight: Dp,
    onResize: (widgetId: String, columnSpan: Int, rowSpan: Int) -> Unit,
    onDragStateChanged: (Boolean) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val currentWidget by rememberUpdatedState(widget)

    Box(
        modifier = modifier
            .size(HANDLE_SIZE)
            .background(MaterialTheme.colorScheme.primary, CircleShape)
            .pointerInput(widget.id, cellWidth, cellHeight, grid) {
                val cellWidthPx = with(density) { cellWidth.toPx() }
                val cellHeightPx = with(density) { cellHeight.toPx() }
                val gridWidthPx = cellWidthPx * grid.columns
                val gridHeightPx = cellHeightPx * grid.rows
                val edgeSnapThresholdPx = with(density) { EDGE_SNAP_THRESHOLD.toPx() }

                var maxColumnSpan = 0
                var maxRowSpan = 0
                var startCornerXPx = 0f
                var startCornerYPx = 0f
                var currentColumnSpan = 0
                var currentRowSpan = 0
                var accumX = 0f
                var accumY = 0f
                var totalDragX = 0f
                var totalDragY = 0f

                detectDragGestures(
                    onDragStart = {
                        val liveWidget = currentWidget
                        maxColumnSpan = grid.columns - liveWidget.gridColumn
                        maxRowSpan = grid.rows - liveWidget.gridRow
                        startCornerXPx = (liveWidget.gridColumn + liveWidget.columnSpan) * cellWidthPx
                        startCornerYPx = (liveWidget.gridRow + liveWidget.rowSpan) * cellHeightPx
                        currentColumnSpan = liveWidget.columnSpan
                        currentRowSpan = liveWidget.rowSpan
                        accumX = 0f
                        accumY = 0f
                        totalDragX = 0f
                        totalDragY = 0f
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
                    accumX += dragAmount.x
                    accumY += dragAmount.y
                    totalDragX += dragAmount.x
                    totalDragY += dragAmount.y

                    val cornerXPx = startCornerXPx + totalDragX
                    val cornerYPx = startCornerYPx + totalDragY
                    val nearRightEdge = gridWidthPx - cornerXPx <= edgeSnapThresholdPx
                    val nearBottomEdge = gridHeightPx - cornerYPx <= edgeSnapThresholdPx

                    var changed = false

                    if (nearRightEdge && currentColumnSpan != maxColumnSpan) {
                        currentColumnSpan = maxColumnSpan
                        changed = true
                    } else if (!nearRightEdge) {
                        val columnDelta = (accumX / cellWidthPx).toInt()
                        if (columnDelta != 0) {
                            currentColumnSpan = (currentColumnSpan + columnDelta).coerceIn(MIN_SPAN, maxColumnSpan)
                            accumX -= columnDelta * cellWidthPx
                            changed = true
                        }
                    }

                    if (nearBottomEdge && currentRowSpan != maxRowSpan) {
                        currentRowSpan = maxRowSpan
                        changed = true
                    } else if (!nearBottomEdge) {
                        val rowDelta = (accumY / cellHeightPx).toInt()
                        if (rowDelta != 0) {
                            currentRowSpan = (currentRowSpan + rowDelta).coerceIn(MIN_SPAN, maxRowSpan)
                            accumY -= rowDelta * cellHeightPx
                            changed = true
                        }
                    }

                    if (changed) {
                        onResize(widget.id, currentColumnSpan, currentRowSpan)
                    }
                }
            },
    )
}

private fun WidgetType.label(): String = when (this) {
    is WidgetType.SystemLogo -> "System Logo"
    is WidgetType.SystemMedia -> "System: ${mediaType.name}"
    is WidgetType.GameMedia -> "Game: ${mediaType.name}"
    is WidgetType.ColorBackground -> "Color Background"
}
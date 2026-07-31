package com.esde.companion.ui.widgets.edit

import android.graphics.Rect
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlipToFront
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.esde.companion.domain.model.GridDimensions
import com.esde.companion.domain.model.ImageEffects
import com.esde.companion.domain.model.MediaType
import com.esde.companion.domain.model.PlacedWidget
import com.esde.companion.domain.model.ScaleMode
import com.esde.companion.domain.model.StateGroup
import com.esde.companion.domain.model.WidgetContent
import com.esde.companion.domain.model.WidgetType
import com.esde.companion.ui.widgets.WidgetContentView
import com.esde.companion.ui.widgets.gridDimensionsFor
import kotlin.math.roundToInt

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

private val MENU_SHAPE = RoundedCornerShape(16.dp)

/**
 * The widget types available to add, filtered to what actually makes sense per canvas -
 * System has no per-game media to show, Playing has no system-level media. Each entry
 * carries sensible default config (scale mode, starting color/alpha) - these are exactly
 * what gets placed on add; per-widget reconfiguration is a later slice.
 */
private fun widgetCatalogFor(stateGroup: StateGroup): List<WidgetType> = when (stateGroup) {
    StateGroup.System -> listOf(
        WidgetType.SystemLogo(ScaleMode.Fit),
        WidgetType.SystemMedia(MediaType.FanArt, ScaleMode.Fill),
        WidgetType.SystemMedia(MediaType.Screenshots, ScaleMode.Fill),
        WidgetType.ColorBackground(colorArgb = 0xFF000000, alpha = 0.5f),
    )

    StateGroup.Playing -> listOf(
        WidgetType.GameMedia(MediaType.Marquees, ScaleMode.Fit),
        WidgetType.GameMedia(MediaType.Covers, ScaleMode.Fit),
        WidgetType.GameMedia(MediaType.ThreeDBoxes, ScaleMode.Fit),
        WidgetType.GameMedia(MediaType.MixImages, ScaleMode.Fill),
        WidgetType.GameMedia(MediaType.Screenshots, ScaleMode.Fill),
        WidgetType.GameMedia(MediaType.FanArt, ScaleMode.Fill),
        WidgetType.GameMedia(MediaType.TitleScreens, ScaleMode.Fit),
        WidgetType.GameMedia(MediaType.BackCovers, ScaleMode.Fit),
        WidgetType.GameMedia(MediaType.PhysicalMedia, ScaleMode.Fit),
        WidgetType.ColorBackground(colorArgb = 0xFF000000, alpha = 0.5f),
    )
}

private fun StateGroup.displayLabel(): String = when (this) {
    StateGroup.System -> "System View"
    StateGroup.Playing -> "Game View"
}

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
            val previewContent by viewModel.previewContent.collectAsStateWithLifecycle()
            val isDragging by viewModel.isDragging.collectAsStateWithLifecycle()
            val selectedWidgetId by viewModel.selectedWidgetId.collectAsStateWithLifecycle()
            val selectedCanvas by viewModel.selectedCanvas.collectAsStateWithLifecycle()
            var showAddPicker by remember { mutableStateOf(false) }

            // Without this, back falls through to the system default and exits the app -
            // MainScreen's own BackHandler (which normally intercepts back so this kiosk
            // app can never be exited) isn't composed while this overlay is showing,
            // since MainActivity only renders one of MainScreen/SettingsScreen/this at a
            // time. Step-back priority: close the add-widget dialog first if it's open,
            // then deselect a selected widget, and only exit edit mode (same as Done)
            // once neither applies - undoing the most local thing first, same as back
            // behaves elsewhere in the app, rather than a single press jumping straight
            // out of a mid-edit state.
            BackHandler(enabled = true) {
                when {
                    showAddPicker -> showAddPicker = false
                    selectedWidgetId != null -> viewModel.selectWidget(null)
                    else -> {
                        viewModel.selectWidget(null)
                        onDone()
                    }
                }
            }

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
                val gridLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)

                if (widgets.isEmpty()) {
                    Text(text = "No widgets yet - open options to add one", modifier = Modifier.padding(24.dp))
                }

                for (widget in widgets) {
                    PlaceholderWidgetBox(
                        widget = widget,
                        content = previewContent[widget.id] ?: WidgetContent.Empty,
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
                            .size(width = cellWidth * widget.columnSpan, height = cellHeight * widget.rowSpan)
                            .zIndex(widget.zIndex.toFloat()),
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
                        modifier = Modifier
                            .offset(
                                x = cellWidth * (selectedWidget.gridColumn + selectedWidget.columnSpan) - HANDLE_SIZE / 2,
                                y = cellHeight * (selectedWidget.gridRow + selectedWidget.rowSpan) - HANDLE_SIZE / 2,
                            )
                            .zIndex(Float.MAX_VALUE - 1), // always above widgets, below grid-line overlay
                    )
                }

                // Faint grid lines - purely visual/draw-only (no pointerInput), given an
                // explicit zIndex far above any realistic widget zIndex so it always
                // draws on top regardless of a widget's own stacking order - composition
                // order alone isn't enough once individual zIndex values are in play (see
                // the .zIndex() on each PlaceholderWidgetBox above), so without this a
                // widget moved above the grid lines' zIndex would cover them.
                Canvas(modifier = Modifier.fillMaxSize().zIndex(Float.MAX_VALUE)) {
                    val cellWidthPx = cellWidth.toPx()
                    val cellHeightPx = cellHeight.toPx()

                    for (column in 1 until grid.columns) {
                        val x = cellWidthPx * column
                        drawLine(color = gridLineColor, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 2f)
                    }
                    for (row in 1 until grid.rows) {
                        val y = cellHeightPx * row
                        drawLine(color = gridLineColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 2f)
                    }
                }
            }

            val optionsButtonAlpha by animateFloatAsState(
                targetValue = if (isDragging) OPTIONS_BUTTON_DRAGGING_ALPHA else OPTIONS_BUTTON_IDLE_ALPHA,
                animationSpec = tween(durationMillis = 150),
                label = "editWidgetsOptionsButtonAlpha",
            )

            var menuExpanded by remember { mutableStateOf(false) }
            var showConfigureDialog by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .alpha(optionsButtonAlpha),
            ) {
                FloatingActionButton(onClick = { menuExpanded = true }) {
                    Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "Widget options")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    shape = MENU_SHAPE,
                ) {
                    DropdownMenuItem(
                        text = { Text("Add Widget") },
                        leadingIcon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            showAddPicker = true
                        },
                    )
                    if (selectedWidgetId != null) {
                        DropdownMenuItem(
                            text = { Text("Configure Widget") },
                            leadingIcon = { Icon(imageVector = Icons.Filled.Settings, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                showConfigureDialog = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Move Forwards") },
                            leadingIcon = { Icon(imageVector = Icons.Filled.FlipToFront, contentDescription = null) },
                            onClick = {
                                viewModel.moveUp(selectedWidgetId!!)
                                menuExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Move Backwards") },
                            leadingIcon = { Icon(imageVector = Icons.Filled.FlipToBack, contentDescription = null) },
                            onClick = {
                                viewModel.moveDown(selectedWidgetId!!)
                                menuExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Remove Widget") },
                            leadingIcon = { Icon(imageVector = Icons.Filled.Delete, contentDescription = null) },
                            onClick = {
                                viewModel.removeWidget(selectedWidgetId!!)
                                menuExpanded = false
                            },
                        )
                    }
                    HorizontalDivider()
                    StateGroup.entries.forEach { group ->
                        DropdownMenuItem(
                            text = { Text(group.displayLabel()) },
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

            if (showAddPicker) {
                AddWidgetDialog(
                    catalog = widgetCatalogFor(selectedCanvas),
                    onPick = { widgetType ->
                        viewModel.addWidget(widgetType)
                        showAddPicker = false
                    },
                    onDismiss = { showAddPicker = false },
                )
            }

            if (showConfigureDialog) {
                widgets.firstOrNull { it.id == selectedWidgetId }?.let { selectedWidget ->
                    ConfigureWidgetDialog(
                        widgetType = selectedWidget.widgetType,
                        onChange = { updated -> viewModel.updateWidgetConfig(selectedWidget.id, updated) },
                        onDismiss = { showConfigureDialog = false },
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
    content: WidgetContent,
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
    val isPlaceholder = content == WidgetContent.Empty

    Box(
        modifier = modifier
            .padding(2.dp)
            .then(
                if (isPlaceholder) {
                    Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                } else {
                    Modifier
                },
            )
            .then(
                Modifier.border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    },
                    shape = RoundedCornerShape(8.dp),
                ),
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
        if (isPlaceholder) {
            Text(
                text = widget.widgetType.label(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            WidgetContentView(content = content, modifier = Modifier.fillMaxSize())
        }
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

/**
 * Simple list picker for widgetCatalogFor(selectedCanvas) - one dialog serves both
 * canvases, since the catalog itself is already filtered per-canvas by the caller.
 */
@Composable
private fun AddWidgetDialog(
    catalog: List<WidgetType>,
    onPick: (WidgetType) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Widget") },
        text = {
            LazyColumn {
                items(catalog) { widgetType ->
                    Text(
                        text = widgetType.label(),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(widgetType) }
                            .padding(vertical = 12.dp),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

/**
 * Content shown depends entirely on [widgetType]'s variant: image-backed types
 * (SystemLogo, SystemMedia, GameMedia) get a scale-mode choice; ColorBackground gets a
 * preset color swatch row plus an alpha slider. Neither variant needs a Confirm step -
 * [onChange] fires immediately on each selection/slider move (see
 * EditWidgetsViewModel.updateWidgetConfig's kdoc for why that's fine here), so the
 * dialog only needs a way to close, not a way to commit.
 */
@Composable
private fun ConfigureWidgetDialog(
    widgetType: WidgetType,
    onChange: (WidgetType) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Widget") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                when (widgetType) {
                    is WidgetType.SystemLogo -> {
                        ScaleModeConfig(current = widgetType.scaleMode) { onChange(widgetType.copy(scaleMode = it)) }
                        ImageEffectsConfig(current = widgetType.effects) { onChange(widgetType.copy(effects = it)) }
                    }

                    is WidgetType.SystemMedia -> {
                        ScaleModeConfig(current = widgetType.scaleMode) { onChange(widgetType.copy(scaleMode = it)) }
                        ImageEffectsConfig(current = widgetType.effects) { onChange(widgetType.copy(effects = it)) }
                    }

                    is WidgetType.GameMedia -> {
                        ScaleModeConfig(current = widgetType.scaleMode) { onChange(widgetType.copy(scaleMode = it)) }
                        ImageEffectsConfig(current = widgetType.effects) { onChange(widgetType.copy(effects = it)) }
                    }

                    is WidgetType.ColorBackground ->
                        ColorBackgroundConfig(current = widgetType, onChange = onChange)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
    )
}

private fun ScaleMode.displayLabel(): String = when (this) {
    ScaleMode.Fit -> "Contain"
    ScaleMode.Fill -> "Cover"
}

private fun ScaleMode.icon(): ImageVector = when (this) {
    ScaleMode.Fit -> Icons.Filled.CropFree   // whole image visible, letterboxed
    ScaleMode.Fill -> Icons.Filled.Crop      // cropped to fill the frame
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScaleModeConfig(current: ScaleMode, onSelect: (ScaleMode) -> Unit) {
    Column {
        Text(text = "Image Scaling", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ScaleMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = mode == current,
                    onClick = { onSelect(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = ScaleMode.entries.size),
                    icon = {
                        SegmentedButtonDefaults.Icon(active = mode == current) {
                            Icon(
                                imageVector = mode.icon(),
                                contentDescription = null,
                                modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
                            )
                        }
                    },
                    label = { Text(mode.displayLabel()) },
                )
            }
        }
    }
}

/**
 * Blur + darken controls for image-backed widget types (SystemLogo, SystemMedia,
 * GameMedia). Both scale as simple 0f..1f sliders - see ImageEffects' kdoc for why, and
 * applyBlurEffect/DarkenOverlay in WidgetCanvas.kt for how these map to rendering.
 * Darken is a flat black overlay rather than a tint color - its main use is muting a
 * busy background image so a logo/widget placed on top reads more clearly.
 */
@Composable
private fun ImageEffectsConfig(current: ImageEffects, onChange: (ImageEffects) -> Unit) {
    Column {
        Text(text = "Blur: ${(current.blurAmount * 100).roundToInt()}%", style = MaterialTheme.typography.titleSmall)
        Slider(
            value = current.blurAmount,
            onValueChange = { onChange(current.copy(blurAmount = it)) },
            valueRange = 0f..1f,
        )
    }
    Spacer(modifier = Modifier.height(16.dp))
    Column {
        Text(text = "Darken: ${(current.darkenAmount * 100).roundToInt()}%", style = MaterialTheme.typography.titleSmall)
        Slider(
            value = current.darkenAmount,
            onValueChange = { onChange(current.copy(darkenAmount = it)) },
            valueRange = 0f..1f,
        )
    }
}

/** Fixed preset palette - a full custom color picker (hue/saturation wheel) is more
 * than this widget type needs for a first pass; these presets cover the common cases
 * (tint/darken behind other widgets) with a much simpler UI. */
private val COLOR_PRESETS = listOf(
    0xFF000000L, // black
    0xFFFFFFFFL, // white
    0xFF9E9E9EL, // gray
    0xFFF44336L, // red
    0xFF2196F3L, // blue
    0xFF4CAF50L, // green
    0xFFFFEB3BL, // yellow
    0xFF9C27B0L, // purple
)

@Composable
private fun ColorBackgroundConfig(
    current: WidgetType.ColorBackground,
    onChange: (WidgetType.ColorBackground) -> Unit,
) {
    Column {
        Text(text = "Color", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            COLOR_PRESETS.forEach { colorArgb ->
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(36.dp)
                        .background(Color(colorArgb), CircleShape)
                        .then(
                            if (colorArgb == current.colorArgb) {
                                Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            } else {
                                Modifier
                            },
                        )
                        .clickable { onChange(current.copy(colorArgb = colorArgb)) },
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        HexColorInput(current = current.colorArgb) { onChange(current.copy(colorArgb = it)) }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Transparency: ${(current.alpha * 100).roundToInt()}%", style = MaterialTheme.typography.titleSmall)
        Slider(
            value = current.alpha,
            onValueChange = { onChange(current.copy(alpha = it)) },
            valueRange = 0f..1f,
        )
    }
}

/**
 * Free-form hex entry alongside the preset swatches. [current]'s own alpha channel is
 * always opaque (0xFF...) - the presets are stored that way and transparency is handled
 * separately by ColorBackground.alpha - so this only ever reads/writes the RGB portion.
 *
 * Local [text] is keyed on [current] rather than plain `remember { }`: this means
 * picking a preset (or another external change) reformats the field to match, while
 * typing an in-progress/invalid hex string is left alone across recompositions since
 * [current] doesn't change until a full valid 6-digit value is entered. onValidHex only
 * fires once the text parses cleanly, so partial input never produces a garbage color.
 */
@Composable
private fun HexColorInput(current: Long, onValidHex: (Long) -> Unit) {
    var text by remember(current) { mutableStateOf(current.toHexRgbString()) }
    val isValid = parseHexColor(text) != null

    OutlinedTextField(
        value = text,
        onValueChange = { newText ->
            text = newText
            parseHexColor(newText)?.let(onValidHex)
        },
        label = { Text("Hex code") },
        leadingIcon = { Text(text = "#", style = MaterialTheme.typography.bodyLarge) },
        isError = !isValid,
        supportingText = { if (!isValid) Text("Enter a 6-digit hex code, e.g. 3A7BD5") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun Long.toHexRgbString(): String = String.format("%06X", this and 0xFFFFFF)

private fun parseHexColor(text: String): Long? {
    val cleaned = text.removePrefix("#").trim()
    if (cleaned.length != 6 || cleaned.any { it !in HEX_CHARS }) return null
    return cleaned.toLongOrNull(16)?.let { 0xFF000000L or it }
}

private val HEX_CHARS = "0123456789abcdefABCDEF".toSet()
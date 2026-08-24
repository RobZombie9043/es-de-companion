package com.esde.companion.ui.widgets.edit

import android.graphics.Rect
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.FlipToFront
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.esde.companion.data.storage.SafPathResolver
import com.esde.companion.domain.model.GridDimensions
import com.esde.companion.domain.model.MediaType
import com.esde.companion.domain.model.PlacedWidget
import com.esde.companion.domain.model.ScaleMode
import com.esde.companion.domain.model.StateGroup
import com.esde.companion.domain.model.WidgetContent
import com.esde.companion.domain.model.WidgetType
import com.esde.companion.ui.CORNER_BUTTON_EDGE_PADDING
import com.esde.companion.ui.CornerFab
import com.esde.companion.ui.dock.DockPreview
import com.esde.companion.ui.widgets.WidgetContentDisplayOptions
import com.esde.companion.ui.widgets.WidgetContentView
import com.esde.companion.ui.widgets.gridDimensionsFor

/** Options button opacity at rest - translucent so it's unobtrusive but still visible. */
private const val OPTIONS_BUTTON_IDLE_ALPHA = 0.75f

/** Options button opacity while a drag is in progress - fades further so it never
 * distracts from placement/sizing happening elsewhere on screen. */
private const val OPTIONS_BUTTON_DRAGGING_ALPHA = 0.15f

/** How far PlaceholderWidgetBox insets its visible border from the widget's true grid
 * bounds (its own `.padding(2.dp)`) - resize handle offsets subtract this so a handle
 * centers on the border as it's actually drawn, not on the untouched cell boundary 2dp
 * further out. Kept as one named constant so the two can't silently drift apart. */
private val WIDGET_BORDER_INSET = 2.dp

private val MENU_SHAPE = RoundedCornerShape(16.dp)

/**
 * The widget types available to add, filtered to what actually makes sense per canvas -
 * System has no per-game media to show, Playing has no system-level media. Each entry
 * carries sensible default config (scale mode, starting color/alpha) - these are exactly
 * what gets placed on add; per-widget reconfiguration is a later slice.
 */
internal fun widgetCatalogFor(stateGroup: StateGroup): List<WidgetType> =
    when (stateGroup) {
        StateGroup.System ->
            listOf(
                WidgetType.SystemLogo(ScaleMode.Fit),
                WidgetType.SystemImage(ScaleMode.Fill),
                WidgetType.SystemMedia(MediaType.FanArt, ScaleMode.Fill),
                WidgetType.SystemMedia(MediaType.Screenshots, ScaleMode.Fill),
                WidgetType.CustomImage(path = "", scaleMode = ScaleMode.Fill),
                WidgetType.ColorBackground(colorArgb = 0xFF000000, alpha = 0.5f),
            )

        StateGroup.Playing ->
            listOf(
                WidgetType.GameMedia(MediaType.Marquees, ScaleMode.Fit),
                WidgetType.GameDescription(),
                WidgetType.Rating(),
                WidgetType.GameMedia(MediaType.Covers, ScaleMode.Fit),
                WidgetType.GameMedia(MediaType.ThreeDBoxes, ScaleMode.Fit),
                WidgetType.GameMedia(MediaType.MixImages, ScaleMode.Fill),
                WidgetType.GameMedia(MediaType.Screenshots, ScaleMode.Fill),
                WidgetType.GameMedia(MediaType.FanArt, ScaleMode.Fill),
                WidgetType.GameMedia(MediaType.TitleScreens, ScaleMode.Fit),
                WidgetType.GameMedia(MediaType.BackCovers, ScaleMode.Fit),
                WidgetType.GameMedia(MediaType.PhysicalMedia, ScaleMode.Fit),
                WidgetType.CustomImage(path = "", scaleMode = ScaleMode.Fill),
                WidgetType.ColorBackground(colorArgb = 0xFF000000, alpha = 0.5f),
                WidgetType.Video(),
            )
    }

private fun StateGroup.displayLabel(): String =
    when (this) {
        StateGroup.System -> "System Canvas"
        StateGroup.Playing -> "Game Canvas"
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
    initialCanvas: StateGroup,
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

        val hapticFeedback = LocalHapticFeedback.current

        Box(modifier = Modifier.fillMaxSize()) {
            val widgets by viewModel.widgets.collectAsStateWithLifecycle()
            val previewContent by viewModel.previewContent.collectAsStateWithLifecycle()
            val isDragging by viewModel.isDragging.collectAsStateWithLifecycle()
            val selectedWidgetId by viewModel.selectedWidgetId.collectAsStateWithLifecycle()
            val selectedCanvas by viewModel.selectedCanvas.collectAsStateWithLifecycle()
            val dockEnabled by viewModel.dockEnabled.collectAsStateWithLifecycle()
            val dockSize by viewModel.dockSize.collectAsStateWithLifecycle()
            val dockOpacityPercent by viewModel.dockOpacityPercent.collectAsStateWithLifecycle()
            val dockMaxApps by viewModel.dockMaxApps.collectAsStateWithLifecycle()
            val dockItems by viewModel.dockItems.collectAsStateWithLifecycle()
            var showAddPicker by remember { mutableStateOf(false) }

            // Picking "Custom Image" from AddWidgetDialog launches straight into this
            // rather than adding a widget with an empty path first - every other catalog
            // entry is immediately fully resolvable, so this keeps that same "add ->
            // instantly see something real" experience instead of adding a placeholder
            // that still needs a follow-up Configure Widget step. Cancelling the picker
            // (null result) adds nothing.
            val addCustomImageLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                    uri?.let(SafPathResolver::resolveDocumentPath)?.let { path ->
                        viewModel.addWidget(WidgetType.CustomImage(path = path, scaleMode = ScaleMode.Fill))
                    }
                }

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
                modifier =
                    Modifier
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

                // Switch to whichever canvas edit mode was entered for, before the first
                // load - selectCanvas() no-ops if it's already the current canvas (the
                // common case, since it defaults to System), and this runs before
                // setGridDimensions' own reload, so there's no flash of the wrong
                // canvas's widgets on entry.
                LaunchedEffect(grid) {
                    viewModel.selectCanvas(initialCanvas)
                    viewModel.setGridDimensions(grid)
                }

                val cellWidth = maxWidth / grid.columns
                val cellHeight = maxHeight / grid.rows
                val gridLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)

                AnimatedVisibility(visible = widgets.isEmpty(), enter = fadeIn(), exit = fadeOut()) {
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
                        modifier =
                            Modifier
                                .offset(x = cellWidth * widget.gridColumn, y = cellHeight * widget.gridRow)
                                .size(width = cellWidth * widget.columnSpan, height = cellHeight * widget.rowSpan)
                                .zIndex(widget.zIndex.toFloat()),
                    )
                }

                // Rendered as siblings of the widget boxes above, not nested inside the
                // selected widget's own Box - a nested handle would have its drag gesture
                // and the widget's own move-drag both react to the same touch. As
                // siblings positioned over just their own edge midpoint, standard
                // hit-testing routes each touch to exactly one of them.
                widgets.firstOrNull { it.id == selectedWidgetId }?.let { selectedWidget ->
                    val verticalCenter = cellHeight * (selectedWidget.gridRow + selectedWidget.rowSpan / 2f)
                    val horizontalCenter = cellWidth * (selectedWidget.gridColumn + selectedWidget.columnSpan / 2f)

                    ResizeHandle(
                        edge = ResizeEdge.Right,
                        widget = selectedWidget,
                        grid = grid,
                        cellWidth = cellWidth,
                        cellHeight = cellHeight,
                        onResize = viewModel::updateWidgetBounds,
                        onDragStateChanged = viewModel::setDragging,
                        onDragEnd = viewModel::persistWidgets,
                        modifier =
                            Modifier
                                .offset(
                                    x =
                                        cellWidth * (selectedWidget.gridColumn + selectedWidget.columnSpan) -
                                            WIDGET_BORDER_INSET - HANDLE_SIZE / 2,
                                    y = verticalCenter - HANDLE_SIZE / 2,
                                )
                                .zIndex(Float.MAX_VALUE - 1),
                        // always above widgets, below grid-line overlay
                    )
                    ResizeHandle(
                        edge = ResizeEdge.Left,
                        widget = selectedWidget,
                        grid = grid,
                        cellWidth = cellWidth,
                        cellHeight = cellHeight,
                        onResize = viewModel::updateWidgetBounds,
                        onDragStateChanged = viewModel::setDragging,
                        onDragEnd = viewModel::persistWidgets,
                        modifier =
                            Modifier
                                .offset(
                                    x = cellWidth * selectedWidget.gridColumn + WIDGET_BORDER_INSET - HANDLE_SIZE / 2,
                                    y = verticalCenter - HANDLE_SIZE / 2,
                                )
                                .zIndex(Float.MAX_VALUE - 1),
                    )
                    ResizeHandle(
                        edge = ResizeEdge.Bottom,
                        widget = selectedWidget,
                        grid = grid,
                        cellWidth = cellWidth,
                        cellHeight = cellHeight,
                        onResize = viewModel::updateWidgetBounds,
                        onDragStateChanged = viewModel::setDragging,
                        onDragEnd = viewModel::persistWidgets,
                        modifier =
                            Modifier
                                .offset(
                                    x = horizontalCenter - HANDLE_SIZE / 2,
                                    y =
                                        cellHeight * (selectedWidget.gridRow + selectedWidget.rowSpan) -
                                            WIDGET_BORDER_INSET - HANDLE_SIZE / 2,
                                )
                                .zIndex(Float.MAX_VALUE - 1),
                    )
                    ResizeHandle(
                        edge = ResizeEdge.Top,
                        widget = selectedWidget,
                        grid = grid,
                        cellWidth = cellWidth,
                        cellHeight = cellHeight,
                        onResize = viewModel::updateWidgetBounds,
                        onDragStateChanged = viewModel::setDragging,
                        onDragEnd = viewModel::persistWidgets,
                        modifier =
                            Modifier
                                .offset(
                                    x = horizontalCenter - HANDLE_SIZE / 2,
                                    y = cellHeight * selectedWidget.gridRow + WIDGET_BORDER_INSET - HANDLE_SIZE / 2,
                                )
                                .zIndex(Float.MAX_VALUE - 1),
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
                        drawLine(
                            color = gridLineColor,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 2f,
                        )
                    }
                    for (row in 1 until grid.rows) {
                        val y = cellHeightPx * row
                        drawLine(
                            color = gridLineColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 2f,
                        )
                    }
                }
            }

            // Visual-only preview of the live App Dock (Settings > App Drawer and Dock),
            // so a widget can't be placed somewhere the dock will actually cover without
            // the person knowing until they exit edit mode. Composed after the
            // BoxWithConstraints above (grid/widgets/grid-lines), so it draws on top of
            // all of it purely by composition order - zIndex only reorders siblings
            // within the same parent, so the Float.MAX_VALUE grid-line Canvas inside that
            // Box has no bearing on stacking against this outer sibling. No clickable/
            // pointerInput modifier anywhere in DockPreview, so drags/taps for widget
            // placement pass straight through it, same "no gesture modifier" idiom as the
            // grid-line Canvas itself. BottomCenter reproduces the live dock's resting
            // position (see MainScreen's comment on the drawer's closed/rest state)
            // without needing edit mode to fake the drawer's own slide animation.
            if (dockEnabled) {
                DockPreview(
                    dockSize = dockSize,
                    dockOpacityPercent = dockOpacityPercent,
                    maxApps = dockMaxApps,
                    dockItems = dockItems,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }

            val optionsButtonAlpha by animateFloatAsState(
                targetValue = if (isDragging) OPTIONS_BUTTON_DRAGGING_ALPHA else OPTIONS_BUTTON_IDLE_ALPHA,
                animationSpec = tween(durationMillis = 150),
                label = "editWidgetsOptionsButtonAlpha",
            )

            var menuExpanded by remember { mutableStateOf(false) }
            var showConfigureDialog by remember { mutableStateOf(false) }

            // Same edge padding as MainScreen's Settings FAB and MainActivity's music FAB
            // (CornerButtonMetrics) - opposite corner, but keeps all three corner buttons
            // at the same offset from their respective edges by construction.
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(CORNER_BUTTON_EDGE_PADDING)
                        .alpha(optionsButtonAlpha),
            ) {
                CornerFab(onClick = { menuExpanded = true }, opacityPercent = dockOpacityPercent) {
                    Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "Widget options")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    shape = MENU_SHAPE,
                ) {
                    StateGroup.entries.forEach { group ->
                        DropdownMenuItem(
                            text = { Text(group.displayLabel()) },
                            leadingIcon =
                                if (group == selectedCanvas) {
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
                        text = { Text("Add Widget") },
                        leadingIcon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            showAddPicker = true
                        },
                    )
                    val selectedId = selectedWidgetId
                    if (selectedId != null) {
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
                                viewModel.moveUp(selectedId)
                                menuExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Move Backwards") },
                            leadingIcon = { Icon(imageVector = Icons.Filled.FlipToBack, contentDescription = null) },
                            onClick = {
                                viewModel.moveDown(selectedId)
                                menuExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Remove Widget") },
                            leadingIcon = { Icon(imageVector = Icons.Filled.Delete, contentDescription = null) },
                            onClick = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.removeWidget(selectedId)
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
                        showAddPicker = false
                        if (widgetType is WidgetType.CustomImage) {
                            addCustomImageLauncher.launch(arrayOf("image/*"))
                        } else {
                            viewModel.addWidget(widgetType)
                        }
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
 *
 * The drag-to-move modifier is only attached while [isSelected] - an unselected widget
 * can still be tapped (that's how it becomes selected) but not dragged, so a touch that
 * merely brushes across an unrelated widget on its way to the one actually being
 * targeted can't accidentally reposition it. [isSelected] is safe to gate on directly
 * (rather than through rememberUpdatedState) because it can only change between
 * gestures, never mid-drag - selecting is its own separate tap, so by the time a drag
 * starts, whichever value applies has already been composed in.
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
    val hapticFeedback = LocalHapticFeedback.current
    val currentWidget by rememberUpdatedState(widget)
    val isPlaceholder = content == WidgetContent.Empty

    Box(
        modifier =
            modifier
                .padding(WIDGET_BORDER_INSET)
                .then(
                    if (isPlaceholder) {
                        Modifier.background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            RoundedCornerShape(8.dp),
                        )
                    } else {
                        Modifier
                    },
                )
                .then(
                    Modifier.border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color =
                            if (isSelected) {
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
                .then(
                    if (isSelected) {
                        Modifier.pointerInput(widget.id, cellWidth, cellHeight) {
                            val cellSizePx =
                                with(density) { Offset(cellWidth.toPx(), cellHeight.toPx()) }
                            var currentCell = IntOffset.Zero
                            var maxCell = IntOffset.Zero
                            var accum = Offset.Zero

                            detectDragGestures(
                                onDragStart = {
                                    val liveWidget = currentWidget
                                    currentCell = IntOffset(liveWidget.gridColumn, liveWidget.gridRow)
                                    maxCell =
                                        IntOffset(
                                            dragMaxCell(grid.columns, liveWidget.columnSpan),
                                            dragMaxCell(grid.rows, liveWidget.rowSpan),
                                        )
                                    accum = Offset.Zero
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
                                val step = nextDragCell(dragAmount, accum, cellSizePx, currentCell, maxCell)
                                accum = step.accum

                                if (step.moved) {
                                    currentCell = step.cell
                                    // Fires once at the moment the drag is clamped onto a grid
                                    // boundary (a real transition, not every tick spent pinned
                                    // against it) - same "hit the wall" feedback as the resize
                                    // handles' edge-snap below.
                                    if (step.hitBoundary) {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    onMove(widget.id, currentCell.x, currentCell.y)
                                }
                            }
                        }
                    } else {
                        Modifier
                    },
                ),
        contentAlignment = Alignment.Center,
    ) {
        if (isPlaceholder) {
            Text(
                text = widget.widgetType.label(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            WidgetContentView(
                content = content,
                widgetType = widget.widgetType,
                modifier = Modifier.fillMaxSize(),
                displayOptions = WidgetContentDisplayOptions(textUserScrollEnabled = false),
            )
        }
    }
}

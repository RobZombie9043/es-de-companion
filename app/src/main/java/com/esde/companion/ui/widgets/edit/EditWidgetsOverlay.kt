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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlipToFront
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.esde.companion.data.storage.SafPathResolver
import com.esde.companion.domain.model.GridDimensions
import com.esde.companion.domain.model.ImageEffects
import com.esde.companion.domain.model.ImageTransitionMode
import com.esde.companion.domain.model.LogoTransitionMode
import com.esde.companion.domain.model.MediaType
import com.esde.companion.domain.model.PlacedWidget
import com.esde.companion.domain.model.ScaleMode
import com.esde.companion.domain.model.StateGroup
import com.esde.companion.domain.model.WidgetContent
import com.esde.companion.domain.model.WidgetType
import com.esde.companion.domain.model.allowsFadeTransition
import com.esde.companion.domain.model.glintEnabled
import com.esde.companion.domain.model.imageTransitionMode
import com.esde.companion.domain.model.isLogoStyle
import com.esde.companion.domain.model.logoTransitionMode
import com.esde.companion.domain.model.supportsImageTransition
import com.esde.companion.domain.model.supportsPanZoom
import com.esde.companion.ui.CORNER_BUTTON_EDGE_PADDING
import com.esde.companion.ui.CornerFab
import com.esde.companion.ui.dock.DockPreview
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

/** Touch target size for a resize handle - offsets/positioning throughout this file are
 * all in terms of this, so the grab zone stays full-size regardless of [HANDLE_DOT_SIZE]. */
private val HANDLE_SIZE = 32.dp

/** Visual size of the dot drawn inside a resize handle's touch target - smaller than
 * [HANDLE_SIZE] so the dot reads as a compact grab indicator without shrinking the
 * actual (harder-to-hit-precisely) touch target to match. */
private val HANDLE_DOT_SIZE = 24.dp

/** How far PlaceholderWidgetBox insets its visible border from the widget's true grid
 * bounds (its own `.padding(2.dp)`) - resize handle offsets subtract this so a handle
 * centers on the border as it's actually drawn, not on the untouched cell boundary 2dp
 * further out. Kept as one named constant so the two can't silently drift apart. */
private val WIDGET_BORDER_INSET = 2.dp

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
        WidgetType.SystemImage(ScaleMode.Fill),
        WidgetType.SystemMedia(MediaType.FanArt, ScaleMode.Fill),
        WidgetType.SystemMedia(MediaType.Screenshots, ScaleMode.Fill),
        WidgetType.CustomImage(path = "", scaleMode = ScaleMode.Fill),
        WidgetType.ColorBackground(colorArgb = 0xFF000000, alpha = 0.5f),
    )

    StateGroup.Playing -> listOf(
        WidgetType.GameMedia(MediaType.Marquees, ScaleMode.Fit),
        WidgetType.GameDescription(),
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
    )
}

private fun StateGroup.displayLabel(): String = when (this) {
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
            val addCustomImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
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
                        modifier = Modifier
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
                        modifier = Modifier
                            .offset(
                                x = cellWidth * (selectedWidget.gridColumn + selectedWidget.columnSpan) -
                                    WIDGET_BORDER_INSET - HANDLE_SIZE / 2,
                                y = verticalCenter - HANDLE_SIZE / 2,
                            )
                            .zIndex(Float.MAX_VALUE - 1), // always above widgets, below grid-line overlay
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
                        modifier = Modifier
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
                        modifier = Modifier
                            .offset(
                                x = horizontalCenter - HANDLE_SIZE / 2,
                                y = cellHeight * (selectedWidget.gridRow + selectedWidget.rowSpan) -
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
                        modifier = Modifier
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
                        drawLine(color = gridLineColor, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 2f)
                    }
                    for (row in 1 until grid.rows) {
                        val y = cellHeightPx * row
                        drawLine(color = gridLineColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 2f)
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
                modifier = Modifier
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
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.removeWidget(selectedWidgetId!!)
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
        modifier = modifier
            .padding(WIDGET_BORDER_INSET)
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
            .then(
                if (isSelected) {
                    Modifier.pointerInput(widget.id, cellWidth, cellHeight) {
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
                                // A saved widget's span can exceed the live-measured grid (e.g.
                                // it was placed on a differently-sized canvas) - coerceAtLeast(0)
                                // keeps the coerceIn below from throwing on an empty range in
                                // that case, pinning the widget to the origin instead of crashing.
                                maxColumn = (grid.columns - liveWidget.columnSpan).coerceAtLeast(0)
                                maxRow = (grid.rows - liveWidget.rowSpan).coerceAtLeast(0)
                                accumX = 0f
                                accumY = 0f
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
                            accumX += dragAmount.x
                            accumY += dragAmount.y

                            val columnDelta = (accumX / cellWidthPx).toInt()
                            val rowDelta = (accumY / cellHeightPx).toInt()

                            if (columnDelta != 0 || rowDelta != 0) {
                                val previousColumn = currentColumn
                                val previousRow = currentRow
                                currentColumn = (currentColumn + columnDelta).coerceIn(0, maxColumn)
                                currentRow = (currentRow + rowDelta).coerceIn(0, maxRow)
                                accumX -= columnDelta * cellWidthPx
                                accumY -= rowDelta * cellHeightPx
                                // Fires once at the moment the drag is clamped onto a grid
                                // boundary (a real transition, not every tick spent pinned
                                // against it) - same "hit the wall" feedback as the resize
                                // handles' edge-snap below.
                                val hitBoundary = (currentColumn != previousColumn && (currentColumn == 0 || currentColumn == maxColumn)) ||
                                    (currentRow != previousRow && (currentRow == 0 || currentRow == maxRow))
                                if (hitBoundary) {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                onMove(widget.id, currentColumn, currentRow)
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
                textUserScrollEnabled = false,
            )
        }
    }
}

/** Which side of the selected widget a [ResizeHandle] sits on and drags along. Left/Right
 * affect the column axis, Top/Bottom the row axis - each handle only ever touches one
 * axis, unlike the single bottom-right corner handle this replaced, which resized both
 * at once. */
private enum class ResizeEdge { Left, Right, Top, Bottom }

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
private fun ResizeHandle(
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
        modifier = modifier
            .size(HANDLE_SIZE) // touch target - see HANDLE_SIZE's kdoc for why this stays full-size
            .pointerInput(widget.id, edge, cellWidth, cellHeight, grid) {
                val cellWidthPx = with(density) { cellWidth.toPx() }
                val cellHeightPx = with(density) { cellHeight.toPx() }
                val cellPx = if (isColumnAxis) cellWidthPx else cellHeightPx
                val gridExtentCells = if (isColumnAxis) grid.columns else grid.rows
                val gridExtentPx = cellPx * gridExtentCells
                val edgeSnapThresholdPx = with(density) { EDGE_SNAP_THRESHOLD.toPx() }

                // maxSpan means different things per anchor: for a far-edge (Right/Bottom)
                // handle it's the largest span can grow to; for a near-edge (Left/Top)
                // handle it's the anchored far edge's fixed cell position, since span
                // there is derived as maxSpan - start.
                var maxSpan = 0
                var start = 0
                var span = 0
                var initialFarEdgePx = 0f
                var initialStartPx = 0f
                var accum = 0f
                var totalDrag = 0f

                detectDragGestures(
                    onDragStart = {
                        val liveWidget = currentWidget
                        start = if (isColumnAxis) liveWidget.gridColumn else liveWidget.gridRow
                        span = if (isColumnAxis) liveWidget.columnSpan else liveWidget.rowSpan
                        // Same reasoning as PlaceholderWidgetBox's onDragStart: a saved
                        // widget's position can leave less room than MIN_SPAN on a
                        // live-measured grid smaller than the one it was placed on -
                        // coerceAtLeast(MIN_SPAN) keeps the coerceIn calls below from
                        // throwing on an empty range in that case.
                        maxSpan = if (isFarEdge) {
                            (gridExtentCells - start).coerceAtLeast(MIN_SPAN)
                        } else {
                            (start + span).coerceAtLeast(MIN_SPAN)
                        }
                        initialFarEdgePx = (start + span) * cellPx
                        initialStartPx = start * cellPx
                        accum = 0f
                        totalDrag = 0f
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
                    accum += delta
                    totalDrag += delta

                    var changed = false

                    if (isFarEdge) {
                        // Right/Bottom: the far edge moves with the drag, start is fixed.
                        val farEdgePx = initialFarEdgePx + totalDrag
                        val nearGridBoundary = gridExtentPx - farEdgePx <= edgeSnapThresholdPx
                        if (nearGridBoundary && span != maxSpan) {
                            span = maxSpan
                            changed = true
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        } else if (!nearGridBoundary) {
                            val cellDelta = (accum / cellPx).toInt()
                            if (cellDelta != 0) {
                                span = (span + cellDelta).coerceIn(MIN_SPAN, maxSpan)
                                accum -= cellDelta * cellPx
                                changed = true
                            }
                        }
                    } else {
                        // Left/Top: the near edge (start) moves with the drag, the far
                        // edge is fixed - start and span must update together.
                        val startPx = initialStartPx + totalDrag
                        val nearGridBoundary = startPx <= edgeSnapThresholdPx
                        if (nearGridBoundary && start != 0) {
                            start = 0
                            span = maxSpan
                            changed = true
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        } else if (!nearGridBoundary) {
                            val cellDelta = (accum / cellPx).toInt()
                            if (cellDelta != 0) {
                                start = (start + cellDelta).coerceIn(0, maxSpan - MIN_SPAN)
                                span = maxSpan - start
                                accum -= cellDelta * cellPx
                                changed = true
                            }
                        }
                    }

                    if (changed) {
                        val liveWidget = currentWidget
                        onResize(
                            widget.id,
                            if (isColumnAxis) start else liveWidget.gridColumn,
                            if (isColumnAxis) liveWidget.gridRow else start,
                            if (isColumnAxis) span else liveWidget.columnSpan,
                            if (isColumnAxis) liveWidget.rowSpan else span,
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(HANDLE_DOT_SIZE)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
        )
    }
}

private fun WidgetType.label(): String = when (this) {
    is WidgetType.SystemLogo -> "System Logo"
    is WidgetType.SystemImage -> "System Image"
    is WidgetType.SystemMedia -> mediaType.systemWidgetLabel()
    is WidgetType.GameMedia -> mediaType.gameWidgetLabel()
    is WidgetType.CustomImage -> "Custom Image"
    is WidgetType.ColorBackground -> "Color Background"
    is WidgetType.GameDescription -> "Description"
}

/** Display label for a MediaType-backed System-canvas widget - only FanArt/Screenshots
 * ever appear there (see widgetCatalogFor), named for what they actually show: a random
 * game's art for the browsed system, not the system's own art. */
private fun MediaType.systemWidgetLabel(): String = when (this) {
    MediaType.FanArt -> "Random Game Fanart"
    MediaType.Screenshots -> "Random Game Screenshot"
    else -> "System: $name"
}

/** Display label for a MediaType-backed Game-canvas widget - plain-English names in
 * place of the raw MediaType enum constant (e.g. "Box Cover" instead of "Covers", "3D
 * Box" instead of "ThreeDBoxes"). */
private fun MediaType.gameWidgetLabel(): String = when (this) {
    MediaType.Marquees -> "Marquee"
    MediaType.Covers -> "Box Cover"
    MediaType.ThreeDBoxes -> "3D Box"
    MediaType.MixImages -> "Mix Image"
    MediaType.Screenshots -> "Screenshot"
    MediaType.FanArt -> "Fan Art"
    MediaType.TitleScreens -> "Title Screen"
    MediaType.BackCovers -> "Box Back Cover"
    MediaType.PhysicalMedia -> "Physical Media"
    else -> "Game: $name"
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
    val hapticFeedback = LocalHapticFeedback.current
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
                            .clickable {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                onPick(widgetType)
                            }
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (widgetType) {
                    is WidgetType.SystemLogo -> {
                        ScaleModeConfig(current = widgetType.scaleMode) { onChange(widgetType.copy(scaleMode = it)) }
                        LogoTransitionPicker(current = widgetType.logoTransitionMode) { onChange(widgetType.copy(logoTransitionMode = it)) }
                        GlintConfig(enabled = widgetType.glintEnabled) { onChange(widgetType.copy(glintEnabled = it)) }
                        ImageEffectsConfig(current = widgetType.effects) { onChange(widgetType.copy(effects = it)) }
                    }

                    is WidgetType.SystemImage -> {
                        ScaleModeConfig(current = widgetType.scaleMode) { onChange(widgetType.copy(scaleMode = it)) }
                        if (widgetType.supportsImageTransition && widgetType.allowsFadeTransition) {
                            ImageTransitionPicker(current = widgetType.imageTransitionMode) { onChange(widgetType.copy(imageTransitionMode = it)) }
                        }
                        if (widgetType.supportsPanZoom) {
                            PanZoomConfig(enabled = widgetType.panZoomEnabled) { onChange(widgetType.copy(panZoomEnabled = it)) }
                        }
                        ImageEffectsConfig(current = widgetType.effects) { onChange(widgetType.copy(effects = it)) }
                    }

                    is WidgetType.SystemMedia -> {
                        ScaleModeConfig(current = widgetType.scaleMode) { onChange(widgetType.copy(scaleMode = it)) }
                        if (widgetType.isLogoStyle) {
                            LogoTransitionPicker(current = widgetType.logoTransitionMode) { onChange(widgetType.copy(logoTransitionMode = it)) }
                            GlintConfig(enabled = widgetType.glintEnabled) { onChange(widgetType.copy(glintEnabled = it)) }
                        } else if (widgetType.supportsImageTransition && widgetType.allowsFadeTransition) {
                            ImageTransitionPicker(current = widgetType.imageTransitionMode) { onChange(widgetType.copy(imageTransitionMode = it)) }
                        }
                        if (widgetType.supportsPanZoom) {
                            PanZoomConfig(enabled = widgetType.panZoomEnabled) { onChange(widgetType.copy(panZoomEnabled = it)) }
                        }
                        ImageEffectsConfig(current = widgetType.effects) { onChange(widgetType.copy(effects = it)) }
                    }

                    is WidgetType.GameMedia -> {
                        ScaleModeConfig(current = widgetType.scaleMode) { onChange(widgetType.copy(scaleMode = it)) }
                        if (widgetType.isLogoStyle) {
                            LogoTransitionPicker(current = widgetType.logoTransitionMode) { onChange(widgetType.copy(logoTransitionMode = it)) }
                            GlintConfig(enabled = widgetType.glintEnabled) { onChange(widgetType.copy(glintEnabled = it)) }
                        } else if (widgetType.supportsImageTransition && widgetType.allowsFadeTransition) {
                            ImageTransitionPicker(current = widgetType.imageTransitionMode) { onChange(widgetType.copy(imageTransitionMode = it)) }
                        }
                        if (widgetType.supportsPanZoom) {
                            PanZoomConfig(enabled = widgetType.panZoomEnabled) { onChange(widgetType.copy(panZoomEnabled = it)) }
                        }
                        ImageEffectsConfig(current = widgetType.effects) { onChange(widgetType.copy(effects = it)) }
                    }

                    is WidgetType.CustomImage -> {
                        CustomImageConfig(current = widgetType, onChange = onChange)
                        ScaleModeConfig(current = widgetType.scaleMode) { onChange(widgetType.copy(scaleMode = it)) }
                        if (widgetType.supportsImageTransition && widgetType.allowsFadeTransition) {
                            ImageTransitionPicker(current = widgetType.imageTransitionMode) { onChange(widgetType.copy(imageTransitionMode = it)) }
                        }
                        if (widgetType.supportsPanZoom) {
                            PanZoomConfig(enabled = widgetType.panZoomEnabled) { onChange(widgetType.copy(panZoomEnabled = it)) }
                        }
                        ImageEffectsConfig(current = widgetType.effects) { onChange(widgetType.copy(effects = it)) }
                    }

                    is WidgetType.ColorBackground ->
                        ColorBackgroundConfig(current = widgetType, onChange = onChange)

                    is WidgetType.GameDescription ->
                        GameDescriptionConfig(current = widgetType, onChange = onChange)
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

/** Shows the currently-picked file's name (or a prompt if none picked yet, e.g. this
 * widget was just added and the picker was cancelled) and a button to (re)launch the
 * same document picker used on add - see addCustomImageLauncher in EditWidgetsOverlay. */
@Composable
private fun CustomImageConfig(
    current: WidgetType.CustomImage,
    onChange: (WidgetType.CustomImage) -> Unit,
) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(SafPathResolver::resolveDocumentPath)?.let { path -> onChange(current.copy(path = path)) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Image", style = MaterialTheme.typography.titleSmall)
        Text(
            text = if (current.path.isNotBlank()) current.path.substringAfterLast('/') else "No image chosen",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = { launcher.launch(arrayOf("image/*")) }) {
            Text(if (current.path.isNotBlank()) "Change Image" else "Choose Image")
        }
    }
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
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Blur: ${(current.blurAmount * 100).roundToInt()}%", style = MaterialTheme.typography.titleSmall)
            Slider(
                value = current.blurAmount,
                onValueChange = { onChange(current.copy(blurAmount = it)) },
                valueRange = 0f..1f,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Darken: ${(current.darkenAmount * 100).roundToInt()}%", style = MaterialTheme.typography.titleSmall)
            Slider(
                value = current.darkenAmount,
                onValueChange = { onChange(current.copy(darkenAmount = it)) },
                valueRange = 0f..1f,
            )
        }
    }
}

/**
 * Only shown when [WidgetType.supportsPanZoom] is true for the widget being configured -
 * eligible image-backed types with ScaleMode.Fill (see its kdoc for why Fit is excluded).
 * See PanZoomImage.kt for the animation this toggle drives.
 */
@Composable
private fun PanZoomConfig(enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Pan & Zoom", style = MaterialTheme.typography.titleSmall)
            Text(
                text = "Slowly zooms and pans across the image while it's displayed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = enabled, onCheckedChange = onChange)
    }
}

/**
 * Only shown when [WidgetType.supportsImageTransition] AND [WidgetType.allowsFadeTransition]
 * are both true for the widget being configured (opaque/backdrop-style image widgets, not
 * logo-style or permanently-instant box-art-style GameMedia - see supportsImageTransition's
 * kdoc). allowsFadeTransition is false whenever the widget has ScaleMode.Fit selected, in
 * which case Fade is the only entry that would matter and None is fixed anyway - hidden
 * entirely rather than shown as a locked one-option row, same reasoning as
 * [WidgetType.supportsPanZoom] hiding rather than graying out.
 */
@Composable
private fun ImageTransitionPicker(current: ImageTransitionMode, onSelected: (ImageTransitionMode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Image Transitions", style = MaterialTheme.typography.titleSmall)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ImageTransitionMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = mode == current,
                    onClick = { onSelected(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = ImageTransitionMode.entries.size),
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

private fun ImageTransitionMode.icon(): ImageVector = when (this) {
    ImageTransitionMode.None -> Icons.Filled.Block
    ImageTransitionMode.Fade -> Icons.Filled.BlurOn
}

private fun ImageTransitionMode.displayLabel(): String = when (this) {
    ImageTransitionMode.None -> "None"
    ImageTransitionMode.Fade -> "Fade"
}

/**
 * Shown for every logo-style widget (SystemLogo, or SystemMedia/GameMedia with mediaType
 * Marquees - see [WidgetType.isLogoStyle]). Deliberately no Fade option - see
 * AnimatedLogoImage's kdoc for why (double-exposure on overlapping transparent images).
 */
@Composable
private fun LogoTransitionPicker(current: LogoTransitionMode, onSelected: (LogoTransitionMode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Logo Transitions", style = MaterialTheme.typography.titleSmall)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            LogoTransitionMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = mode == current,
                    onClick = { onSelected(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = LogoTransitionMode.entries.size),
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

private fun LogoTransitionMode.icon(): ImageVector = when (this) {
    LogoTransitionMode.None -> Icons.Filled.Block
    LogoTransitionMode.Slide -> Icons.Filled.ArrowForward
    LogoTransitionMode.Scale -> Icons.Filled.ZoomIn
}

private fun LogoTransitionMode.displayLabel(): String = when (this) {
    LogoTransitionMode.None -> "None"
    LogoTransitionMode.Slide -> "Slide"
    LogoTransitionMode.Scale -> "Scale"
}

/**
 * Shown alongside [LogoTransitionPicker] for the same logo-style widget types -
 * independent of [LogoTransitionMode], a periodic ambient shimmer orthogonal to whichever
 * entrance-transition mode is selected above (see AnimatedLogoImage's glint loop).
 */
@Composable
private fun GlintConfig(enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Logo Glint", style = MaterialTheme.typography.titleSmall)
            Text(
                text = "A periodic light sweep across this widget",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = enabled, onCheckedChange = onChange)
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
    val hapticFeedback = LocalHapticFeedback.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Color", style = MaterialTheme.typography.titleSmall)
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
                            .clickable {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                onChange(current.copy(colorArgb = colorArgb))
                            },
                    )
                }
            }
            HexColorInput(current = current.colorArgb) { onChange(current.copy(colorArgb = it)) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Transparency: ${(current.alpha * 100).roundToInt()}%", style = MaterialTheme.typography.titleSmall)
            Slider(
                value = current.alpha,
                onValueChange = { onChange(current.copy(alpha = it)) },
                valueRange = 0f..1f,
            )
        }
    }
}

@Composable
private fun GameDescriptionConfig(
    current: WidgetType.GameDescription,
    onChange: (WidgetType.GameDescription) -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Text Size: ${current.fontSizeSp.roundToInt()}sp", style = MaterialTheme.typography.titleSmall)
            Slider(
                value = current.fontSizeSp,
                onValueChange = { onChange(current.copy(fontSizeSp = it)) },
                valueRange = 10f..36f,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Text Color", style = MaterialTheme.typography.titleSmall)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                COLOR_PRESETS.forEach { colorArgb ->
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(36.dp)
                            .background(Color(colorArgb), CircleShape)
                            .then(
                                if (colorArgb == current.textColorArgb) {
                                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                } else {
                                    Modifier
                                },
                            )
                            .clickable {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                onChange(current.copy(textColorArgb = colorArgb))
                            },
                    )
                }
            }
            HexColorInput(current = current.textColorArgb) { onChange(current.copy(textColorArgb = it)) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Background Color", style = MaterialTheme.typography.titleSmall)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                COLOR_PRESETS.forEach { colorArgb ->
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(36.dp)
                            .background(Color(colorArgb), CircleShape)
                            .then(
                                if (colorArgb == current.backgroundColorArgb) {
                                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                } else {
                                    Modifier
                                },
                            )
                            .clickable {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                onChange(current.copy(backgroundColorArgb = colorArgb))
                            },
                    )
                }
            }
            HexColorInput(current = current.backgroundColorArgb) { onChange(current.copy(backgroundColorArgb = it)) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Background Transparency: ${(current.backgroundAlpha * 100).roundToInt()}%",
                style = MaterialTheme.typography.titleSmall,
            )
            Slider(
                value = current.backgroundAlpha,
                onValueChange = { onChange(current.copy(backgroundAlpha = it)) },
                valueRange = 0f..1f,
            )
        }
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
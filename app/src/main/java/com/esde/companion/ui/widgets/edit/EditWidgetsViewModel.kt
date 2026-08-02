package com.esde.companion.ui.widgets.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esde.companion.domain.model.DockSize
import com.esde.companion.domain.model.GridDimensions
import com.esde.companion.domain.model.ImageTransitionMode
import com.esde.companion.domain.model.InstalledApp
import com.esde.companion.domain.model.LogoTransitionMode
import com.esde.companion.domain.model.MediaType
import com.esde.companion.domain.model.PlacedWidget
import com.esde.companion.domain.model.StateGroup
import com.esde.companion.domain.model.WidgetContent
import com.esde.companion.domain.model.WidgetContentResolver
import com.esde.companion.domain.model.WidgetType
import com.esde.companion.domain.usecase.ObserveDockAppsUseCase
import com.esde.companion.domain.usecase.ObserveDockEnabledUseCase
import com.esde.companion.domain.usecase.ObserveDockMaxAppsUseCase
import com.esde.companion.domain.usecase.ObserveDockOpacityUseCase
import com.esde.companion.domain.usecase.ObserveDockSizeUseCase
import com.esde.companion.domain.usecase.ObserveImageTransitionModeUseCase
import com.esde.companion.domain.usecase.ObserveInstalledAppsUseCase
import com.esde.companion.domain.usecase.ObserveLastGameReferenceUseCase
import com.esde.companion.domain.usecase.ObserveLastSystemShortNameUseCase
import com.esde.companion.domain.usecase.ObserveLogoTransitionModeUseCase
import com.esde.companion.domain.usecase.ObserveWidgetCanvasUseCase
import com.esde.companion.domain.usecase.ResolveCustomSystemImageUseCase
import com.esde.companion.domain.usecase.ResolveCustomSystemLogoUseCase
import com.esde.companion.domain.usecase.ResolveGameDescriptionUseCase
import com.esde.companion.domain.usecase.ResolveGameMediaUseCase
import com.esde.companion.domain.usecase.ResolveRandomSystemMediaUseCase
import com.esde.companion.domain.usecase.SaveWidgetCanvasUseCase
import com.esde.companion.ui.main.systemLogoAssetName
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** New widgets default to a 4x4 cell footprint (clamped to the grid's actual size on
 * very small/coarse grids), centered on the canvas - a reasonable starting size/position
 * for the person to then drag and resize into place. */
private const val DEFAULT_NEW_WIDGET_SPAN = 4

/**
 * Drives the edit-mode overlay. Unlike the live WidgetsViewModel, [widgets] here is a
 * local, mutable snapshot rather than a continuous subscription to the repository -
 * dragging/resizing needs to mutate positions in place without a live DataStore emission
 * (including our own saves) racing an in-progress gesture. [widgets] is (re)loaded once
 * whenever the selected canvas or grid dimensions change, and explicitly persisted via
 * [persistWidgets] rather than on every position/size change.
 *
 * [previewContent] gives edit mode something real to preview rather than only labeled
 * placeholder boxes - resolved from the last known browsed system/game (see
 * LastKnownContextRepository), NOT the live AppState, since edit mode is deliberately
 * decoupled from whatever's happening live (see EditWidgetsOverlay's kdoc on why the
 * canvas underneath is fully obscured while editing). A widget whose type has no
 * resolvable content for that last-known context (e.g. no back-cover art for this
 * particular system) gets WidgetContent.Empty in the map, which EditWidgetsOverlay
 * interprets as "fall back to the placeholder box" - per-widget, not per-canvas, so a
 * mix of real previews and placeholders on the same canvas is expected and fine.
 * Recomputed only at the same discrete points [widgets] itself changes for reasons other
 * than a live drag (reload, add, configure) - never on every drag/resize tick, since
 * content resolution doesn't depend on position/size at all.
 *
 * [isDragging] is shared drag-in-progress state, driven by whichever widget/handle is
 * currently being dragged - used by EditWidgetsOverlay to fade the options button out of
 * the way during an active drag rather than only at rest.
 *
 * [selectedWidgetId] is which widget currently shows its resize handle - null means none
 * selected. Cleared on canvas switch since a selection from one canvas has no meaning on
 * another.
 */
class EditWidgetsViewModel(
    private val observeWidgetCanvas: ObserveWidgetCanvasUseCase,
    private val saveWidgetCanvas: SaveWidgetCanvasUseCase,
    private val resolveGameMedia: ResolveGameMediaUseCase,
    private val resolveRandomSystemMedia: ResolveRandomSystemMediaUseCase,
    private val resolveGameDescription: ResolveGameDescriptionUseCase,
    private val resolveCustomSystemImage: ResolveCustomSystemImageUseCase,
    private val resolveCustomSystemLogo: ResolveCustomSystemLogoUseCase,
    private val observeLastSystemShortName: ObserveLastSystemShortNameUseCase,
    private val observeLastGameReference: ObserveLastGameReferenceUseCase,
    observeImageTransitionMode: ObserveImageTransitionModeUseCase,
    observeLogoTransitionMode: ObserveLogoTransitionModeUseCase,
    observeDockEnabled: ObserveDockEnabledUseCase,
    observeDockSize: ObserveDockSizeUseCase,
    observeDockOpacity: ObserveDockOpacityUseCase,
    observeDockMaxApps: ObserveDockMaxAppsUseCase,
    observeDockApps: ObserveDockAppsUseCase,
    observeInstalledApps: ObserveInstalledAppsUseCase,
) : ViewModel() {

    val imageTransitionMode: StateFlow<ImageTransitionMode> = observeImageTransitionMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000), ImageTransitionMode.None)

    val logoTransitionMode: StateFlow<LogoTransitionMode> = observeLogoTransitionMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000), LogoTransitionMode.None)

    val dockEnabled: StateFlow<Boolean> = observeDockEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000), false)

    val dockSize: StateFlow<DockSize> = observeDockSize()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000), DockSize.Medium)

    val dockOpacityPercent: StateFlow<Int> = observeDockOpacity()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000), 60)

    val dockMaxApps: StateFlow<Int> = observeDockMaxApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000), 5)

    /** Same resolution as AppDockViewModel.dockItems (pinned package names -> InstalledApp,
     * capped to maxApps) - duplicated rather than shared, per that file's own precedent for
     * small logic at this size (see its kdoc on recordLaunchLocation). Purely for
     * DockPreview - this ViewModel never lets the dock's own contents be edited here. */
    val dockItems: StateFlow<List<InstalledApp>> =
        combine(observeDockApps(), observeInstalledApps(), observeDockMaxApps()) { pinned, installed, max ->
            val byPackageName = installed.associateBy { it.packageName }
            pinned.take(max).mapNotNull { byPackageName[it] }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000), emptyList())

    private var gridDimensions: GridDimensions? = null
    private var loadJob: Job? = null

    /** Caches each (system, media type) random pick for the life of this ViewModel, so
     * repeatedly resolving preview content (e.g. after every Configure Widget tweak, which
     * calls [refreshPreviewContent] on every change) keeps showing the same picture rather
     * than rerolling ResolveRandomSystemMediaUseCase's random pick each time. A cache miss
     * naturally occurs when [resolvePreviewContent] is asked about a system it hasn't seen
     * yet, so browsing to a different system still gets its own fresh random pick. */
    private val randomSystemMediaCache = mutableMapOf<Pair<String, MediaType>, String?>()

    private val _selectedCanvas = MutableStateFlow(StateGroup.System)
    val selectedCanvas: StateFlow<StateGroup> = _selectedCanvas.asStateFlow()

    private val _widgets = MutableStateFlow<List<PlacedWidget>>(emptyList())
    val widgets: StateFlow<List<PlacedWidget>> = _widgets.asStateFlow()

    private val _previewContent = MutableStateFlow<Map<String, WidgetContent>>(emptyMap())
    val previewContent: StateFlow<Map<String, WidgetContent>> = _previewContent.asStateFlow()

    private val _isDragging = MutableStateFlow(false)
    val isDragging: StateFlow<Boolean> = _isDragging.asStateFlow()

    private val _selectedWidgetId = MutableStateFlow<String?>(null)
    val selectedWidgetId: StateFlow<String?> = _selectedWidgetId.asStateFlow()

    fun setGridDimensions(grid: GridDimensions) {
        gridDimensions = grid
        reload()
    }

    fun selectCanvas(stateGroup: StateGroup) {
        if (_selectedCanvas.value == stateGroup) return
        _selectedCanvas.value = stateGroup
        _selectedWidgetId.value = null
        reload()
    }

    fun setDragging(dragging: Boolean) {
        _isDragging.value = dragging
    }

    fun selectWidget(widgetId: String?) {
        _selectedWidgetId.value = widgetId
    }

    /** Live preview during a drag - updates the in-memory position only, no persistence,
     * and deliberately does not touch previewContent - content resolution never depends
     * on position/size. */
    fun updateWidgetPosition(widgetId: String, gridColumn: Int, gridRow: Int) {
        _widgets.value = _widgets.value.map { widget ->
            if (widget.id == widgetId) widget.copy(gridColumn = gridColumn, gridRow = gridRow) else widget
        }
    }

    /** Live preview during a resize - updates the in-memory size only, no persistence,
     * same non-interaction with previewContent as updateWidgetPosition. */
    fun updateWidgetSize(widgetId: String, columnSpan: Int, rowSpan: Int) {
        _widgets.value = _widgets.value.map { widget ->
            if (widget.id == widgetId) widget.copy(columnSpan = columnSpan, rowSpan = rowSpan) else widget
        }
    }

    /**
     * Replaces the selected widget's WidgetType (scale mode, or color/alpha for
     * ColorBackground) while leaving its position/size untouched. Persists immediately
     * and refreshes previewContent, since the widget's resolved image can genuinely
     * change here (e.g. switching a GameMedia widget's underlying media type would, if
     * that were configurable - scale mode alone doesn't change *which* file is shown,
     * but refreshing unconditionally is cheap and keeps this correct if that ever
     * changes).
     */
    fun updateWidgetConfig(widgetId: String, widgetType: WidgetType) {
        _widgets.value = _widgets.value.map { widget ->
            if (widget.id == widgetId) widget.copy(widgetType = widgetType) else widget
        }
        persistWidgets()
        refreshPreviewContent()
    }

    /**
     * Adds a new widget of [widgetType] to the current canvas: centered, sized to
     * [DEFAULT_NEW_WIDGET_SPAN] (clamped to the grid), and given the highest zIndex of
     * any widget currently on the canvas so it's immediately visible on top rather than
     * potentially buried under an existing full-bleed backdrop. Selected immediately so
     * the person can drag/resize it right away without a separate tap-to-select step.
     */
    fun addWidget(widgetType: WidgetType) {
        val grid = gridDimensions ?: return
        val columnSpan = DEFAULT_NEW_WIDGET_SPAN.coerceAtMost(grid.columns)
        val rowSpan = DEFAULT_NEW_WIDGET_SPAN.coerceAtMost(grid.rows)
        val gridColumn = ((grid.columns - columnSpan) / 2).coerceAtLeast(0)
        val gridRow = ((grid.rows - rowSpan) / 2).coerceAtLeast(0)
        val nextZIndex = (_widgets.value.maxOfOrNull { it.zIndex } ?: -1) + 1

        val newWidget = PlacedWidget(
            id = UUID.randomUUID().toString(),
            widgetType = widgetType,
            gridColumn = gridColumn,
            gridRow = gridRow,
            columnSpan = columnSpan,
            rowSpan = rowSpan,
            zIndex = nextZIndex,
        )

        _widgets.value = _widgets.value + newWidget
        _selectedWidgetId.value = newWidget.id
        persistWidgets()
        refreshPreviewContent()
    }

    /**
     * Removes the widget from the current canvas and clears selection if it was the
     * removed widget.
     */
    fun removeWidget(widgetId: String) {
        _widgets.value = _widgets.value.filterNot { it.id == widgetId }
        if (_selectedWidgetId.value == widgetId) {
            _selectedWidgetId.value = null
        }
        persistWidgets()
        refreshPreviewContent()
    }

    /**
     * Swaps zIndex with whichever widget is immediately above this one in stacking
     * order - a discrete, one-step move rather than jumping straight to the top. Finds
     * the actual neighbor by sorted position rather than just incrementing zIndex by 1,
     * since zIndex values aren't guaranteed contiguous (e.g. after several add/remove/
     * reorder actions, values can have gaps) - incrementing blindly could land on top of
     * an existing widget's value instead of truly swapping places with the next one up.
     * No-op if already topmost.
     */
    fun moveUp(widgetId: String) {
        val sorted = _widgets.value.sortedBy { it.zIndex }
        val index = sorted.indexOfFirst { it.id == widgetId }
        if (index == -1 || index == sorted.lastIndex) return

        val current = sorted[index]
        val next = sorted[index + 1]
        _widgets.value = _widgets.value.map { widget ->
            when (widget.id) {
                current.id -> widget.copy(zIndex = next.zIndex)
                next.id -> widget.copy(zIndex = current.zIndex)
                else -> widget
            }
        }
        persistWidgets()
    }

    /** Same as [moveUp], swapping with the neighbor immediately below instead. No-op if
     * already at the bottom. */
    fun moveDown(widgetId: String) {
        val sorted = _widgets.value.sortedBy { it.zIndex }
        val index = sorted.indexOfFirst { it.id == widgetId }
        if (index <= 0) return

        val current = sorted[index]
        val previous = sorted[index - 1]
        _widgets.value = _widgets.value.map { widget ->
            when (widget.id) {
                current.id -> widget.copy(zIndex = previous.zIndex)
                previous.id -> widget.copy(zIndex = current.zIndex)
                else -> widget
            }
        }
        persistWidgets()
    }

    /** Called once per gesture (drag/resize end/cancel) or discrete action (add/remove),
     * not per movement - see the two update functions above. */
    fun persistWidgets() {
        val stateGroup = _selectedCanvas.value
        val widgetsToSave = _widgets.value
        val grid = gridDimensions ?: return
        viewModelScope.launch { saveWidgetCanvas(stateGroup, widgetsToSave, grid) }
    }

    private fun reload() {
        val grid = gridDimensions ?: return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val loaded = observeWidgetCanvas(_selectedCanvas.value, grid).first()
            _widgets.value = loaded
            _previewContent.value = resolvePreviewContent(loaded)
        }
    }

    private fun refreshPreviewContent() {
        viewModelScope.launch {
            _previewContent.value = resolvePreviewContent(_widgets.value)
        }
    }

    /**
     * Same resolution logic WidgetsViewModel uses for the live screen (WidgetContentResolver,
     * shared), just sourced from the last-known system/game rather than the live AppState,
     * and with no generic-background fallback - fallbackBackgroundAssetPath is null here
     * deliberately, so a widget with nothing to show resolves to WidgetContent.Empty and
     * EditWidgetsOverlay falls back to the placeholder box, rather than a full-bleed
     * generic image (which is the right fallback for the live screen, not for editing).
     */
    private suspend fun resolvePreviewContent(widgets: List<PlacedWidget>): Map<String, WidgetContent> {
        val lastSystemShortName = observeLastSystemShortName().first()
        val lastGameReference = observeLastGameReference().first()
        val gameMedia = lastGameReference?.let { resolveGameMedia(it.systemShortName, it.romPath) }
        val gameDescription = lastGameReference?.let { resolveGameDescription(it.systemShortName, it.romPath) }

        val hasSystemImageWidget = widgets.any { it.widgetType is WidgetType.SystemImage }
        val neededSystemMediaTypes = (
                widgets.mapNotNull { (it.widgetType as? WidgetType.SystemMedia)?.mediaType } +
                        if (hasSystemImageWidget) listOf(MediaType.FanArt, MediaType.Screenshots) else emptyList()
                ).distinct()
        val systemMediaByType: Map<MediaType, String?> = lastSystemShortName?.let { shortName ->
            neededSystemMediaTypes.associateWith { mediaType ->
                randomSystemMediaCache.getOrPut(shortName to mediaType) { resolveRandomSystemMedia(shortName, mediaType) }
            }
        } ?: emptyMap()

        val systemLogoAssetPath = lastSystemShortName
            ?.let { "file:///android_asset/system_logos/${systemLogoAssetName(it)}.svg" }

        val needsCustomLogo = widgets.any { it.widgetType is WidgetType.SystemLogo }
        val needsCustomImage = widgets.any { it.widgetType is WidgetType.SystemImage }
        val customSystemLogoPath = if (needsCustomLogo) lastSystemShortName?.let { resolveCustomSystemLogo(systemLogoAssetName(it)) } else null
        val customSystemImagePath = if (needsCustomImage) lastSystemShortName?.let { resolveCustomSystemImage(systemLogoAssetName(it)) } else null

        return widgets.associate { widget ->
            widget.id to WidgetContentResolver.resolve(
                widgetType = widget.widgetType,
                systemLogoAssetPath = { systemLogoAssetPath },
                customSystemLogoLookup = { customSystemLogoPath },
                customSystemImageLookup = { customSystemImagePath },
                systemMediaLookup = { mediaType -> systemMediaByType[mediaType] },
                gameMediaLookup = { mediaType -> gameMedia?.path(mediaType) },
                gameDescriptionLookup = { gameDescription?.text },
                fallbackBackgroundAssetPath = null,
            )
        }
    }
}

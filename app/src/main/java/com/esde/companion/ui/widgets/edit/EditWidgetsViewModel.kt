package com.esde.companion.ui.widgets.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esde.companion.domain.model.GridDimensions
import com.esde.companion.domain.model.PlacedWidget
import com.esde.companion.domain.model.StateGroup
import com.esde.companion.domain.usecase.ObserveWidgetCanvasUseCase
import com.esde.companion.domain.usecase.SaveWidgetCanvasUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Drives the edit-mode overlay. Unlike the live WidgetsViewModel, [widgets] here is a
 * local, mutable snapshot rather than a continuous subscription to the repository -
 * dragging/resizing needs to mutate positions in place without a live DataStore emission
 * (including our own saves) racing an in-progress gesture. [widgets] is (re)loaded once
 * whenever the selected canvas or grid dimensions change, and explicitly persisted via
 * [persistWidgets] rather than on every position/size change.
 *
 * [isDragging] is shared drag-in-progress state, driven by whichever widget/handle is
 * currently being dragged - used by EditWidgetsOverlay to fade the header out of the way
 * during an active drag rather than only at rest.
 *
 * [selectedWidgetId] is which widget currently shows its resize handle - null means none
 * selected. Cleared on canvas switch since a selection from one canvas has no meaning on
 * another.
 */
class EditWidgetsViewModel(
    private val observeWidgetCanvas: ObserveWidgetCanvasUseCase,
    private val saveWidgetCanvas: SaveWidgetCanvasUseCase,
) : ViewModel() {

    private var gridDimensions: GridDimensions? = null
    private var loadJob: Job? = null

    private val _selectedCanvas = MutableStateFlow(StateGroup.System)
    val selectedCanvas: StateFlow<StateGroup> = _selectedCanvas.asStateFlow()

    private val _widgets = MutableStateFlow<List<PlacedWidget>>(emptyList())
    val widgets: StateFlow<List<PlacedWidget>> = _widgets.asStateFlow()

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

    /** Live preview during a drag - updates the in-memory position only, no persistence. */
    fun updateWidgetPosition(widgetId: String, gridColumn: Int, gridRow: Int) {
        _widgets.value = _widgets.value.map { widget ->
            if (widget.id == widgetId) widget.copy(gridColumn = gridColumn, gridRow = gridRow) else widget
        }
    }

    /** Live preview during a resize - updates the in-memory size only, no persistence. */
    fun updateWidgetSize(widgetId: String, columnSpan: Int, rowSpan: Int) {
        _widgets.value = _widgets.value.map { widget ->
            if (widget.id == widgetId) widget.copy(columnSpan = columnSpan, rowSpan = rowSpan) else widget
        }
    }

    /** Called once per gesture (drag/resize end/cancel), not per movement - see the two
     * update functions above. */
    fun persistWidgets() {
        val stateGroup = _selectedCanvas.value
        val widgetsToSave = _widgets.value
        viewModelScope.launch { saveWidgetCanvas(stateGroup, widgetsToSave) }
    }

    private fun reload() {
        val grid = gridDimensions ?: return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _widgets.value = observeWidgetCanvas(_selectedCanvas.value, grid).first()
        }
    }
}

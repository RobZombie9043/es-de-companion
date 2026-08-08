package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.GridDimensions
import com.esde.companion.domain.model.PlacedWidget
import com.esde.companion.domain.model.StateGroup
import com.esde.companion.domain.model.defaultCanvas
import com.esde.companion.domain.model.rescaleWidgetsToGrid
import com.esde.companion.domain.repository.WidgetLayoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveWidgetCanvasUseCase(
    private val repository: WidgetLayoutRepository,
) {
    operator fun invoke(
        stateGroup: StateGroup,
        grid: GridDimensions,
    ): Flow<List<PlacedWidget>> =
        repository.observeCanvas(stateGroup).map { saved ->
            when {
                saved.widgets.isEmpty() -> defaultCanvas(stateGroup, grid)
                saved.grid == null || saved.grid == grid -> saved.widgets
                else -> rescaleWidgetsToGrid(saved.widgets, from = saved.grid, to = grid)
            }
        }
}

class SaveWidgetCanvasUseCase(
    private val repository: WidgetLayoutRepository,
) {
    suspend operator fun invoke(
        stateGroup: StateGroup,
        widgets: List<PlacedWidget>,
        grid: GridDimensions,
    ) = repository.saveCanvas(stateGroup, widgets, grid)
}

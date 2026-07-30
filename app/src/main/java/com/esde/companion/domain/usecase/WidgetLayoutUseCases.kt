package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.GridDimensions
import com.esde.companion.domain.model.PlacedWidget
import com.esde.companion.domain.model.StateGroup
import com.esde.companion.domain.model.defaultCanvas
import com.esde.companion.domain.repository.WidgetLayoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveWidgetCanvasUseCase(
    private val repository: WidgetLayoutRepository,
) {
    operator fun invoke(stateGroup: StateGroup, grid: GridDimensions): Flow<List<PlacedWidget>> =
        repository.observeCanvas(stateGroup).map { saved ->
            saved.ifEmpty { defaultCanvas(stateGroup, grid) }
        }
}

class SaveWidgetCanvasUseCase(
    private val repository: WidgetLayoutRepository,
) {
    suspend operator fun invoke(stateGroup: StateGroup, widgets: List<PlacedWidget>) =
        repository.saveCanvas(stateGroup, widgets)
}

class ObserveWidgetsLockedUseCase(
    private val repository: WidgetLayoutRepository,
) {
    operator fun invoke(): Flow<Boolean> = repository.observeWidgetsLocked()
}

class SetWidgetsLockedUseCase(
    private val repository: WidgetLayoutRepository,
) {
    suspend operator fun invoke(locked: Boolean) = repository.setWidgetsLocked(locked)
}
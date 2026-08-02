package com.esde.companion.domain.repository

import com.esde.companion.domain.model.GridDimensions
import com.esde.companion.domain.model.PlacedWidget
import com.esde.companion.domain.model.SavedWidgetCanvas
import com.esde.companion.domain.model.StateGroup
import kotlinx.coroutines.flow.Flow

/**
 * Persists each StateGroup's widget canvas independently, plus the global edit lock.
 * Interface only - the DataStore-backed implementation lives in `data`, following the
 * same pattern as FileAppDrawerSettingsRepository.
 *
 * The saved/observed grid dimensions are the grid the canvas was authored under (see
 * SavedWidgetCanvas), so ObserveWidgetCanvasUseCase can rescale placements if the app is
 * now running on a differently-sized grid.
 */
interface WidgetLayoutRepository {
    fun observeCanvas(stateGroup: StateGroup): Flow<SavedWidgetCanvas>
    suspend fun saveCanvas(stateGroup: StateGroup, widgets: List<PlacedWidget>, grid: GridDimensions)
    fun observeWidgetsLocked(): Flow<Boolean>
    suspend fun setWidgetsLocked(locked: Boolean)
}
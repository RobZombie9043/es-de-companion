package com.esde.companion.domain.repository

import com.esde.companion.domain.model.PlacedWidget
import com.esde.companion.domain.model.StateGroup
import kotlinx.coroutines.flow.Flow

/**
 * Persists each StateGroup's widget canvas independently, plus the global edit lock.
 * Interface only - the DataStore-backed implementation lives in `data`, following the
 * same pattern as FileAppDrawerSettingsRepository.
 */
interface WidgetLayoutRepository {
    fun observeCanvas(stateGroup: StateGroup): Flow<List<PlacedWidget>>
    suspend fun saveCanvas(stateGroup: StateGroup, widgets: List<PlacedWidget>)
    fun observeWidgetsLocked(): Flow<Boolean>
    suspend fun setWidgetsLocked(locked: Boolean)
}
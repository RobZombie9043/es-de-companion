package com.esde.companion.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.esde.companion.domain.model.PlacedWidget
import com.esde.companion.domain.model.StateGroup
import com.esde.companion.domain.repository.WidgetLayoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class FileWidgetLayoutRepository(
    private val context: Context,
) : WidgetLayoutRepository {

    override fun observeCanvas(stateGroup: StateGroup): Flow<List<PlacedWidget>> =
        context.widgetLayoutDataStore.data.map { prefs ->
            prefs[canvasKey(stateGroup)]?.let { Json.decodeFromString<List<PlacedWidgetDto>>(it).toDomainList() }
                ?: emptyList()
        }

    override suspend fun saveCanvas(stateGroup: StateGroup, widgets: List<PlacedWidget>) {
        context.widgetLayoutDataStore.edit {
            it[canvasKey(stateGroup)] = Json.encodeToString(widgets.toDtoList())
        }
    }

    override fun observeWidgetsLocked(): Flow<Boolean> =
        context.widgetLayoutDataStore.data.map { it[WIDGETS_LOCKED_KEY] ?: false }

    override suspend fun setWidgetsLocked(locked: Boolean) {
        context.widgetLayoutDataStore.edit { it[WIDGETS_LOCKED_KEY] = locked }
    }

    private fun canvasKey(stateGroup: StateGroup) = stringPreferencesKey("canvas_${stateGroup.name}")

    private companion object {
        val WIDGETS_LOCKED_KEY = booleanPreferencesKey("widgets_locked")
    }
}
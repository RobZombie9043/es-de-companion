package com.esde.companion.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.esde.companion.domain.model.GridDimensions
import com.esde.companion.domain.model.PlacedWidget
import com.esde.companion.domain.model.SavedWidgetCanvas
import com.esde.companion.domain.model.StateGroup
import com.esde.companion.domain.repository.WidgetLayoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class FileWidgetLayoutRepository(
    private val context: Context,
) : WidgetLayoutRepository {
    override fun observeCanvas(stateGroup: StateGroup): Flow<SavedWidgetCanvas> =
        context.widgetLayoutDataStore.data.map { prefs ->
            prefs[canvasKey(stateGroup)]?.let { decodeCanvas(it) }
                ?: SavedWidgetCanvas(grid = null, widgets = emptyList())
        }

    override suspend fun saveCanvas(
        stateGroup: StateGroup,
        widgets: List<PlacedWidget>,
        grid: GridDimensions,
    ) {
        context.widgetLayoutDataStore.edit {
            it[canvasKey(stateGroup)] = Json.encodeToString(canvasDtoOf(widgets, grid))
        }
    }

    /**
     * Data saved before grid-rescaling existed is a bare JSON array of widgets, not the
     * current {grid, widgets} object - decodeFromString<CanvasDto> throws on that shape,
     * so fall back to the old array format with grid = null (meaning "assume it already
     * matches the current grid", see SavedWidgetCanvas's kdoc).
     */
    internal fun decodeCanvas(json: String): SavedWidgetCanvas =
        try {
            Json.decodeFromString<CanvasDto>(json).toDomain()
        } catch (
            @Suppress("SwallowedException") e: SerializationException,
        ) {
            SavedWidgetCanvas(grid = null, widgets = Json.decodeFromString<List<PlacedWidgetDto>>(json).toDomainList())
        }

    private fun canvasKey(stateGroup: StateGroup) = stringPreferencesKey("canvas_${stateGroup.name}")
}

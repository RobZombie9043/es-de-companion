package com.esde.companion.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.esde.companion.domain.model.DockSize
import com.esde.companion.domain.repository.DockSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * DataStore-backed [DockSettingsRepository]. [context] is expected to be an application
 * context (see AppContainer), not an Activity context - same note as
 * [FileAppDrawerSettingsRepository].
 */
class FileDockSettingsRepository(
    private val context: Context,
) : DockSettingsRepository {
    override suspend fun setDockEnabled(enabled: Boolean) {
        context.dockSettingsDataStore.edit { it[DOCK_ENABLED_KEY] = enabled }
    }

    override fun observeDockEnabled(): Flow<Boolean> =
        context.dockSettingsDataStore.data.map {
            it[DOCK_ENABLED_KEY] ?: false
        }

    override suspend fun setDockMaxApps(maxApps: Int) {
        context.dockSettingsDataStore.edit {
            it[DOCK_MAX_APPS_KEY] = maxApps.coerceIn(MIN_DOCK_MAX_APPS, MAX_DOCK_MAX_APPS)
        }
    }

    override fun observeDockMaxApps(): Flow<Int> =
        context.dockSettingsDataStore.data.map {
            it[DOCK_MAX_APPS_KEY] ?: DEFAULT_DOCK_MAX_APPS
        }

    override suspend fun setDockSize(size: DockSize) {
        context.dockSettingsDataStore.edit { it[DOCK_SIZE_KEY] = size.name }
    }

    override fun observeDockSize(): Flow<DockSize> =
        context.dockSettingsDataStore.data.map { prefs ->
            prefs[DOCK_SIZE_KEY]?.let { name -> DockSize.entries.find { it.name == name } } ?: DockSize.Medium
        }

    override suspend fun setDockApps(packageNames: List<String>) {
        context.dockSettingsDataStore.edit { it[DOCK_APPS_KEY] = Json.encodeToString(packageNames) }
    }

    override fun observeDockApps(): Flow<List<String>> =
        context.dockSettingsDataStore.data.map { prefs ->
            prefs[DOCK_APPS_KEY]?.let { Json.decodeFromString<List<String>>(it) } ?: emptyList()
        }

    private companion object {
        const val DEFAULT_DOCK_MAX_APPS = 5
        const val MIN_DOCK_MAX_APPS = 2
        const val MAX_DOCK_MAX_APPS = 5

        val DOCK_ENABLED_KEY = booleanPreferencesKey("dock_enabled")
        val DOCK_MAX_APPS_KEY = intPreferencesKey("dock_max_apps")
        val DOCK_SIZE_KEY = stringPreferencesKey("dock_size")
        val DOCK_APPS_KEY = stringPreferencesKey("dock_apps")
    }
}

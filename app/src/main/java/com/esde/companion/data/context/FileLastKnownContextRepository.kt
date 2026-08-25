package com.esde.companion.data.context

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.esde.companion.domain.model.GameReference
import com.esde.companion.domain.repository.LastKnownContextRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal val Context.lastKnownContextDataStore by preferencesDataStore(name = "last_known_context")

/**
 * DataStore Preferences-backed - a separate small file from widget_layout's, since this
 * is conceptually unrelated (a fact about what's been browsed, not a layout preference)
 * even though both are simple key/value data.
 */
class FileLastKnownContextRepository(
    private val context: Context,
) : LastKnownContextRepository {
    override fun observeLastSystemShortName(): Flow<String?> =
        context.lastKnownContextDataStore.data.map {
            it[SYSTEM_SHORT_NAME_KEY]
        }

    override suspend fun setLastSystemShortName(systemShortName: String) {
        context.lastKnownContextDataStore.edit { it[SYSTEM_SHORT_NAME_KEY] = systemShortName }
    }

    override fun observeLastGameReference(): Flow<GameReference?> =
        context.lastKnownContextDataStore.data.map { prefs ->
            val systemShortName = prefs[GAME_SYSTEM_SHORT_NAME_KEY]
            val romPath = prefs[GAME_ROM_PATH_KEY]
            if (systemShortName != null && romPath != null) {
                GameReference(systemShortName, romPath, prefs[GAME_SYSTEM_PATH_KEY])
            } else {
                null
            }
        }

    override suspend fun setLastGameReference(gameReference: GameReference) {
        context.lastKnownContextDataStore.edit {
            it[GAME_SYSTEM_SHORT_NAME_KEY] = gameReference.systemShortName
            it[GAME_ROM_PATH_KEY] = gameReference.romPath
            if (gameReference.systemPath != null) {
                it[GAME_SYSTEM_PATH_KEY] = gameReference.systemPath
            } else {
                it.remove(GAME_SYSTEM_PATH_KEY)
            }
        }
    }

    private companion object {
        val SYSTEM_SHORT_NAME_KEY = stringPreferencesKey("last_system_short_name")
        val GAME_SYSTEM_SHORT_NAME_KEY = stringPreferencesKey("last_game_system_short_name")
        val GAME_ROM_PATH_KEY = stringPreferencesKey("last_game_rom_path")
        val GAME_SYSTEM_PATH_KEY = stringPreferencesKey("last_game_system_path")
    }
}

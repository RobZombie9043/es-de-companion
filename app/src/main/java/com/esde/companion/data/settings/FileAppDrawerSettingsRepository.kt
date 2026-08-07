package com.esde.companion.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.esde.companion.domain.repository.AppDrawerSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore-backed [AppDrawerSettingsRepository]. [context] is expected to be an
 * application context (see AppContainer), not an Activity context - same note as
 * [FileOnboardingRepository].
 */
class FileAppDrawerSettingsRepository(
    private val context: Context,
) : AppDrawerSettingsRepository {

    override suspend fun setHiddenApps(packageNames: Set<String>) {
        context.appDrawerSettingsDataStore.edit { it[HIDDEN_APPS_KEY] = packageNames }
    }

    override fun observeHiddenApps(): Flow<Set<String>> =
        context.appDrawerSettingsDataStore.data.map { it[HIDDEN_APPS_KEY] ?: emptySet() }

    override suspend fun setGridColumns(columns: Int) {
        context.appDrawerSettingsDataStore.edit { it[GRID_COLUMNS_KEY] = columns.coerceIn(3, 6) }
    }

    override fun observeGridColumns(): Flow<Int> =
        context.appDrawerSettingsDataStore.data.map { it[GRID_COLUMNS_KEY] ?: DEFAULT_GRID_COLUMNS }

    override suspend fun setOtherScreenLaunchApps(packageNames: Set<String>) {
        context.appDrawerSettingsDataStore.edit { it[OTHER_SCREEN_LAUNCH_APPS_KEY] = packageNames }
    }

    override fun observeOtherScreenLaunchApps(): Flow<Set<String>> =
        context.appDrawerSettingsDataStore.data.map { it[OTHER_SCREEN_LAUNCH_APPS_KEY] ?: emptySet() }

    override suspend fun setSortFoldersOnTop(sortOnTop: Boolean) {
        context.appDrawerSettingsDataStore.edit { it[SORT_FOLDERS_ON_TOP_KEY] = sortOnTop }
    }

    override fun observeSortFoldersOnTop(): Flow<Boolean> =
        context.appDrawerSettingsDataStore.data.map { it[SORT_FOLDERS_ON_TOP_KEY] ?: true }

    private companion object {
        const val DEFAULT_GRID_COLUMNS = 5

        val HIDDEN_APPS_KEY = stringSetPreferencesKey("hidden_app_packages")
        val GRID_COLUMNS_KEY = intPreferencesKey("grid_columns")
        val OTHER_SCREEN_LAUNCH_APPS_KEY = stringSetPreferencesKey("other_screen_launch_app_packages")
        val SORT_FOLDERS_ON_TOP_KEY = booleanPreferencesKey("sort_folders_on_top")
    }
}
package com.esde.companion.data.settings

import android.content.Context
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

    override suspend fun setDrawerOpacityPercent(percent: Int) {
        context.appDrawerSettingsDataStore.edit { it[DRAWER_OPACITY_KEY] = percent.coerceIn(0, 100) }
    }

    override fun observeDrawerOpacityPercent(): Flow<Int> =
        context.appDrawerSettingsDataStore.data.map { it[DRAWER_OPACITY_KEY] ?: DEFAULT_DRAWER_OPACITY_PERCENT }

    override suspend fun setGridColumns(columns: Int) {
        context.appDrawerSettingsDataStore.edit { it[GRID_COLUMNS_KEY] = columns.coerceIn(3, 6) }
    }

    override fun observeGridColumns(): Flow<Int> =
        context.appDrawerSettingsDataStore.data.map { it[GRID_COLUMNS_KEY] ?: DEFAULT_GRID_COLUMNS }

    private companion object {
        const val DEFAULT_DRAWER_OPACITY_PERCENT = 80
        const val DEFAULT_GRID_COLUMNS = 4

        val HIDDEN_APPS_KEY = stringSetPreferencesKey("hidden_app_packages")
        val DRAWER_OPACITY_KEY = intPreferencesKey("drawer_opacity_percent")
        val GRID_COLUMNS_KEY = intPreferencesKey("grid_columns")
    }
}
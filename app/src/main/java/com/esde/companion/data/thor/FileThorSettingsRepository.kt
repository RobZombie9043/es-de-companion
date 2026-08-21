package com.esde.companion.data.thor

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.esde.companion.domain.model.HallSensorCalibration
import com.esde.companion.domain.model.VolumeSyncMode
import com.esde.companion.domain.repository.ThorSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore-backed [ThorSettingsRepository]. [context] is expected to be an application
 * context (see AppContainer), not an Activity context - same note as
 * [com.esde.companion.data.settings.FileOnboardingRepository].
 *
 * Same shape as OnboardingRepository/FileOnboardingRepository - one observe/set pair per
 * setting, now covering four features' worth - which already carries this same
 * TooManyFunctions violation as a baselined, pre-existing pattern; suppressed here rather
 * than baselined since this is new code, not pre-existing.
 */
@Suppress("TooManyFunctions")
class FileThorSettingsRepository(
    private val context: Context,
) : ThorSettingsRepository {
    override suspend fun setLidWakeGuardEnabled(enabled: Boolean) {
        context.thorSettingsDataStore.edit { it[LID_WAKE_GUARD_ENABLED_KEY] = enabled }
    }

    override fun observeLidWakeGuardEnabled(): Flow<Boolean> =
        context.thorSettingsDataStore.data.map { it[LID_WAKE_GUARD_ENABLED_KEY] ?: false }

    override suspend fun setHallSensorCalibration(calibration: HallSensorCalibration) {
        context.thorSettingsDataStore.edit { prefs ->
            prefs.putOrRemove(SENSOR_TYPE_KEY, calibration.sensorType)
            prefs[SENSOR_NAME_KEY] = calibration.sensorName
            prefs.putOrRemove(CLOSED_VALUE_KEY, calibration.closedValue)
            prefs.putOrRemove(OPEN_VALUE_KEY, calibration.openValue)
        }
    }

    private fun <T : Any> MutablePreferences.putOrRemove(
        key: Preferences.Key<T>,
        value: T?,
    ) {
        if (value != null) this[key] = value else remove(key)
    }

    override fun observeHallSensorCalibration(): Flow<HallSensorCalibration> =
        context.thorSettingsDataStore.data.map { prefs ->
            HallSensorCalibration(
                sensorType = prefs[SENSOR_TYPE_KEY],
                sensorName = prefs[SENSOR_NAME_KEY] ?: "",
                closedValue = prefs[CLOSED_VALUE_KEY],
                openValue = prefs[OPEN_VALUE_KEY],
            )
        }

    override suspend fun setAutoFpsEnabled(enabled: Boolean) {
        context.thorSettingsDataStore.edit { it[AUTO_FPS_ENABLED_KEY] = enabled }
    }

    override fun observeAutoFpsEnabled(): Flow<Boolean> {
        return context.thorSettingsDataStore.data.map { it[AUTO_FPS_ENABLED_KEY] ?: false }
    }

    override suspend fun setAutoFpsTriggerPackages(packages: Set<String>) {
        context.thorSettingsDataStore.edit { it[AUTO_FPS_TRIGGER_PACKAGES_KEY] = packages }
    }

    override fun observeAutoFpsTriggerPackages(): Flow<Set<String>> =
        context.thorSettingsDataStore.data.map { it[AUTO_FPS_TRIGGER_PACKAGES_KEY] ?: emptySet() }

    override suspend fun setTaskKillerEnabled(enabled: Boolean) {
        context.thorSettingsDataStore.edit { it[TASK_KILLER_ENABLED_KEY] = enabled }
    }

    override fun observeTaskKillerEnabled(): Flow<Boolean> {
        return context.thorSettingsDataStore.data.map { it[TASK_KILLER_ENABLED_KEY] ?: false }
    }

    override suspend fun setTaskKillerExcludedPackages(packages: Set<String>) {
        context.thorSettingsDataStore.edit { it[TASK_KILLER_EXCLUDED_PACKAGES_KEY] = packages }
    }

    // getStringSet only falls back to the given default when the key has never been written -
    // once the user saves any selection (even an empty one) from the exclude-apps picker, that
    // saved set takes over for good. Matches Asgard's TaskKillerPrefs.excludedPackages exactly.
    override fun observeTaskKillerExcludedPackages(): Flow<Set<String>> {
        return context.thorSettingsDataStore.data.map {
            it[TASK_KILLER_EXCLUDED_PACKAGES_KEY] ?: DEFAULT_TASK_KILLER_EXCLUDED_PACKAGES
        }
    }

    override suspend fun setVolumeSyncEnabled(enabled: Boolean) {
        context.thorSettingsDataStore.edit { it[VOLUME_SYNC_ENABLED_KEY] = enabled }
    }

    override fun observeVolumeSyncEnabled(): Flow<Boolean> {
        return context.thorSettingsDataStore.data.map { it[VOLUME_SYNC_ENABLED_KEY] ?: false }
    }

    override suspend fun setVolumeSyncMode(mode: VolumeSyncMode) {
        context.thorSettingsDataStore.edit { it[VOLUME_SYNC_MODE_KEY] = mode.name }
    }

    override fun observeVolumeSyncMode(): Flow<VolumeSyncMode> =
        context.thorSettingsDataStore.data.map { prefs ->
            // Falls back to Linked for both the unset case and any unrecognized stored value -
            // same reasoning as FileOnboardingRepository.observeThemePreference.
            prefs[VOLUME_SYNC_MODE_KEY]?.let { stored ->
                runCatching { VolumeSyncMode.valueOf(stored) }.getOrNull()
            } ?: VolumeSyncMode.Linked
        }

    private companion object {
        val LID_WAKE_GUARD_ENABLED_KEY = booleanPreferencesKey("lid_wake_guard_enabled")
        val SENSOR_TYPE_KEY = intPreferencesKey("hall_sensor_type")
        val SENSOR_NAME_KEY = stringPreferencesKey("hall_sensor_name")
        val CLOSED_VALUE_KEY = floatPreferencesKey("hall_sensor_closed_value")
        val OPEN_VALUE_KEY = floatPreferencesKey("hall_sensor_open_value")
        val AUTO_FPS_ENABLED_KEY = booleanPreferencesKey("auto_fps_enabled")
        val AUTO_FPS_TRIGGER_PACKAGES_KEY = stringSetPreferencesKey("auto_fps_trigger_packages")
        val TASK_KILLER_ENABLED_KEY = booleanPreferencesKey("task_killer_enabled")
        val TASK_KILLER_EXCLUDED_PACKAGES_KEY = stringSetPreferencesKey("task_killer_excluded_packages")
        val VOLUME_SYNC_ENABLED_KEY = booleanPreferencesKey("volume_sync_enabled")
        val VOLUME_SYNC_MODE_KEY = stringPreferencesKey("volume_sync_mode")

        // ES-DE and this app are this device's actual home/launcher pair (this app declares the
        // HOME category itself - see CLAUDE.md) - force-stopping either is equivalent to killing
        // the launcher, not "the game". Matches Asgard's TaskKillerPrefs.DEFAULT_EXCLUDED_PACKAGES.
        const val ESDE_PACKAGE_NAME = "org.es_de.frontend"
        val DEFAULT_TASK_KILLER_EXCLUDED_PACKAGES = setOf(ESDE_PACKAGE_NAME, "com.esde.companion")
    }
}

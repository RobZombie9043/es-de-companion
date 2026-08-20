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
import com.esde.companion.domain.repository.ThorSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore-backed [ThorSettingsRepository]. [context] is expected to be an application
 * context (see AppContainer), not an Activity context - same note as
 * [com.esde.companion.data.settings.FileOnboardingRepository].
 */
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

    private companion object {
        val LID_WAKE_GUARD_ENABLED_KEY = booleanPreferencesKey("lid_wake_guard_enabled")
        val SENSOR_TYPE_KEY = intPreferencesKey("hall_sensor_type")
        val SENSOR_NAME_KEY = stringPreferencesKey("hall_sensor_name")
        val CLOSED_VALUE_KEY = floatPreferencesKey("hall_sensor_closed_value")
        val OPEN_VALUE_KEY = floatPreferencesKey("hall_sensor_open_value")
        val AUTO_FPS_ENABLED_KEY = booleanPreferencesKey("auto_fps_enabled")
        val AUTO_FPS_TRIGGER_PACKAGES_KEY = stringSetPreferencesKey("auto_fps_trigger_packages")
    }
}

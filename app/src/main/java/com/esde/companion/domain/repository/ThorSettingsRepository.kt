package com.esde.companion.domain.repository

import com.esde.companion.domain.model.HallSensorCalibration
import kotlinx.coroutines.flow.Flow

/**
 * Persistence for Thor Settings (Ayn Thor-only: Lid Wake Guard + Auto FPS Mode - see
 * CLAUDE.md). Deliberately its own repository/DataStore file rather than folded into
 * [OnboardingRepository] - these settings are meaningless on any non-Thor device, unlike
 * everything else that repository covers.
 *
 * Device/OS capability checks (is this device a Thor, is the accessibility service granted,
 * is the privileged Settings-write service available) are NOT part of this interface - see
 * CLAUDE.md's note on why those follow the plain `data/` object pattern
 * ([com.esde.companion.data.storage.AllFilesAccessPermission]) instead.
 */
interface ThorSettingsRepository {
    suspend fun setLidWakeGuardEnabled(enabled: Boolean)

    fun observeLidWakeGuardEnabled(): Flow<Boolean>

    suspend fun setHallSensorCalibration(calibration: HallSensorCalibration)

    fun observeHallSensorCalibration(): Flow<HallSensorCalibration>

    suspend fun setAutoFpsEnabled(enabled: Boolean)

    fun observeAutoFpsEnabled(): Flow<Boolean>

    suspend fun setAutoFpsTriggerPackages(packages: Set<String>)

    fun observeAutoFpsTriggerPackages(): Flow<Set<String>>
}

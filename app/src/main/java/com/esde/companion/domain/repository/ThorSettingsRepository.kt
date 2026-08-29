package com.esde.companion.domain.repository

import com.esde.companion.domain.model.HallSensorCalibration
import com.esde.companion.domain.model.TaskKillerTarget
import com.esde.companion.domain.model.VolumeSyncMode
import kotlinx.coroutines.flow.Flow

/**
 * Persistence for Thor Settings (Ayn Thor-only: Lid Wake Guard, Auto FPS Mode, Task Killer,
 * Volume Sync - see CLAUDE.md). Deliberately its own repository/DataStore file rather than
 * folded into [OnboardingRepository] - these settings are meaningless on any non-Thor device,
 * unlike everything else that repository covers.
 *
 * Device/OS capability checks (is this device a Thor, is the accessibility service granted,
 * is the privileged Settings-write service available) are NOT part of this interface - see
 * CLAUDE.md's note on why those follow the plain `data/` object pattern
 * ([com.esde.companion.data.storage.AllFilesAccessPermission]) instead.
 *
 * Same shape as OnboardingRepository - one observe/set pair per setting - which already
 * carries this same TooManyFunctions violation as a baselined, pre-existing pattern;
 * suppressed here rather than baselined since this is new code, not pre-existing.
 */
@Suppress("TooManyFunctions")
interface ThorSettingsRepository {
    suspend fun setLidWakeGuardEnabled(enabled: Boolean)

    fun observeLidWakeGuardEnabled(): Flow<Boolean>

    suspend fun setHallSensorCalibration(calibration: HallSensorCalibration)

    fun observeHallSensorCalibration(): Flow<HallSensorCalibration>

    suspend fun setAutoFpsEnabled(enabled: Boolean)

    fun observeAutoFpsEnabled(): Flow<Boolean>

    suspend fun setAutoFpsTriggerPackages(packages: Set<String>)

    fun observeAutoFpsTriggerPackages(): Flow<Set<String>>

    suspend fun setTaskKillerEnabled(enabled: Boolean)

    fun observeTaskKillerEnabled(): Flow<Boolean>

    /** Never force-stopped even while Task Killer is on - defaults to this app's own package
     * plus ES-DE's, since both act as this device's home/launcher pair (see
     * `data/thor/FileThorSettingsRepository`'s kdoc for the exact default). */
    suspend fun setTaskKillerExcludedPackages(packages: Set<String>)

    fun observeTaskKillerExcludedPackages(): Flow<Set<String>>

    suspend fun setTaskKillerTarget(target: TaskKillerTarget)

    fun observeTaskKillerTarget(): Flow<TaskKillerTarget>

    suspend fun setVolumeSyncEnabled(enabled: Boolean)

    fun observeVolumeSyncEnabled(): Flow<Boolean>

    suspend fun setVolumeSyncMode(mode: VolumeSyncMode)

    fun observeVolumeSyncMode(): Flow<VolumeSyncMode>
}

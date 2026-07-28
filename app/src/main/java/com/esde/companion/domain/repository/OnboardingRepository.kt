package com.esde.companion.domain.repository

import com.esde.companion.domain.model.LogFolderValidation
import com.esde.companion.domain.model.MediaFolderValidation
import kotlinx.coroutines.flow.Flow

/**
 * Everything onboarding/settings needs: default path suggestions, folder validation,
 * and persistence of the user's chosen paths. How paths are actually persisted (DataStore,
 * in this app) and how folders are checked (plain File I/O) is entirely a data-layer
 * implementation detail.
 *
 * Re-enterable by design: the save/observe methods are not "first run only" - a Settings
 * screen reuses these same methods to change folders later.
 */
interface OnboardingRepository {

    fun defaultLogFolderPath(): String
    fun defaultMediaFolderPath(): String

    suspend fun validateLogFolder(path: String): LogFolderValidation
    suspend fun validateMediaFolder(path: String): MediaFolderValidation

    suspend fun saveLogFolderPath(path: String)
    suspend fun saveMediaFolderPath(path: String)

    fun observeLogFolderPath(): Flow<String?>
    fun observeMediaFolderPath(): Flow<String?>

    suspend fun markOnboardingComplete()
    fun observeOnboardingComplete(): Flow<Boolean>

    /**
     * Whether the on-screen state overlay (AppState/args/cover-image info drawn on top
     * of the main screen) is shown. Defaults to true when never explicitly set - this is
     * currently the only content the main screen has, so it should be visible out of the
     * box. Purely a display preference; has no bearing on the state pipeline itself.
     */
    suspend fun setOverlayEnabled(enabled: Boolean)
    fun observeOverlayEnabled(): Flow<Boolean>
}
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
 * Re-enterable by design: save*/observe* are not "first run only" - a Settings screen
* reuses these same methods to change folders later.
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
}
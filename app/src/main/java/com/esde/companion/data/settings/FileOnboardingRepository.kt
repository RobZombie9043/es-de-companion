package com.esde.companion.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.esde.companion.domain.model.LogFolderValidation
import com.esde.companion.domain.model.MediaFolderValidation
import com.esde.companion.domain.repository.OnboardingRepository
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * DataStore-backed [OnboardingRepository]. Folder existence/log-file checks are plain
 * File I/O (this app isn't Play-Store distributed, so MANAGE_EXTERNAL_STORAGE is granted
 * at runtime during onboarding rather than using SAF - see conversation history / the
 * folder-picker screens for why).
 *
 * [context] is expected to be an application context (see AppContainer), not an Activity
 * context - this repository can outlive any single Activity.
 */
class FileOnboardingRepository(
    private val context: Context,
) : OnboardingRepository {

    override fun defaultLogFolderPath(): String = DEFAULT_ESDE_ROOT

    override fun defaultMediaFolderPath(): String = "$DEFAULT_ESDE_ROOT/downloaded_media"

    override suspend fun validateLogFolder(path: String): LogFolderValidation =
        withContext(Dispatchers.IO) {
            val folder = File(path)
            if (!folder.isDirectory) {
                LogFolderValidation.FolderNotFound
            } else {
                val logFile = File(folder, "logs/es_log.txt")
                LogFolderValidation.FolderFound(logFileFound = logFile.isFile)
            }
        }

    override suspend fun validateMediaFolder(path: String): MediaFolderValidation =
        withContext(Dispatchers.IO) {
            if (File(path).isDirectory) {
                MediaFolderValidation.FolderFound
            } else {
                MediaFolderValidation.FolderNotFound
            }
        }

    override suspend fun saveLogFolderPath(path: String) {
        context.onboardingDataStore.edit { it[LOG_FOLDER_PATH_KEY] = path }
    }

    override suspend fun saveMediaFolderPath(path: String) {
        context.onboardingDataStore.edit { it[MEDIA_FOLDER_PATH_KEY] = path }
    }

    override fun observeLogFolderPath(): Flow<String?> =
        context.onboardingDataStore.data.map { it[LOG_FOLDER_PATH_KEY] }

    override fun observeMediaFolderPath(): Flow<String?> =
        context.onboardingDataStore.data.map { it[MEDIA_FOLDER_PATH_KEY] }

    override suspend fun markOnboardingComplete() {
        context.onboardingDataStore.edit { it[ONBOARDING_COMPLETE_KEY] = true }
    }

    override fun observeOnboardingComplete(): Flow<Boolean> =
        context.onboardingDataStore.data.map { it[ONBOARDING_COMPLETE_KEY] ?: false }

    private companion object {
        const val DEFAULT_ESDE_ROOT = "/storage/emulated/0/ES-DE"

        val LOG_FOLDER_PATH_KEY = stringPreferencesKey("log_folder_path")
        val MEDIA_FOLDER_PATH_KEY = stringPreferencesKey("media_folder_path")
        val ONBOARDING_COMPLETE_KEY = booleanPreferencesKey("onboarding_complete")
    }
}
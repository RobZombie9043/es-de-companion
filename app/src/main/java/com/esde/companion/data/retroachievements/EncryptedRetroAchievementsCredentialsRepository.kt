package com.esde.companion.data.retroachievements

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.esde.companion.domain.model.RetroAchievementsCredentials
import com.esde.companion.domain.repository.RetroAchievementsCredentialsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val PREFS_FILE_NAME = "retroachievements_credentials"
private const val KEY_USERNAME = "username"
private const val KEY_WEB_API_KEY = "web_api_key"

/**
 * Stores RetroAchievements credentials via `EncryptedSharedPreferences` (AES256_GCM master
 * key) - the app's first use of the `SharedPreferences` family instead of DataStore (see
 * CLAUDE.md's Tech Stack table), used for this one secret-shaped piece of persisted state
 * and nothing else. [setCredentials] is only ever called after
 * `ValidateRetroAchievementsCredentialsUseCase` has confirmed the credentials against the
 * live API - there is no "stored but unvalidated" state to represent here.
 *
 * [context] is expected to be an application context (see AppContainer), not an Activity
 * context - this repository can outlive any single Activity.
 *
 * Deliberately never constructed into `ExportConfigBackupUseCase`/`RestoreConfigBackupUseCase`
 * - see AppContainer's wiring notes for why simply never passing this repository to those
 * use cases is a structural guarantee against a plaintext credential leak.
 */
class EncryptedRetroAchievementsCredentialsRepository(
    private val context: Context,
) : RetroAchievementsCredentialsRepository {
    private val prefs: SharedPreferences by lazy { createEncryptedPrefs() }

    private val credentialsFlow: MutableStateFlow<RetroAchievementsCredentials?> by lazy {
        MutableStateFlow(readStoredCredentials())
    }

    override suspend fun setCredentials(credentials: RetroAchievementsCredentials) {
        prefs
            .edit()
            .putString(KEY_USERNAME, credentials.username)
            .putString(KEY_WEB_API_KEY, credentials.webApiKey)
            .apply()
        credentialsFlow.value = credentials
    }

    override suspend fun clearCredentials() {
        prefs.edit().clear().apply()
        credentialsFlow.value = null
    }

    override fun observeCredentials(): Flow<RetroAchievementsCredentials?> = credentialsFlow.asStateFlow()

    private fun createEncryptedPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private fun readStoredCredentials(): RetroAchievementsCredentials? {
        val username = prefs.getString(KEY_USERNAME, null) ?: return null
        return prefs.getString(KEY_WEB_API_KEY, null)?.let { RetroAchievementsCredentials(username, it) }
    }
}

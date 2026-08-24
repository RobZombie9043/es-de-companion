package com.esde.companion.domain.repository

import com.esde.companion.domain.model.RetroAchievementsCredentials
import kotlinx.coroutines.flow.Flow

/**
 * Persisted RetroAchievements sign-in credentials. Implemented via `EncryptedSharedPreferences`
 * rather than DataStore (see CLAUDE.md) - the app's one secret-shaped piece of persisted
 * state. Credentials are only ever written here after `ValidateRetroAchievementsCredentialsUseCase`
 * has confirmed them against the live API; there is no "stored but unvalidated" state to
 * represent. Deliberately excluded from Backup & Restore - see `AppContainer`'s wiring notes.
 */
interface RetroAchievementsCredentialsRepository {
    suspend fun setCredentials(credentials: RetroAchievementsCredentials)

    suspend fun clearCredentials()

    fun observeCredentials(): Flow<RetroAchievementsCredentials?>
}

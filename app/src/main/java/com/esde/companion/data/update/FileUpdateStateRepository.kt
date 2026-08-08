package com.esde.companion.data.update

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.esde.companion.domain.repository.UpdateStateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val LAST_SEEN_CHANGELOG_VERSION_CODE_KEY = intPreferencesKey("last_seen_changelog_version_code")

/**
 * DataStore-backed [UpdateStateRepository]. [context] is expected to be an application
 * context (see AppContainer), not an Activity context - same note as
 * [com.esde.companion.data.settings.FileOnboardingRepository].
 */
class FileUpdateStateRepository(
    private val context: Context,
) : UpdateStateRepository {
    override fun observeLastSeenChangelogVersionCode(): Flow<Int?> =
        context.updateSettingsDataStore.data.map { it[LAST_SEEN_CHANGELOG_VERSION_CODE_KEY] }

    override suspend fun setLastSeenChangelogVersionCode(versionCode: Int) {
        context.updateSettingsDataStore.edit { it[LAST_SEEN_CHANGELOG_VERSION_CODE_KEY] = versionCode }
    }
}

package com.esde.companion.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.esde.companion.domain.model.AppFolder
import com.esde.companion.domain.repository.AppFolderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * DataStore-backed [AppFolderRepository]. [context] is expected to be an application
 * context (see AppContainer), not an Activity context - same note as
 * [FileAppDrawerSettingsRepository]. Mirrors [FileDockSettingsRepository]'s
 * setDockApps/observeDockApps shape: one JSON-encoded list under one preference key.
 */
class FileAppFolderRepository(
    private val context: Context,
) : AppFolderRepository {
    override suspend fun setFolders(folders: List<AppFolder>) {
        context.appFolderSettingsDataStore.edit {
            it[FOLDERS_KEY] = Json.encodeToString(folders.map(AppFolder::toDto))
        }
    }

    override fun observeFolders(): Flow<List<AppFolder>> =
        context.appFolderSettingsDataStore.data.map { prefs ->
            prefs[FOLDERS_KEY]
                ?.let { Json.decodeFromString<List<AppFolderDto>>(it) }
                ?.map(AppFolderDto::toDomain)
                ?: emptyList()
        }

    private companion object {
        val FOLDERS_KEY = stringPreferencesKey("app_folders")
    }
}

@Serializable
private data class AppFolderDto(
    val id: String,
    val name: String,
    val memberPackageNames: Set<String>,
)

private fun AppFolder.toDto() = AppFolderDto(id = id, name = name, memberPackageNames = memberPackageNames)

private fun AppFolderDto.toDomain() = AppFolder(id = id, name = name, memberPackageNames = memberPackageNames)

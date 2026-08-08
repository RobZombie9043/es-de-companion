package com.esde.companion.domain.repository

import com.esde.companion.domain.model.AppFolder
import kotlinx.coroutines.flow.Flow

/**
 * Persisted App Drawer folders. Kept separate from [AppDrawerSettingsRepository] - that
 * interface is flat scalar/set prefs with no JSON involved, while folders are structured
 * data, same reasoning as [DockSettingsRepository] getting its own dedicated interface/
 * DataStore rather than being folded into an existing one.
 *
 * Persists whatever list it's given, without validating membership (e.g. an app listed in
 * two folders) - see [AppFolder]'s kdoc on where that invariant is actually enforced.
 */
interface AppFolderRepository {
    suspend fun setFolders(folders: List<AppFolder>)

    fun observeFolders(): Flow<List<AppFolder>>
}

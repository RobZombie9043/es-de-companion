package com.esde.companion.data.settings

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

/**
 * Single DataStore instance for App Drawer folders - kept separate from
 * [appDrawerSettingsDataStore] since folders are structured data with their own
 * serialization concerns (see AppFolderRepository).
 */
internal val Context.appFolderSettingsDataStore by preferencesDataStore(name = "app_folders")

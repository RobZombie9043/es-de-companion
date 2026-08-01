package com.esde.companion.data.settings

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

/**
 * Single DataStore instance for App Dock settings (enabled, max apps, size, opacity,
 * pinned apps) - kept separate from [appDrawerSettingsDataStore] since the dock is its
 * own settings surface (see DockSettingsRepository).
 */
internal val Context.dockSettingsDataStore by preferencesDataStore(name = "dock_settings")

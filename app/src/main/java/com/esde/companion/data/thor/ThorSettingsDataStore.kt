package com.esde.companion.data.thor

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

/**
 * Single DataStore instance for Thor Settings (Lid Wake Guard + Auto FPS Mode) - its own
 * DataStore file, not [com.esde.companion.data.settings.FileOnboardingRepository]'s, since
 * these settings are meaningless on any non-Thor device (see CLAUDE.md).
 */
internal val Context.thorSettingsDataStore by preferencesDataStore(name = "thor_settings")

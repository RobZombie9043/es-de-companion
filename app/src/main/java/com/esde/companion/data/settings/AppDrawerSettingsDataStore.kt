package com.esde.companion.data.settings

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

/**
 * Single DataStore instance for App Drawer display/visibility settings (hidden apps,
 * background opacity, grid columns) - kept separate from [onboardingDataStore] since
 * these are a distinct concern (see AppDrawerSettingsRepository), not onboarding state.
 */
internal val Context.appDrawerSettingsDataStore by preferencesDataStore(name = "app_drawer_settings")
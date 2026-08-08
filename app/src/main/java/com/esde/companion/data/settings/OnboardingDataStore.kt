package com.esde.companion.data.settings

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

/**
 * Single DataStore instance for onboarding/settings state (chosen folder paths, whether
 * onboarding has been completed). Kept as its own file/extension property, the standard
 * DataStore pattern, rather than a field inside FileOnboardingRepository, so there's only
 * ever one instance regardless of how many repository instances get constructed.
 */
internal val Context.onboardingDataStore by preferencesDataStore(name = "onboarding_settings")

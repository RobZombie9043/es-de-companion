package com.esde.companion.data.gameguides

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

/** Its own DataStore file, deliberately not shared with [gameGuideLibraryDataStore] - same
 * reasoning as `UserProgressCacheDataStore`'s kdoc: different concerns, different write
 * frequency (progress writes happen on a debounced scroll, the library index doesn't). */
internal val Context.gameGuideSettingsDataStore by preferencesDataStore(name = "game_guides_settings")

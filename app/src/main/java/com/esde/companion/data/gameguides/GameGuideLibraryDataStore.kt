package com.esde.companion.data.gameguides

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

internal val Context.gameGuideLibraryDataStore by preferencesDataStore(name = "game_guides_library")

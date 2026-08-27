package com.esde.companion.data.gamelist

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

internal val Context.gameLaunchOverrideDataStore by preferencesDataStore(name = "game_launch_overrides")

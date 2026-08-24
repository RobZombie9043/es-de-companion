package com.esde.companion.data.retroachievements

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

internal val Context.gameMatchOverrideDataStore by preferencesDataStore(name = "retroachievements_game_match_overrides")

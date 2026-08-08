package com.esde.companion.data.update

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

/** Single DataStore instance backing [FileUpdateStateRepository]. */
internal val Context.updateSettingsDataStore by preferencesDataStore(name = "update_settings")

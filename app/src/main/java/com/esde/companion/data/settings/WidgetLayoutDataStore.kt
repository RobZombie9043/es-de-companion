package com.esde.companion.data.settings

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

internal val Context.widgetLayoutDataStore by preferencesDataStore(name = "widget_layout")

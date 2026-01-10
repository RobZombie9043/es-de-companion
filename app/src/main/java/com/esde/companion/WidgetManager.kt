package com.esde.companion

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class WidgetManager(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveWidgets(widgets: List<OverlayWidget>) {
        val json = gson.toJson(widgets)
        prefs.edit().putString("widgets", json).apply()
    }

    fun loadWidgets(): List<OverlayWidget> {
        val json = prefs.getString("widgets", null) ?: return emptyList()
        val type = object : TypeToken<List<OverlayWidget>>() {}.type
        return gson.fromJson(json, type)
    }

    fun deleteWidget(widgetId: String) {
        val widgets = loadWidgets().filter { it.id != widgetId }
        saveWidgets(widgets)
    }
}
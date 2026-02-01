package com.esde.companion.managers

import android.content.Context
import android.content.SharedPreferences
import com.esde.companion.OverlayWidget
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class WidgetManager(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveWidgets(widgets: List<OverlayWidget>) {
        // ========== START: Convert to percentages before saving ==========
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // Convert all widgets to percentages
        widgets.forEach { widget ->
            widget.toPercentages(screenWidth, screenHeight)
        }
        // ========== END: Convert to percentages before saving ==========

        val json = gson.toJson(widgets)
        prefs.edit().putString("widgets", json).apply()
    }

    fun loadWidgets(): List<OverlayWidget> {
        val json = prefs.getString("widgets", null) ?: return emptyList()
        val type = object : TypeToken<List<OverlayWidget>>() {}.type
        val widgets: List<OverlayWidget> = gson.fromJson(json, type)

        // ========== START: Convert from percentages after loading ==========
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // Convert all widgets from percentages to current screen dimensions
        widgets.forEach { widget ->
            widget.fromPercentages(screenWidth, screenHeight)
        }
        // ========== END: Convert from percentages after loading ==========

        return widgets
    }

    fun deleteWidget(widgetId: String) {
        val widgets = loadWidgets().filter { it.id != widgetId }
        saveWidgets(widgets)
    }
}
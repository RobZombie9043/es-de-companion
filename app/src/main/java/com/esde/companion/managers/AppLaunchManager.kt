package com.esde.companion.managers

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages app launch preferences (top/bottom screen)
 */
class AppLaunchManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "app_launch_prefs"
        private const val KEY_PREFIX = "launch_position_"
        const val POSITION_TOP = "top"
        const val POSITION_BOTTOM = "bottom"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Get the launch position for a specific package
     * @return POSITION_TOP (default) or POSITION_BOTTOM
     */
    fun getLaunchPosition(packageName: String): String {
        return prefs.getString(KEY_PREFIX + packageName, POSITION_TOP) ?: POSITION_TOP
    }

    /**
     * Set the launch position for a specific package
     */
    fun setLaunchPosition(packageName: String, position: String) {
        prefs.edit()
            .putString(KEY_PREFIX + packageName, position)
            .apply()
    }

    /**
     * Check if app should launch on top screen (default)
     */
    fun shouldLaunchOnTop(packageName: String): Boolean {
        return getLaunchPosition(packageName) == POSITION_TOP
    }

    /**
     * Check if app should launch on bottom screen
     */
    fun shouldLaunchOnBottom(packageName: String): Boolean {
        return getLaunchPosition(packageName) == POSITION_BOTTOM
    }
}
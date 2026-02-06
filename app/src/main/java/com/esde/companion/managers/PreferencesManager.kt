package com.esde.companion.managers

import android.content.Context
import android.content.SharedPreferences
import com.esde.companion.data.AppConstants
import com.esde.companion.data.PreferenceKeys

/**
 * Centralized manager for all SharedPreferences access in ES-DE Companion.
 *
 * BENEFITS:
 * - Type-safe preference access (no magic strings)
 * - Single source of truth for defaults
 * - Easy to add migration logic
 * - Consistent preference handling
 *
 * USAGE:
 * ```
 * val prefsManager = PreferencesManager(context)
 *
 * // Read
 * val dimmingLevel = prefsManager.dimmingLevel
 *
 * // Write
 * prefsManager.dimmingLevel = 50
 * ```
 */
class PreferencesManager(context: Context) {

    companion object {
        // Animation preset constants (reference AppConstants for consistency)
        const val PRESET_ANIMATION_DURATION = AppConstants.Timing.DEFAULT_ANIMATION_DURATION
        const val PRESET_ANIMATION_SCALE = AppConstants.UI.DEFAULT_ANIMATION_SCALE
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PreferenceKeys.PREFS_NAME,
        Context.MODE_PRIVATE
    )

    // ========== PATH PROPERTIES ==========

    var mediaPath: String
        get() = prefs.getString(
            PreferenceKeys.KEY_MEDIA_PATH,
            PreferenceKeys.DEFAULT_MEDIA_PATH
        ) ?: PreferenceKeys.DEFAULT_MEDIA_PATH
        set(value) = prefs.edit()
            .putString(PreferenceKeys.KEY_MEDIA_PATH, value)
            .apply()

    var systemPath: String
        get() = prefs.getString(
            PreferenceKeys.KEY_SYSTEM_PATH,
            PreferenceKeys.DEFAULT_SYSTEM_PATH
        ) ?: PreferenceKeys.DEFAULT_SYSTEM_PATH
        set(value) = prefs.edit()
            .putString(PreferenceKeys.KEY_SYSTEM_PATH, value)
            .apply()

    var systemLogosPath: String
        get() = prefs.getString(
            PreferenceKeys.KEY_SYSTEM_LOGOS_PATH,
            PreferenceKeys.DEFAULT_SYSTEM_LOGOS_PATH
        ) ?: PreferenceKeys.DEFAULT_SYSTEM_LOGOS_PATH
        set(value) = prefs.edit()
            .putString(PreferenceKeys.KEY_SYSTEM_LOGOS_PATH, value)
            .apply()

    var customBackgroundPath: String
        get() = prefs.getString(
            PreferenceKeys.KEY_CUSTOM_BACKGROUND,
            PreferenceKeys.DEFAULT_CUSTOM_BACKGROUND
        ) ?: PreferenceKeys.DEFAULT_CUSTOM_BACKGROUND
        set(value) = prefs.edit()
            .putString(PreferenceKeys.KEY_CUSTOM_BACKGROUND, value)
            .apply()

    var scriptsPath: String
        get() = prefs.getString(
            PreferenceKeys.KEY_SCRIPTS_PATH,
            PreferenceKeys.DEFAULT_SCRIPTS_PATH
        ) ?: PreferenceKeys.DEFAULT_SCRIPTS_PATH
        set(value) = prefs.edit()
            .putString(PreferenceKeys.KEY_SCRIPTS_PATH, value)
            .apply()

    // ========== UI PROPERTIES ==========

    var dimmingLevel: Int
        get() = prefs.getInt(
            PreferenceKeys.KEY_DIMMING,
            PreferenceKeys.DEFAULT_DIMMING
        )
        set(value) = prefs.edit()
            .putInt(PreferenceKeys.KEY_DIMMING, value)
            .apply()

    var blurLevel: Int
        get() = prefs.getInt(
            PreferenceKeys.KEY_BLUR,
            PreferenceKeys.DEFAULT_BLUR
        )
        set(value) = prefs.edit()
            .putInt(PreferenceKeys.KEY_BLUR, value)
            .apply()

    var drawerTransparency: Int
        get() = prefs.getInt(
            PreferenceKeys.KEY_DRAWER_TRANSPARENCY,
            PreferenceKeys.DEFAULT_DRAWER_TRANSPARENCY
        )
        set(value) = prefs.edit()
            .putInt(PreferenceKeys.KEY_DRAWER_TRANSPARENCY, value)
            .apply()

    var columnCount: Int
        get() = prefs.getInt(
            PreferenceKeys.KEY_COLUMN_COUNT,
            PreferenceKeys.DEFAULT_COLUMN_COUNT
        )
        set(value) = prefs.edit()
            .putInt(PreferenceKeys.KEY_COLUMN_COUNT, value)
            .apply()

    // ========== WIDGET PROPERTIES ==========

    var widgetsLocked: Boolean
        get() = prefs.getBoolean(
            PreferenceKeys.KEY_WIDGETS_LOCKED,
            PreferenceKeys.DEFAULT_WIDGETS_LOCKED
        )
        set(value) = prefs.edit()
            .putBoolean(PreferenceKeys.KEY_WIDGETS_LOCKED, value)
            .apply()

    var snapToGrid: Boolean
        get() = prefs.getBoolean(
            PreferenceKeys.KEY_SNAP_TO_GRID,
            PreferenceKeys.DEFAULT_SNAP_TO_GRID
        )
        set(value) = prefs.edit()
            .putBoolean(PreferenceKeys.KEY_SNAP_TO_GRID, value)
            .apply()

    var showGrid: Boolean
        get() = prefs.getBoolean(
            PreferenceKeys.KEY_SHOW_GRID,
            PreferenceKeys.DEFAULT_SHOW_GRID
        )
        set(value) = prefs.edit()
            .putBoolean(PreferenceKeys.KEY_SHOW_GRID, value)
            .apply()

    // ========== VIDEO PROPERTIES ==========

    var videoEnabled: Boolean
        get() = prefs.getBoolean(
            PreferenceKeys.KEY_VIDEO_ENABLED,
            PreferenceKeys.DEFAULT_VIDEO_ENABLED
        )
        set(value) = prefs.edit()
            .putBoolean(PreferenceKeys.KEY_VIDEO_ENABLED, value)
            .apply()

    var videoDelay: Int
        get() = prefs.getInt(
            PreferenceKeys.KEY_VIDEO_DELAY,
            PreferenceKeys.DEFAULT_VIDEO_DELAY
        )
        set(value) = prefs.edit()
            .putInt(PreferenceKeys.KEY_VIDEO_DELAY, value)
            .apply()

    var videoAudioEnabled: Boolean
        get() = prefs.getBoolean(PreferenceKeys.KEY_VIDEO_AUDIO_ENABLED, PreferenceKeys.DEFAULT_VIDEO_AUDIO_ENABLED)
        set(value) = prefs.edit().putBoolean(PreferenceKeys.KEY_VIDEO_AUDIO_ENABLED, value).apply()

    // ========== MUSIC PROPERTIES ==========

    var musicEnabled: Boolean
        get() = prefs.getBoolean(
            PreferenceKeys.KEY_MUSIC_ENABLED,
            PreferenceKeys.DEFAULT_MUSIC_ENABLED
        )
        set(value) = prefs.edit()
            .putBoolean(PreferenceKeys.KEY_MUSIC_ENABLED, value)
            .apply()

    var musicPath: String
        get() = prefs.getString(
            PreferenceKeys.KEY_MUSIC_PATH,
            PreferenceKeys.DEFAULT_MUSIC_PATH
        ) ?: PreferenceKeys.DEFAULT_MUSIC_PATH
        set(value) = prefs.edit()
            .putString(PreferenceKeys.KEY_MUSIC_PATH, value)
            .apply()

    var musicVideoBehavior: String
        get() = prefs.getString(
            PreferenceKeys.KEY_MUSIC_VIDEO_BEHAVIOR,
            PreferenceKeys.DEFAULT_MUSIC_VIDEO_BEHAVIOR
        ) ?: PreferenceKeys.DEFAULT_MUSIC_VIDEO_BEHAVIOR
        set(value) = prefs.edit()
            .putString(PreferenceKeys.KEY_MUSIC_VIDEO_BEHAVIOR, value)
            .apply()

    var musicSongTitleEnabled: Boolean
        get() = prefs.getBoolean(
            PreferenceKeys.KEY_MUSIC_SONG_TITLE_ENABLED,
            PreferenceKeys.DEFAULT_MUSIC_SONG_TITLE_ENABLED
        )
        set(value) = prefs.edit()
            .putBoolean(PreferenceKeys.KEY_MUSIC_SONG_TITLE_ENABLED, value)
            .apply()

    var musicSongTitleDuration: Int
        get() = prefs.getInt(
            PreferenceKeys.KEY_MUSIC_SONG_TITLE_DURATION,
            PreferenceKeys.DEFAULT_MUSIC_SONG_TITLE_DURATION
        )
        set(value) = prefs.edit()
            .putInt(PreferenceKeys.KEY_MUSIC_SONG_TITLE_DURATION, value)
            .apply()

    var musicSongTitleOpacity: Int
        get() = prefs.getInt(
            PreferenceKeys.KEY_MUSIC_SONG_TITLE_OPACITY,
            PreferenceKeys.DEFAULT_MUSIC_SONG_TITLE_OPACITY
        )
        set(value) = prefs.edit()
            .putInt(PreferenceKeys.KEY_MUSIC_SONG_TITLE_OPACITY, value)
            .apply()

    // Music per-state toggles
    var musicSystemEnabled: Boolean
        get() = prefs.getBoolean(PreferenceKeys.KEY_MUSIC_SYSTEM_ENABLED, PreferenceKeys.DEFAULT_MUSIC_SYSTEM_ENABLED)
        set(value) = prefs.edit().putBoolean(PreferenceKeys.KEY_MUSIC_SYSTEM_ENABLED, value).apply()

    var musicGameEnabled: Boolean
        get() = prefs.getBoolean(PreferenceKeys.KEY_MUSIC_GAME_ENABLED, PreferenceKeys.DEFAULT_MUSIC_GAME_ENABLED)
        set(value) = prefs.edit().putBoolean(PreferenceKeys.KEY_MUSIC_GAME_ENABLED, value).apply()

    var musicScreensaverEnabled: Boolean
        get() = prefs.getBoolean(PreferenceKeys.KEY_MUSIC_SCREENSAVER_ENABLED, PreferenceKeys.DEFAULT_MUSIC_SCREENSAVER_ENABLED)
        set(value) = prefs.edit().putBoolean(PreferenceKeys.KEY_MUSIC_SCREENSAVER_ENABLED, value).apply()

    // ========== BEHAVIOR PROPERTIES ==========

    var gameLaunchBehavior: String
        get() = prefs.getString(
            PreferenceKeys.KEY_GAME_LAUNCH_BEHAVIOR,
            PreferenceKeys.DEFAULT_GAME_LAUNCH_BEHAVIOR
        ) ?: PreferenceKeys.DEFAULT_GAME_LAUNCH_BEHAVIOR
        set(value) = prefs.edit()
            .putString(PreferenceKeys.KEY_GAME_LAUNCH_BEHAVIOR, value)
            .apply()

    var screensaverBehavior: String
        get() = prefs.getString(
            PreferenceKeys.KEY_SCREENSAVER_BEHAVIOR,
            PreferenceKeys.DEFAULT_SCREENSAVER_BEHAVIOR
        ) ?: PreferenceKeys.DEFAULT_SCREENSAVER_BEHAVIOR
        set(value) = prefs.edit()
            .putString(PreferenceKeys.KEY_SCREENSAVER_BEHAVIOR, value)
            .apply()

    var systemViewBackgroundType: String
        get() = prefs.getString(
            PreferenceKeys.KEY_SYSTEM_VIEW_BACKGROUND_TYPE,
            PreferenceKeys.DEFAULT_SYSTEM_VIEW_BACKGROUND_TYPE
        ) ?: PreferenceKeys.DEFAULT_SYSTEM_VIEW_BACKGROUND_TYPE
        set(value) = prefs.edit()
            .putString(PreferenceKeys.KEY_SYSTEM_VIEW_BACKGROUND_TYPE, value)
            .apply()

    var gameViewBackgroundType: String
        get() = prefs.getString(
            PreferenceKeys.KEY_GAME_VIEW_BACKGROUND_TYPE,
            PreferenceKeys.DEFAULT_GAME_VIEW_BACKGROUND_TYPE
        ) ?: PreferenceKeys.DEFAULT_GAME_VIEW_BACKGROUND_TYPE
        set(value) = prefs.edit()
            .putString(PreferenceKeys.KEY_GAME_VIEW_BACKGROUND_TYPE, value)
            .apply()

    var systemBackgroundColor: Int
        get() = prefs.getInt(PreferenceKeys.KEY_SYSTEM_BACKGROUND_COLOR, PreferenceKeys.DEFAULT_SYSTEM_BACKGROUND_COLOR)
        set(value) = prefs.edit().putInt(PreferenceKeys.KEY_SYSTEM_BACKGROUND_COLOR, value).apply()

    var gameBackgroundColor: Int
        get() = prefs.getInt(PreferenceKeys.KEY_GAME_BACKGROUND_COLOR, PreferenceKeys.DEFAULT_GAME_BACKGROUND_COLOR)
        set(value) = prefs.edit().putInt(PreferenceKeys.KEY_GAME_BACKGROUND_COLOR, value).apply()

    var blackOverlayEnabled: Boolean
        get() = prefs.getBoolean(PreferenceKeys.KEY_BLACK_OVERLAY_ENABLED, PreferenceKeys.DEFAULT_BLACK_OVERLAY_ENABLED)
        set(value) = prefs.edit().putBoolean(PreferenceKeys.KEY_BLACK_OVERLAY_ENABLED, value).apply()

    // ========== LOGO PROPERTIES ==========

    var systemLogoSize: Int
        get() = prefs.getInt(
            PreferenceKeys.KEY_SYSTEM_LOGO_SIZE,
            PreferenceKeys.DEFAULT_SYSTEM_LOGO_SIZE
        )
        set(value) = prefs.edit()
            .putInt(PreferenceKeys.KEY_SYSTEM_LOGO_SIZE, value)
            .apply()

    var showSystemLogo: Boolean
        get() = prefs.getBoolean(
            PreferenceKeys.KEY_SHOW_SYSTEM_LOGO,
            PreferenceKeys.DEFAULT_SHOW_SYSTEM_LOGO
        )
        set(value) = prefs.edit()
            .putBoolean(PreferenceKeys.KEY_SHOW_SYSTEM_LOGO, value)
            .apply()

    var showGameLogo: Boolean
        get() = prefs.getBoolean(
            PreferenceKeys.KEY_SHOW_GAME_LOGO,
            PreferenceKeys.DEFAULT_SHOW_GAME_LOGO
        )
        set(value) = prefs.edit()
            .putBoolean(PreferenceKeys.KEY_SHOW_GAME_LOGO, value)
            .apply()

    // ========== ANIMATION PROPERTIES ==========

    var animationStyle: String
        get() = prefs.getString(
            PreferenceKeys.KEY_ANIMATION_STYLE,
            PreferenceKeys.DEFAULT_ANIMATION_STYLE
        ) ?: PreferenceKeys.DEFAULT_ANIMATION_STYLE
        set(value) = prefs.edit()
            .putString(PreferenceKeys.KEY_ANIMATION_STYLE, value)
            .apply()

    var animationDuration: Int
        get() = prefs.getInt(
            PreferenceKeys.KEY_ANIMATION_DURATION,
            PreferenceKeys.DEFAULT_ANIMATION_DURATION
        )
        set(value) = prefs.edit()
            .putInt(PreferenceKeys.KEY_ANIMATION_DURATION, value)
            .apply()

    var animationScale: Int
        get() = prefs.getInt(
            PreferenceKeys.KEY_ANIMATION_SCALE,
            PreferenceKeys.DEFAULT_ANIMATION_SCALE
        )
        set(value) = prefs.edit()
            .putInt(PreferenceKeys.KEY_ANIMATION_SCALE, value)
            .apply()

    // ========== SETUP/STATE PROPERTIES ==========

    var setupCompleted: Boolean
        get() = prefs.getBoolean(
            PreferenceKeys.KEY_SETUP_COMPLETED,
            PreferenceKeys.DEFAULT_SETUP_COMPLETED
        )
        set(value) = prefs.edit()
            .putBoolean(PreferenceKeys.KEY_SETUP_COMPLETED, value)
            .apply()

    var tutorialVersionShown: String
        get() = prefs.getString(
            PreferenceKeys.KEY_TUTORIAL_VERSION_SHOWN,
            PreferenceKeys.DEFAULT_TUTORIAL_VERSION_SHOWN
        ) ?: PreferenceKeys.DEFAULT_TUTORIAL_VERSION_SHOWN
        set(value) = prefs.edit()
            .putString(PreferenceKeys.KEY_TUTORIAL_VERSION_SHOWN, value)
            .apply()

    var settingsHintCount: Int
        get() = prefs.getInt(
            PreferenceKeys.KEY_SETTINGS_HINT_COUNT,
            PreferenceKeys.DEFAULT_SETTINGS_HINT_COUNT
        )
        set(value) = prefs.edit()
            .putInt(PreferenceKeys.KEY_SETTINGS_HINT_COUNT, value)
            .apply()

    var hiddenApps: Set<String>
        get() = prefs.getStringSet(
            PreferenceKeys.KEY_HIDDEN_APPS,
            emptySet()
        ) ?: emptySet()
        set(value) = prefs.edit()
            .putStringSet(PreferenceKeys.KEY_HIDDEN_APPS, value)
            .apply()

    var defaultWidgetsCreated: Boolean
        get() = prefs.getBoolean(
            PreferenceKeys.KEY_DEFAULT_WIDGETS_CREATED,
            PreferenceKeys.DEFAULT_DEFAULT_WIDGETS_CREATED
        )
        set(value) = prefs.edit()
            .putBoolean(PreferenceKeys.KEY_DEFAULT_WIDGETS_CREATED, value)
            .apply()

    var widgetTutorialShown: Boolean
        get() = prefs.getBoolean(
            PreferenceKeys.KEY_WIDGET_TUTORIAL_SHOWN,
            PreferenceKeys.DEFAULT_WIDGET_TUTORIAL_SHOWN
        )
        set(value) = prefs.edit()
            .putBoolean(PreferenceKeys.KEY_WIDGET_TUTORIAL_SHOWN, value)
            .apply()

    var widgetTutorialDontShowAuto: Boolean
        get() = prefs.getBoolean(
            PreferenceKeys.KEY_WIDGET_TUTORIAL_DONT_SHOW_AUTO,
            PreferenceKeys.DEFAULT_WIDGET_TUTORIAL_DONT_SHOW_AUTO
        )
        set(value) = prefs.edit()
            .putBoolean(PreferenceKeys.KEY_WIDGET_TUTORIAL_DONT_SHOW_AUTO, value)
            .apply()

    // ========== CACHE PROPERTIES ==========

    var lastImageCacheVersion: Int
        get() = prefs.getInt(
            PreferenceKeys.KEY_LAST_IMAGE_CACHE_VERSION,
            PreferenceKeys.DEFAULT_LAST_IMAGE_CACHE_VERSION
        )
        set(value) = prefs.edit()
            .putInt(PreferenceKeys.KEY_LAST_IMAGE_CACHE_VERSION, value)
            .apply()

    // ========== HELPER METHODS ==========

    /**
     * Clear all preferences (useful for testing or factory reset).
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }

    /**
     * Check if a preference exists.
     */
    fun contains(key: String): Boolean {
        return prefs.contains(key)
    }

    /**
     * Get raw SharedPreferences instance (for legacy compatibility during migration).
     *
     * DEPRECATION WARNING: This should only be used during migration.
     * New code should use the type-safe properties above.
     */
    @Deprecated(
        message = "Use type-safe properties instead",
        replaceWith = ReplaceWith("prefsManager.propertyName")
    )
    fun getRawPreferences(): SharedPreferences {
        return prefs
    }
}
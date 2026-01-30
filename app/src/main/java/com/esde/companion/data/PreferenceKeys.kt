package com.esde.companion.data

/**
 * Centralized preference keys and default values for ES-DE Companion.
 *
 * MIGRATION GUIDE:
 * - Old: prefs.getString("media_path", "/storage/emulated/0/ES-DE/downloaded_media")
 * - New: prefsManager.mediaPath
 *
 * This ensures:
 * - No typos in preference keys
 * - Consistent default values
 * - Type-safe access
 * - Easy to add new preferences
 */
object PreferenceKeys {
    // Shared preferences file name
    const val PREFS_NAME = "ESDESecondScreenPrefs"

    // ========== PATH PREFERENCES ==========
    const val KEY_MEDIA_PATH = "media_path"
    const val DEFAULT_MEDIA_PATH = "/storage/emulated/0/ES-DE/downloaded_media"

    const val KEY_SYSTEM_PATH = "system_path"
    const val DEFAULT_SYSTEM_PATH = "/storage/emulated/0/ES-DE/downloaded_media"

    const val KEY_SYSTEM_LOGOS_PATH = "system_logos_path"
    const val DEFAULT_SYSTEM_LOGOS_PATH = "/storage/emulated/0/ES-DE Companion/system_images"

    const val KEY_CUSTOM_BACKGROUND = "custom_background_uri"
    const val DEFAULT_CUSTOM_BACKGROUND = ""

    const val KEY_SCRIPTS_PATH = "scripts_path"
    const val DEFAULT_SCRIPTS_PATH = "/storage/emulated/0/ES-DE/scripts"

    const val KEY_LOGS_PATH = "logs_path"
    const val DEFAULT_LOGS_PATH = "/storage/emulated/0/ES-DE Companion/logs"

    // ========== UI PREFERENCES ==========
    const val KEY_DIMMING = "dimming"
    const val DEFAULT_DIMMING = 25

    const val KEY_BLUR = "blur"
    const val DEFAULT_BLUR = 0

    const val KEY_DRAWER_TRANSPARENCY = "drawer_transparency"
    const val DEFAULT_DRAWER_TRANSPARENCY = 70

    const val KEY_COLUMN_COUNT = "column_count"
    const val DEFAULT_COLUMN_COUNT = 4

    // ========== WIDGET PREFERENCES ==========
    const val KEY_WIDGETS_LOCKED = "widgets_locked"
    const val DEFAULT_WIDGETS_LOCKED = true

    const val KEY_SNAP_TO_GRID = "snap_to_grid"
    const val DEFAULT_SNAP_TO_GRID = true

    const val KEY_SHOW_GRID = "show_grid"
    const val DEFAULT_SHOW_GRID = false

    // ========== VIDEO PREFERENCES ==========
    const val KEY_VIDEO_ENABLED = "video_enabled"
    const val DEFAULT_VIDEO_ENABLED = false

    const val KEY_VIDEO_DELAY = "video_delay"
    const val DEFAULT_VIDEO_DELAY = 3
    const val KEY_VIDEO_AUDIO_ENABLED = "video_audio_enabled"
    const val DEFAULT_VIDEO_AUDIO_ENABLED = false

    // ========== MUSIC PREFERENCES ==========
    const val KEY_MUSIC_ENABLED = "music.enabled"
    const val DEFAULT_MUSIC_ENABLED = false

    const val KEY_MUSIC_PATH = "music_path"
    const val DEFAULT_MUSIC_PATH = "/storage/emulated/0/ES-DE Companion/music"

    const val KEY_MUSIC_SYSTEM_VIEW = "music.system_view"
    const val DEFAULT_MUSIC_SYSTEM_VIEW = true

    const val KEY_MUSIC_GAME_VIEW = "music.game_view"
    const val DEFAULT_MUSIC_GAME_VIEW = true

    const val KEY_MUSIC_SCREENSAVER = "music.screensaver"
    const val DEFAULT_MUSIC_SCREENSAVER = false

    const val KEY_MUSIC_VIDEO_BEHAVIOR = "music_video_behavior"
    const val DEFAULT_MUSIC_VIDEO_BEHAVIOR = "duck"

    const val KEY_MUSIC_SONG_TITLE_ENABLED = "music.song_title_enabled"
    const val DEFAULT_MUSIC_SONG_TITLE_ENABLED = false

    const val KEY_MUSIC_SONG_TITLE_DURATION = "music.song_title_duration"
    const val DEFAULT_MUSIC_SONG_TITLE_DURATION = 5

    const val KEY_MUSIC_SONG_TITLE_OPACITY = "music.song_title_opacity"
    const val DEFAULT_MUSIC_SONG_TITLE_OPACITY = 70

    const val KEY_MUSIC_SYSTEM_ENABLED = "music.system_enabled"
    const val DEFAULT_MUSIC_SYSTEM_ENABLED = true

    const val KEY_MUSIC_GAME_ENABLED = "music.game_enabled"
    const val DEFAULT_MUSIC_GAME_ENABLED = true

    const val KEY_MUSIC_SCREENSAVER_ENABLED = "music.screensaver_enabled"
    const val DEFAULT_MUSIC_SCREENSAVER_ENABLED = false

    // ========== BEHAVIOR PREFERENCES ==========
    const val KEY_GAME_LAUNCH_BEHAVIOR = "game_launch_behavior"
    const val DEFAULT_GAME_LAUNCH_BEHAVIOR = "game_image"

    const val KEY_SCREENSAVER_BEHAVIOR = "screensaver_behavior"
    const val DEFAULT_SCREENSAVER_BEHAVIOR = "game_image"

    const val DEFAULT_IMAGE_PREFERENCE = "fanart"

    const val KEY_SYSTEM_VIEW_BACKGROUND_TYPE = "system_image_preference"
    const val DEFAULT_SYSTEM_VIEW_BACKGROUND_TYPE = "fanart"

    const val KEY_GAME_VIEW_BACKGROUND_TYPE = "game_image_preference"
    const val DEFAULT_GAME_VIEW_BACKGROUND_TYPE = "fanart"

    const val KEY_SYSTEM_BACKGROUND_COLOR = "system_background_color"
    const val DEFAULT_SYSTEM_BACKGROUND_COLOR = 0xFF1A1A1A.toInt()  // #1A1A1A

    const val KEY_GAME_BACKGROUND_COLOR = "game_background_color"
    const val DEFAULT_GAME_BACKGROUND_COLOR = 0xFF1A1A1A.toInt()  // #1A1A1A

    const val KEY_BLACK_OVERLAY_ENABLED = "black_overlay_enabled"
    const val DEFAULT_BLACK_OVERLAY_ENABLED = false

    // ========== LOGO PREFERENCES ==========
    const val KEY_SYSTEM_LOGO_SIZE = "system_logo_size"
    const val DEFAULT_SYSTEM_LOGO_SIZE = 50

    const val KEY_SHOW_SYSTEM_LOGO = "show_system_logo"
    const val DEFAULT_SHOW_SYSTEM_LOGO = true

    const val KEY_SHOW_GAME_LOGO = "show_game_logo"
    const val DEFAULT_SHOW_GAME_LOGO = false

    // ========== ANIMATION PREFERENCES ==========
    const val KEY_ANIMATION_STYLE = "animation_style"
    const val DEFAULT_ANIMATION_STYLE = "scale_fade"

    const val KEY_ANIMATION_DURATION = "animation_duration"
    const val DEFAULT_ANIMATION_DURATION = 250

    const val KEY_ANIMATION_SCALE = "animation_scale"
    const val DEFAULT_ANIMATION_SCALE = 95

    // ========== SETUP/STATE PREFERENCES ==========
    const val KEY_SETUP_COMPLETED = "setup_completed"
    const val DEFAULT_SETUP_COMPLETED = false

    const val KEY_TUTORIAL_VERSION_SHOWN = "tutorial_version_shown"
    const val DEFAULT_TUTORIAL_VERSION_SHOWN = ""

    const val KEY_SETTINGS_HINT_COUNT = "settings_hint_count"
    const val DEFAULT_SETTINGS_HINT_COUNT = 0

    const val KEY_HIDDEN_APPS = "hidden_apps"
    // Default is empty set, handled specially in PreferencesManager

    const val KEY_DEFAULT_WIDGETS_CREATED = "default_widgets_created"
    const val DEFAULT_DEFAULT_WIDGETS_CREATED = false

    const val KEY_WIDGET_TUTORIAL_SHOWN = "widget_tutorial_shown"
    const val DEFAULT_WIDGET_TUTORIAL_SHOWN = false

    const val KEY_WIDGET_TUTORIAL_DONT_SHOW_AUTO = "widget_tutorial_dont_show_auto"
    const val DEFAULT_WIDGET_TUTORIAL_DONT_SHOW_AUTO = false

    // ========== CACHE KEYS ==========
    const val KEY_LAST_IMAGE_CACHE_VERSION = "last_image_cache_version"
    const val DEFAULT_LAST_IMAGE_CACHE_VERSION = 1

}
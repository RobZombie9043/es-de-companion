package com.esde.companion.data

/**
 * Centralized constants for ES-DE Companion.
 *
 * All magic numbers, timing values, file paths, and configuration constants
 * are defined here for easy maintenance, testing, and documentation.
 *
 * ORGANIZATION:
 * - Paths: File system paths and log file names
 * - Timing: All timing values (delays, debouncing, timeouts)
 * - UI: User interface constants (grid size, defaults, limits)
 * - FileExtensions: Supported file formats
 * - Cache: Cache versioning
 * - Animation: Animation presets and limits
 */
object AppConstants {

    // ========== PATH CONSTANTS ==========

    /**
     * File system paths and log file names.
     *
     * NOTE: Default paths are also defined in PreferenceKeys for user-configurable
     * locations, but these constants provide the canonical values.
     */
    object Paths {
        // Directory names (relative to storage root) - these remain const
        const val SCRIPTS_DIR = "ES-DE/scripts"
        const val MEDIA_DIR = "ES-DE/downloaded_media"
        const val SYSTEM_IMAGES_DIR = "ES-DE Companion/system_images"
        const val SYSTEM_LOGOS_DIR = "ES-DE Companion/system_logos"
        const val LOGS_DIR = "ES-DE Companion/logs"

        // Dynamic storage root that works across devices
        val DEFAULT_STORAGE_ROOT: String
            get() = android.os.Environment.getExternalStorageDirectory().absolutePath

        // Full default paths (computed dynamically)
        val DEFAULT_SCRIPTS_PATH: String
            get() = "$DEFAULT_STORAGE_ROOT/$SCRIPTS_DIR"

        val DEFAULT_MEDIA_PATH: String
            get() = "$DEFAULT_STORAGE_ROOT/$MEDIA_DIR"

        val DEFAULT_SYSTEM_IMAGES_PATH: String
            get() = "$DEFAULT_STORAGE_ROOT/$SYSTEM_IMAGES_DIR"

        val DEFAULT_SYSTEM_LOGOS_PATH: String
            get() = "$DEFAULT_STORAGE_ROOT/$SYSTEM_LOGOS_DIR"

        // Logs directory (FIXED - not user-configurable due to FileObserver limitations)
        val DEFAULT_LOGS_PATH: String
            get() = "$DEFAULT_STORAGE_ROOT/$LOGS_DIR"

        // Log file names (written by ES-DE scripts, read by app)
        const val SYSTEM_NAME_LOG = "esde_system_name.txt"
        const val GAME_FILENAME_LOG = "esde_game_filename.txt"
        const val GAME_NAME_LOG = "esde_game_name.txt"
        const val GAME_SYSTEM_LOG = "esde_game_system.txt"
        const val GAME_START_FILENAME_LOG = "esde_gamestart_filename.txt"
        const val GAME_START_NAME_LOG = "esde_gamestart_name.txt"
        const val GAME_START_SYSTEM_LOG = "esde_gamestart_system.txt"
        const val GAME_END_FILENAME_LOG = "esde_gameend_filename.txt"
        const val GAME_END_NAME_LOG = "esde_gameend_name.txt"
        const val GAME_END_SYSTEM_LOG = "esde_gameend_system.txt"
        const val SCREENSAVER_START_LOG = "esde_screensaver_start.txt"
        const val SCREENSAVER_END_LOG = "esde_screensaver_end.txt"
        const val SCREENSAVER_GAME_FILENAME_LOG = "esde_screensavergameselect_filename.txt"
        const val SCREENSAVER_GAME_NAME_LOG = "esde_screensavergameselect_name.txt"
        const val SCREENSAVER_GAME_SYSTEM_LOG = "esde_screensavergameselect_system.txt"

        // Media subdirectories
        const val MEDIA_FANART = "fanart"
        const val MEDIA_SCREENSHOTS = "screenshots"
        const val MEDIA_MARQUEES = "marquees"
        const val MEDIA_COVERS = "covers"
        const val MEDIA_3DBOXES = "3dboxes"
        const val MEDIA_MIXIMAGES = "miximages"
        const val MEDIA_BACKCOVERS = "backcovers"
        const val MEDIA_PHYSICALMEDIA = "physicalmedia"
        const val MEDIA_TITLESCREENS = "titlescreens"
        const val MEDIA_VIDEOS = "videos"

        // Music subdirectory structure
        const val MUSIC_SYSTEMS_SUBDIR = "systems"
    }

    // ========== TIMING CONSTANTS ==========

    /**
     * All timing-related values in milliseconds.
     *
     * CATEGORIES:
     * - Video: Video playback delays
     * - Scrolling: Debounce thresholds and delays
     * - Interaction: User interaction timeouts
     * - Animation: Animation durations
     * - Verification: Background task timeouts
     */
    object Timing {
        // Video delays (user-configurable multiplier applied to these)
        const val VIDEO_DELAY_MULTIPLIER = 500L  // Multiply slider value (0-10) by this

        // System scrolling debounce
        const val SYSTEM_FAST_SCROLL_THRESHOLD = 250L  // If faster than this, it's "fast scrolling"
        const val SYSTEM_FAST_SCROLL_DELAY = 50L      // Delay for fast scrolling
        const val SYSTEM_SLOW_SCROLL_DELAY = 0L      // Delay for slow scrolling

        // Game scrolling (no debounce for instant response)
        const val GAME_FAST_SCROLL_THRESHOLD = 250L
        const val GAME_FAST_SCROLL_DELAY = 0L          // Instant
        const val GAME_SLOW_SCROLL_DELAY = 0L          // Instant

        // Event debouncing (prevent duplicate events)
        const val GAME_EVENT_DEBOUNCE = 500L            // Filter game-select during game-start/end

        // Double-tap detection
        const val DOUBLE_TAP_MIN_INTERVAL = 100L        // Minimum time between taps (prevents accidents)
        // DOUBLE_TAP_TIMEOUT uses ViewConfiguration.getDoubleTapTimeout() (~300ms)

        // UI delays
        const val WIZARD_DELAY = 500L                   // Delay before starting wizard
        const val TUTORIAL_DELAY = 3000L                // Delay before showing tutorial
        const val SETTINGS_DELAY = 1000L                // Delay before launching settings

        // Background task timeouts
        const val SCRIPT_VERIFICATION_TIMEOUT = 15000L  // 15 seconds for script validation

        // Animation durations
        const val FADE_ANIMATION_DURATION = 300L        // Standard fade duration
        const val SLIDE_ANIMATION_DURATION = 200L       // Drawer slide duration

        // Default animation values (user-configurable)
        const val DEFAULT_ANIMATION_DURATION = 300      // milliseconds
        const val ANIMATION_DURATION_MIN = 100          // Minimum slider value
        const val ANIMATION_DURATION_MAX = 500          // Maximum slider value
        const val ANIMATION_DURATION_STEP = 10          // Slider increment

        // Music timing (if ENABLE_BACKGROUND_MUSIC)
        const val MUSIC_CROSS_FADE_DURATION = 300L
        const val MUSIC_DUCK_FADE_DURATION = 300L
        const val SONG_TITLE_STEP_SECONDS = 2

        // SD card mount retry delays (progressive backoff: 1s, 2s, 3s, 4s, 5s)
        const val SD_MOUNT_RETRY_BASE_DELAY = 1000L    // 1 second
        const val SD_MOUNT_RETRY_MAX_DELAY = 5000L     // 5 seconds max
    }

    // ========== UI CONSTANTS ==========

    /**
     * User interface constants.
     *
     * CATEGORIES:
     * - Grid: Widget grid system
     * - Drawer: App drawer configuration
     * - Limits: Slider and input limits
     * - Defaults: Default UI values
     */
    object UI {
        // Widget grid system
        const val GRID_SIZE = 20f                       // Grid cell size in dp
        const val DEFAULT_WIDGET_WIDTH_PERCENT = 0.75f  // 75% of screen width
        const val DEFAULT_WIDGET_HEIGHT_PERCENT = 0.35f // 35% of screen height

        // App drawer
        const val DEFAULT_COLUMN_COUNT = 4
        const val MIN_COLUMN_COUNT = 2
        const val MAX_COLUMN_COUNT = 8

        // Visual effects limits
        const val DEFAULT_DIMMING = 25
        const val DEFAULT_BLUR = 0

        const val DEFAULT_DRAWER_TRANSPARENCY = 70

        // Animation limits
        const val ANIMATION_SCALE_MIN = 85
        const val ANIMATION_SCALE_MAX = 100
        const val DEFAULT_ANIMATION_SCALE = 95          // percentage (95% = 0.95f)

        // Video settings limits
        const val VIDEO_DELAY_MIN = 0                   // Slider minimum
        const val VIDEO_DELAY_MAX = 10                  // Slider maximum (0-5 seconds in 0.5s increments)
        const val DEFAULT_VIDEO_DELAY = 4               // 2 seconds (4 * 0.5)

        // Music volume ducking
        const val MUSIC_NORMAL_VOLUME = 1.0f
        const val MUSIC_DUCKED_VOLUME = 0.2f            // 20% when video playing
    }

    // ========== FILE EXTENSIONS ==========

    /**
     * Supported file formats for media types.
     */
    object FileExtensions {
        val VIDEO = listOf("mp4", "mkv", "avi", "wmv", "mov", "webm")
        val IMAGE = listOf("jpg", "jpeg", "png", "webp", "gif")
        val AUDIO = listOf("mp3", "ogg", "flac", "m4a", "wav", "aac")

    }

    // ========== SCRIPT REQUIREMENTS ==========

    /**
     * Script validation requirements.
     */
    object Scripts {
        const val TOTAL_SCRIPT_COUNT = 7
        const val EXPECTED_SHEBANG = "#!/bin/sh"
        const val OLD_SHEBANG = "#!/bin/bash"          // Outdated format

        // Script file names
        const val GAME_SELECT_SCRIPT = "esdecompanion-game-select.sh"
        const val SYSTEM_SELECT_SCRIPT = "esdecompanion-system-select.sh"
        const val GAME_START_SCRIPT = "esdecompanion-game-start.sh"
        const val GAME_END_SCRIPT = "esdecompanion-game-end.sh"
        const val SCREENSAVER_START_SCRIPT = "esdecompanion-screensaver-start.sh"
        const val SCREENSAVER_END_SCRIPT = "esdecompanion-screensaver-end.sh"
        const val SCREENSAVER_GAME_SELECT_SCRIPT = "esdecompanion-screensaver-game-select.sh"
    }

    // ========== SHARED PREFERENCES ==========

    /**
     * SharedPreferences file name.
     * Note: Preference keys are in PreferenceKeys.kt for user-configurable values.
     */
    object Preferences {
        const val PREFS_NAME = "ESDESecondScreenPrefs"
    }
}
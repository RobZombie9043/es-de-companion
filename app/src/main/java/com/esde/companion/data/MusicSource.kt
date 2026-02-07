package com.esde.companion.data

/**
 * ═══════════════════════════════════════════════════════════
 * MUSIC SOURCE
 * ═══════════════════════════════════════════════════════════
 * Represents the source of music to play.
 *
 * This sealed class ensures type-safe music source selection
 * and makes it easy to determine when music should cross-fade
 * (different sources) vs. continue (same source).
 * ═══════════════════════════════════════════════════════════
 */
sealed class MusicSource {
    /**
     * Generic/frontend music from the base music folder.
     * Used as fallback when system-specific folder doesn't exist, and for screensavers
     *
     * Path: /storage/emulated/0/ES-DE Companion/music/
     */
    object Generic : MusicSource() {
        override fun toString(): String = "Generic"
        override fun equals(other: Any?): Boolean = other is Generic
        override fun hashCode(): Int = 0
    }

    /**
     * System-specific music from a system subfolder.
     * Used for: System/game browsing (automatically falls back to Generic if folder doesn't exist)
     *
     * Path: /storage/emulated/0/ES-DE Companion/music/systems/{systemName}/
     *
     * @param systemName The ES-DE system name (e.g., "snes", "arcade")
     */
    data class System(val systemName: String) : MusicSource() {
        override fun toString(): String = "System($systemName)"
    }

    /**
     * Get the folder path for this music source.
     *
     * @param basePath The base music folder path
     * @return The full path to the music folder for this source
     */
    fun getPath(basePath: String): String {
        return when (this) {
            is Generic -> basePath
            is System -> "$basePath/${AppConstants.Paths.MUSIC_SYSTEMS_SUBDIR}/$systemName"
        }
    }
}
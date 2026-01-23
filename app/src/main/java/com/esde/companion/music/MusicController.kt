package com.esde.companion.music

import com.esde.companion.AppState

/**
 * ═══════════════════════════════════════════════════════════
 * MUSIC CONTROLLER INTERFACE
 * ═══════════════════════════════════════════════════════════
 * Defines the contract for music playback control in ES-DE Companion.
 *
 * This interface allows for:
 * - Clean separation between music logic and MainActivity
 * - Easy testing with mock implementations
 * - Safe null handling when music is disabled
 * - Clear API surface for music features
 * ═══════════════════════════════════════════════════════════
 */
interface MusicController {

    /**
     * Called when app state changes (system/game browsing, playing, screensaver).
     *
     * The music system will automatically:
     * - Start/stop music based on state and settings
     * - Cross-fade between different music sources
     * - Continue same track when appropriate
     *
     * @param newState The new AppState
     */
    fun onStateChanged(newState: AppState)

    /**
     * Called when a Companion video starts playing.
     *
     * Behavior depends on "music_video_behavior" setting:
     * - "continue": No change (music stays at 100%)
     * - "duck": Fade music to 20% volume
     * - "pause": Pause music playback
     */
    fun onVideoStarted()

    /**
     * Called when a Companion video stops playing.
     *
     * Restores music to previous state:
     * - If ducked: Fade back to 100% volume
     * - If paused: Resume playback
     * - If at 100%: No change
     */
    fun onVideoEnded()

    /**
     * Called when activity becomes visible (onStart).
     *
     * Music will resume if it was playing before visibility was lost.
     */
    fun onActivityVisible()

    /**
     * Called when activity becomes invisible (onStop, device sleep).
     *
     * Music will pause and track that it should resume when visible again.
     */
    fun onActivityInvisible()

    /**
     * Release all music resources.
     *
     * Called in MainActivity.onDestroy() to clean up:
     * - MediaPlayer instances
     * - Handler callbacks
     * - Any pending operations
     */
    fun release()

}
package com.esde.companion.music

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import com.esde.companion.AppState
import java.io.File

/**
 * ═══════════════════════════════════════════════════════════
 * MUSIC MANAGER
 * ═══════════════════════════════════════════════════════════
 * Manages background music playback for ES-DE Companion.
 *
 * FEATURES:
 * - State-based music playback (system/game/screensaver)
 * - System-specific music support
 * - Cross-fade transitions between sources
 * - Video ducking/pausing
 * - Playlist management with looping
 *
 * REMOVAL NOTE:
 * This entire file can be deleted if ES-DE adds native music.
 * See FeatureFlags.kt for complete removal instructions.
 * ═══════════════════════════════════════════════════════════
 */
class MusicManager(
    private val context: Context,
    private val prefs: SharedPreferences
) : MusicController {

    companion object {
        private const val TAG = "MusicManager"

        // Music folder paths
        private const val BASE_MUSIC_PATH = "/storage/emulated/0/ES-DE Companion/music"

        // Supported audio formats
        private val AUDIO_EXTENSIONS = listOf("mp3", "ogg", "flac", "m4a", "wav", "aac")

        // Animation durations
        private const val CROSS_FADE_DURATION = 300L // milliseconds
        private const val DUCK_FADE_DURATION = 300L

        // Volume levels
        private const val NORMAL_VOLUME = 1.0f
        private const val DUCKED_VOLUME = 0.2f // 20%
    }

    // ========== PLAYBACK STATE ==========

    private var musicPlayer: MediaPlayer? = null
    private var currentMusicSource: MusicSource? = null
    private var currentPlaylist: List<File> = emptyList()
    private var currentTrackIndex: Int = 0
    private var currentVolume: Float = NORMAL_VOLUME
    private var targetVolume: Float = NORMAL_VOLUME

    // Video interaction state
    private var isMusicDucked: Boolean = false
    private var wasMusicPausedForVideo: Boolean = false

    // Handler for volume fades and track transitions
    private val handler = Handler(Looper.getMainLooper())
    private var volumeFadeRunnable: Runnable? = null

    // Track the last AppState to detect source changes
    private var lastState: AppState? = null
    // Track if music is actually playing (stopped during GamePlaying)
    private var isMusicPlaying: Boolean = false

    // Track if activity is visible (onStart/onStop lifecycle)
    private var isActivityVisible: Boolean = true
    // Track if music was playing before becoming invisible
    private var wasMusicPlayingBeforeInvisible: Boolean = false

    // Callback for song title updates
    private var onSongChanged: ((String) -> Unit)? = null
    // Callback for when music stops
    private var onMusicStopped: (() -> Unit)? = null

    init {
        android.util.Log.d(TAG, "MusicManager initialized")
        android.util.Log.d(TAG, "Base music path: $BASE_MUSIC_PATH")
    }

    // ========== PUBLIC API (MusicController Interface) ==========

    override fun onStateChanged(newState: AppState) {
        android.util.Log.d(TAG, "━━━ STATE CHANGE ━━━")
        android.util.Log.d(TAG, "New state: $newState")

        // CRITICAL: Don't play music if activity is not visible
        // This prevents music from playing when:
        // - Device is asleep
        // - Another app is on top (e.g., ES-DE when scrolling)
        // - User has switched away from companion
        if (!isActivityVisible) {
            android.util.Log.d(TAG, "Music blocked - activity not visible")
            stopMusic()
            lastState = newState
            return
        }

        // Check if music is globally enabled
        if (!isMusicEnabled()) {
            android.util.Log.d(TAG, "Music disabled globally - stopping playback")
            stopMusic()
            lastState = newState
            return
        }

        // Determine if this state should play music
        val shouldPlay = shouldPlayMusicForState(newState)
        android.util.Log.d(TAG, "Should play music: $shouldPlay")

        if (!shouldPlay) {
            stopMusic()
            lastState = newState
            return
        }

        // Get the music source for this state
        val newSource = getMusicSourceForState(newState)
        android.util.Log.d(TAG, "Music source: $newSource")

        if (newSource == null) {
            android.util.Log.d(TAG, "No music source available")
            stopMusic()
            lastState = newState
            return
        }

        // Determine if we need to cross-fade or continue
        val oldSource = currentMusicSource
        val needsCrossFade = shouldCrossFade(oldSource, newSource, lastState, newState)

        android.util.Log.d(TAG, "Old source: $oldSource")
        android.util.Log.d(TAG, "Needs cross-fade: $needsCrossFade")

        if (needsCrossFade) {
            // Different source - cross-fade
            crossFadeToSource(newSource)
        } else if (!isMusicPlaying) {
            // Music was stopped (even if source is the same) - start fresh
            android.util.Log.d(TAG, "Starting music (was not playing)")
            startMusic(newSource)
        } else if (oldSource == null) {
            // No music playing - start fresh
            android.util.Log.d(TAG, "Starting music (no previous source)")
            startMusic(newSource)
        } else {
            // Same source AND already playing - continue
            android.util.Log.d(TAG, "Continuing current music (same source)")
        }

        lastState = newState
    }

    override fun onVideoStarted() {
        if (musicPlayer == null || !musicPlayer!!.isPlaying) {
            return
        }

        val behavior = prefs.getString("music.video_behavior", "duck") ?: "duck"
        android.util.Log.d(TAG, "Video started - behavior: $behavior")

        when (behavior) {
            "continue" -> {
                // Do nothing - music stays at 100%
                android.util.Log.d(TAG, "Continuing music at full volume")
            }
            "duck" -> {
                duckMusic()
            }
            "pause" -> {
                pauseMusicForVideo()
            }
        }
    }

    override fun onVideoEnded() {
        android.util.Log.d(TAG, "Video ended")

        if (isMusicDucked) {
            restoreMusicVolume()
        } else if (wasMusicPausedForVideo) {
            resumeMusicFromVideo()
        }
    }

    override fun onActivityVisible() {
        android.util.Log.d(TAG, "━━━ ACTIVITY VISIBLE ━━━")
        isActivityVisible = true

        // Resume music if it was playing before becoming invisible
        if (wasMusicPlayingBeforeInvisible) {
            android.util.Log.d(TAG, "Resuming music (was playing before invisible)")

            // Cancel any pending fade operations
            volumeFadeRunnable?.let { handler.removeCallbacks(it) }

            // Clear flag and restart music from the current source
            wasMusicPlayingBeforeInvisible = false

            if (currentMusicSource != null) {
                android.util.Log.d(TAG, "Restarting music from source: $currentMusicSource")
                startMusic(currentMusicSource!!)
            } else {
                android.util.Log.d(TAG, "No music source to resume")
                isMusicPlaying = false
            }
        } else {
            android.util.Log.d(TAG, "Not resuming music (was not playing)")
        }
    }

    override fun onActivityInvisible() {
        android.util.Log.d(TAG, "━━━ ACTIVITY INVISIBLE ━━━")
        isActivityVisible = false

        // Pause music if currently playing
        if (musicPlayer?.isPlaying == true) {
            android.util.Log.d(TAG, "Pausing music (activity not visible)")

            wasMusicPlayingBeforeInvisible = true

            // Fade out then pause
            fadeVolume(currentVolume, 0f, CROSS_FADE_DURATION) {
                musicPlayer?.pause()
            }
        } else {
            android.util.Log.d(TAG, "Music not playing - no pause needed")
            wasMusicPlayingBeforeInvisible = false
        }
    }

    override fun release() {
        android.util.Log.d(TAG, "Releasing music resources")

        // Cancel any pending operations
        volumeFadeRunnable?.let { handler.removeCallbacks(it) }

        // Release player
        musicPlayer?.release()
        musicPlayer = null

        currentMusicSource = null
        currentPlaylist = emptyList()
        lastState = null
        isMusicPlaying = false
    }

    /**
     * Set callback for when song changes
     */
    fun setOnSongChangedListener(listener: (String) -> Unit) {
        onSongChanged = listener
    }

    /**
     * Set callback for when music stops
     */
    fun setOnMusicStoppedListener(listener: () -> Unit) {
        onMusicStopped = listener
    }

    // ========== MUSIC CONTROL ==========

    /**
     * Start playing music from a new source.
     */
    private fun startMusic(source: MusicSource) {
        android.util.Log.d(TAG, "Starting music from source: $source")

        // Load playlist for this source
        val playlist = loadPlaylist(source)
        if (playlist.isEmpty()) {
            android.util.Log.d(TAG, "No music files found for source: $source")
            isMusicPlaying = false
            return
        }

        android.util.Log.d(TAG, "Loaded playlist with ${playlist.size} tracks")

        // Store new state
        currentMusicSource = source
        currentPlaylist = playlist
        currentTrackIndex = 0

        // Reset volume state for new playback
        targetVolume = NORMAL_VOLUME
        isMusicDucked = false
        wasMusicPausedForVideo = false

        // Play first track
        playTrack(playlist[0])
        isMusicPlaying = true
    }

    /**
     * Stop all music playback.
     */
    private fun stopMusic() {
        if (musicPlayer == null) {
            return
        }

        android.util.Log.d(TAG, "Stopping music")

        // Notify listener that music stopped
        onMusicStopped?.invoke()

        // Fade out then stop
        fadeVolume(currentVolume, 0f, CROSS_FADE_DURATION) {
            musicPlayer?.stop()
            musicPlayer?.release()
            musicPlayer = null
            currentMusicSource = null
            currentPlaylist = emptyList()
            isMusicDucked = false
            wasMusicPausedForVideo = false
            isMusicPlaying = false
            targetVolume = NORMAL_VOLUME  // RESET target volume for next playback
        }
    }

    /**
     * Cross-fade from current source to a new source.
     */
    private fun crossFadeToSource(newSource: MusicSource) {
        android.util.Log.d(TAG, "Cross-fading to source: $newSource")

        val oldPlayer = musicPlayer

        // Load new playlist
        val newPlaylist = loadPlaylist(newSource)
        if (newPlaylist.isEmpty()) {
            android.util.Log.d(TAG, "No music files found for new source")
            stopMusic()
            return
        }

        // Update state
        currentMusicSource = newSource
        currentPlaylist = newPlaylist
        currentTrackIndex = 0

        // Fade out old player
        if (oldPlayer != null && oldPlayer.isPlaying) {
            fadeVolume(currentVolume, 0f, CROSS_FADE_DURATION) {
                oldPlayer.stop()
                oldPlayer.release()
            }
        }

        // Start new player (it will fade in)
        playTrack(newPlaylist[0])
        isMusicPlaying = true
    }

    /**
     * Play a specific audio track.
     */
    private fun playTrack(file: File) {
        android.util.Log.d(TAG, "Playing track: ${file.name}")

        // Notify listener of song change
        val songName = file.nameWithoutExtension
        onSongChanged?.invoke(songName)

        try {
            // Release old player
            musicPlayer?.release()

            // Create new player
            musicPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnPreparedListener { mp ->
                    android.util.Log.d(TAG, "Track prepared, starting playback")
                    mp.start()
                    // Fade in from 0 to target volume
                    fadeVolume(0f, targetVolume, CROSS_FADE_DURATION)
                }
                setOnCompletionListener {
                    android.util.Log.d(TAG, "Track completed, playing next")
                    playNextTrack()
                }
                setOnErrorListener { _, what, extra ->
                    android.util.Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    playNextTrack() // Skip to next track on error
                    true
                }
                prepareAsync()
            }

        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error playing track: ${file.name}", e)
            playNextTrack()
        }
    }

    /**
     * Play the next track in the playlist.
     */
    private fun playNextTrack() {
        if (currentPlaylist.isEmpty()) {
            android.util.Log.d(TAG, "No playlist loaded")
            return
        }

        // Move to next track (loop back to start if at end)
        currentTrackIndex = (currentTrackIndex + 1) % currentPlaylist.size
        android.util.Log.d(TAG, "Next track index: $currentTrackIndex / ${currentPlaylist.size}")

        playTrack(currentPlaylist[currentTrackIndex])
    }

    // ========== VOLUME CONTROL ==========

    /**
     * Duck music volume for video playback.
     */
    private fun duckMusic() {
        android.util.Log.d(TAG, "Ducking music to ${DUCKED_VOLUME * 100}%")
        isMusicDucked = true
        fadeVolume(currentVolume, DUCKED_VOLUME, DUCK_FADE_DURATION)
    }

    /**
     * Restore music volume after video ends.
     */
    private fun restoreMusicVolume() {
        android.util.Log.d(TAG, "Restoring music to ${NORMAL_VOLUME * 100}%")
        isMusicDucked = false
        fadeVolume(currentVolume, NORMAL_VOLUME, DUCK_FADE_DURATION)
    }

    /**
     * Pause music for video playback.
     */
    private fun pauseMusicForVideo() {
        android.util.Log.d(TAG, "Pausing music for video")
        wasMusicPausedForVideo = true

        // Fade out then pause
        fadeVolume(currentVolume, 0f, DUCK_FADE_DURATION) {
            musicPlayer?.pause()
        }
    }

    /**
     * Resume music after video ends.
     */
    private fun resumeMusicFromVideo() {
        android.util.Log.d(TAG, "Resuming music after video")
        wasMusicPausedForVideo = false

        musicPlayer?.start()
        fadeVolume(0f, NORMAL_VOLUME, DUCK_FADE_DURATION)
    }

    /**
     * Smoothly fade volume from one level to another.
     *
     * @param fromVolume Starting volume (0.0 - 1.0)
     * @param toVolume Target volume (0.0 - 1.0)
     * @param duration Fade duration in milliseconds
     * @param onComplete Optional callback when fade completes
     */
    private fun fadeVolume(
        fromVolume: Float,
        toVolume: Float,
        duration: Long,
        onComplete: (() -> Unit)? = null
    ) {
        // Cancel any existing fade
        volumeFadeRunnable?.let { handler.removeCallbacks(it) }

        val player = musicPlayer ?: return

        currentVolume = fromVolume
        targetVolume = toVolume
        player.setVolume(fromVolume, fromVolume)

        val startTime = System.currentTimeMillis()
        val volumeDelta = toVolume - fromVolume

        volumeFadeRunnable = object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - startTime
                val progress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)

                val newVolume = fromVolume + (volumeDelta * progress)
                currentVolume = newVolume
                player.setVolume(newVolume, newVolume)

                if (progress < 1f) {
                    handler.postDelayed(this, 16) // ~60fps
                } else {
                    onComplete?.invoke()
                }
            }
        }

        handler.post(volumeFadeRunnable!!)
    }

    // ========== PLAYLIST MANAGEMENT ==========

    /**
     * Load all audio files from a music source.
     */
    private fun loadPlaylist(source: MusicSource): List<File> {
        val sourcePath = source.getPath(BASE_MUSIC_PATH)
        val sourceDir = File(sourcePath)

        android.util.Log.d(TAG, "Loading playlist from: $sourcePath")

        if (!sourceDir.exists() || !sourceDir.isDirectory) {
            android.util.Log.d(TAG, "Music directory does not exist: $sourcePath")

            // If system-specific folder doesn't exist, try generic as fallback
            if (source is MusicSource.System) {
                android.util.Log.d(TAG, "System folder not found, falling back to generic")
                return loadPlaylist(MusicSource.Generic)
            }

            return emptyList()
        }

        // Find all audio files
        val audioFiles = sourceDir.listFiles { file ->
            file.isFile && AUDIO_EXTENSIONS.any { ext ->
                file.name.endsWith(".$ext", ignoreCase = true)
            }
        }?.toList() ?: emptyList()

        if (audioFiles.isEmpty()) {
            android.util.Log.d(TAG, "No audio files found in: $sourcePath")

            // If system-specific folder is empty, try generic as fallback
            if (source is MusicSource.System) {
                android.util.Log.d(TAG, "System folder empty, falling back to generic")
                return loadPlaylist(MusicSource.Generic)
            }

            return emptyList()
        }

        // Sort alphabetically then shuffle
        val sortedFiles = audioFiles.sortedBy { it.name }
        val shuffledFiles = sortedFiles.shuffled()

        android.util.Log.d(TAG, "Found ${shuffledFiles.size} audio files (shuffled):")
        shuffledFiles.take(5).forEach { file ->
            android.util.Log.d(TAG, "  - ${file.name}")
        }
        if (shuffledFiles.size > 5) {
            android.util.Log.d(TAG, "  ... and ${shuffledFiles.size - 5} more")
        }

        return shuffledFiles
    }

    // ========== STATE LOGIC ==========

    /**
     * Check if music is globally enabled.
     */
    private fun isMusicEnabled(): Boolean {
        return prefs.getBoolean("music.enabled", false)
    }

    /**
     * Determine if music should play for a given state.
     */
    private fun shouldPlayMusicForState(state: AppState): Boolean {
        return when (state) {
            is AppState.SystemBrowsing -> {
                prefs.getBoolean("music.system_enabled", true)
            }
            is AppState.GameBrowsing -> {
                prefs.getBoolean("music.game_enabled", true)
            }
            is AppState.GamePlaying -> {
                false // Never play music during gameplay
            }
            is AppState.Screensaver -> {
                prefs.getBoolean("music.screensaver_enabled", false)
            }
        }
    }

    /**
     * Get the music source for a given state.
     */
    private fun getMusicSourceForState(state: AppState): MusicSource? {
        val systemSpecificEnabled = prefs.getBoolean("music.system_specific_enabled", false)

        return when (state) {
            is AppState.SystemBrowsing -> {
                if (systemSpecificEnabled && state.systemName.isNotEmpty()) {
                    MusicSource.System(state.systemName)
                } else {
                    MusicSource.Generic
                }
            }
            is AppState.GameBrowsing -> {
                // Use same source as SystemBrowsing would use
                if (systemSpecificEnabled && state.systemName.isNotEmpty()) {
                    MusicSource.System(state.systemName)
                } else {
                    MusicSource.Generic
                }
            }
            is AppState.GamePlaying -> {
                null // No music during gameplay
            }
            is AppState.Screensaver -> {
                MusicSource.Generic // Always use generic for screensaver
            }
        }
    }

    /**
     * Determine if a state transition requires cross-fading.
     *
     * Cross-fade when:
     * - Source changes (e.g., Generic → System("snes"))
     * - System changes in SystemBrowsing (e.g., "snes" → "genesis")
     *
     * Continue without cross-fade when:
     * - SystemBrowsing → GameBrowsing (same system)
     * - GameBrowsing → SystemBrowsing (same system)
     */
    private fun shouldCrossFade(
        oldSource: MusicSource?,
        newSource: MusicSource,
        oldState: AppState?,
        newState: AppState
    ): Boolean {
        // If no old source, we're starting fresh (not a cross-fade)
        if (oldSource == null) {
            return false
        }

        // If sources are different, cross-fade
        if (oldSource != newSource) {
            return true
        }

        // Sources are the same - check state transitions
        // SystemBrowsing ↔ GameBrowsing should NOT cross-fade
        val isSystemToGame = oldState is AppState.SystemBrowsing && newState is AppState.GameBrowsing
        val isGameToSystem = oldState is AppState.GameBrowsing && newState is AppState.SystemBrowsing

        if (isSystemToGame || isGameToSystem) {
            // Same source, compatible states - continue playing
            return false
        }

        // Default: no cross-fade needed (same source)
        return false
    }
}
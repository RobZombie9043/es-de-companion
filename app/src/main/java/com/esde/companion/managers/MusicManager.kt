package com.esde.companion.managers

import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.esde.companion.data.AppState
import com.esde.companion.data.AppConstants
import com.esde.companion.data.MusicSource
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
 * ARCHITECTURE:
 * - Follows standard manager pattern
 * - Lifecycle: init -> use -> cleanup()
 * - Callbacks for UI updates (song changed, playback state)
 * - Activity visibility tracking (onActivityVisible/Invisible)
 * ═══════════════════════════════════════════════════════════
 */
class MusicManager(
    private val prefsManager: PreferencesManager
) {

    companion object {
        private const val TAG = "MusicManager"

        // Supported audio formats
        private val AUDIO_EXTENSIONS = AppConstants.FileExtensions.AUDIO

        // Animation durations
        private const val CROSS_FADE_DURATION = AppConstants.Timing.MUSIC_CROSS_FADE_DURATION
        private const val DUCK_FADE_DURATION = AppConstants.Timing.MUSIC_DUCK_FADE_DURATION

        // Volume levels
        private const val NORMAL_VOLUME = AppConstants.UI.MUSIC_NORMAL_VOLUME
        private const val DUCKED_VOLUME = AppConstants.UI.MUSIC_DUCKED_VOLUME
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
    // Track if black overlay is shown
    private var isBlackOverlayShown: Boolean = false

    // Callback for song title updates
    private var onSongChanged: ((String) -> Unit)? = null
    // Callback for when music stops
    private var onMusicStopped: (() -> Unit)? = null
    // Callback for when playback state changes (playing/paused)
    private var onPlaybackStateChanged: ((Boolean) -> Unit)? = null

    init {
        Log.d(TAG, "MusicManager initialized")
        Log.d(TAG, "Base music path: ${getMusicPath()}")
    }

    // ========== PATH MANAGEMENT ==========

    /**
     * Get the music path from preferences, or use default.
     */
    private fun getMusicPath(): String {
        return prefsManager.musicPath
    }

    // ========== PUBLIC API (MusicController Interface) ==========

    fun onStateChanged(newState: AppState) {
        Log.d(TAG, "━━━ STATE CHANGE ━━━")
        Log.d(TAG, "New state: $newState")

        // CRITICAL: Don't play music if activity is not visible
        // This prevents music from playing when:
        // - Device is asleep
        // - Another app is on top (e.g., ES-DE when scrolling)
        // - User has switched away from companion
        if (!isActivityVisible) {
            Log.d(TAG, "Music blocked - activity not visible")
            stopMusic()
            lastState = newState
            return
        }

        // CRITICAL: Don't play music if black overlay is shown
        // User has explicitly hidden the companion display
        if (isBlackOverlayShown) {
            Log.d(TAG, "Music blocked - black overlay shown")
            stopMusic()
            lastState = newState
            return
        }

        // Check if music is globally enabled
        if (!isMusicEnabled()) {
            Log.d(TAG, "Music disabled globally - stopping playback")
            stopMusic()
            lastState = newState
            return
        }

        // Determine if this state should play music
        val shouldPlay = shouldPlayMusicForState(newState)
        Log.d(TAG, "Should play music: $shouldPlay")

        if (!shouldPlay) {
            stopMusic()
            lastState = newState
            return
        }

        // Get the music source for this state
        val requestedSource = getMusicSourceForState(newState)
        Log.d(TAG, "Requested music source: $requestedSource")

        if (requestedSource == null) {
            Log.d(TAG, "No music source available")
            stopMusic()
            lastState = newState
            return
        }

        // Resolve the actual source (after fallback) to determine if cross-fade is needed
        val actualSource = resolveActualSource(requestedSource)
        Log.d(TAG, "Actual music source (after fallback): $actualSource")

        if (actualSource == null) {
            Log.d(TAG, "No music files available")
            stopMusic()
            lastState = newState
            return
        }

        // Determine if we need to cross-fade or continue
        val oldSource = currentMusicSource
        val needsCrossFade = shouldCrossFade(oldSource, actualSource, lastState, newState)

        Log.d(TAG, "Old source: $oldSource")
        Log.d(TAG, "Needs cross-fade: $needsCrossFade")

        if (needsCrossFade) {
            // Different source - cross-fade
            crossFadeToSource(actualSource)
        } else if (!isMusicPlaying) {
            // Music was stopped (even if source is the same) - start fresh
            Log.d(TAG, "Starting music (was not playing)")
            startMusic(actualSource)
        } else if (oldSource == null) {
            // No music playing - start fresh
            Log.d(TAG, "Starting music (no previous source)")
            startMusic(actualSource)
        } else {
            // Same source AND already playing - continue
            Log.d(TAG, "Continuing current music (same source)")
        }

        lastState = newState
    }

    fun onVideoStarted() {
        if (musicPlayer == null || !musicPlayer!!.isPlaying) {
            return
        }

        val behavior = prefsManager.musicVideoBehavior
        Log.d(TAG, "Video started - behavior: $behavior")

        when (behavior) {
            "continue" -> {
                // Do nothing - music stays at 100%
                Log.d(TAG, "Continuing music at full volume")
            }
            "duck" -> {
                duckMusic()
            }
            "pause" -> {
                pauseMusicForVideo()
            }
        }
    }

    fun onVideoEnded() {
        Log.d(TAG, "Video ended")

        // Don't restore music if we're in GamePlaying state
        // Music should stay stopped during gameplay
        if (lastState is AppState.GamePlaying) {
            Log.d(TAG, "Not restoring music - game is playing")
            // Reset video interaction flags
            isMusicDucked = false
            wasMusicPausedForVideo = false
            return
        }

        if (isMusicDucked) {
            restoreMusicVolume()
        } else if (wasMusicPausedForVideo) {
            resumeMusicFromVideo()
        }
    }

    fun onBlackOverlayChanged(isShown: Boolean) {
        Log.d(TAG, "━━━ BLACK OVERLAY ${if (isShown) "SHOWN" else "HIDDEN"} ━━━")
        isBlackOverlayShown = isShown

        if (isShown) {
            // Black overlay shown - pause music
            Log.d(TAG, "Pausing music (black overlay shown)")

            if (musicPlayer?.isPlaying == true) {
                wasMusicPausedForVideo = true

                // Fade out then pause
                fadeVolume(currentVolume, 0f, CROSS_FADE_DURATION) {
                    musicPlayer?.pause()
                }
            }
        } else {
            // Black overlay hidden - resume music
            Log.d(TAG, "Resuming music (black overlay hidden)")

            // Resume if we paused it
            if (wasMusicPausedForVideo) {
                musicPlayer?.start()
                fadeVolume(0f, NORMAL_VOLUME, CROSS_FADE_DURATION)
                wasMusicPausedForVideo = false
            }

            // NOTE: MainActivity will call onStateChanged() after this to handle
            // the case where user scrolled to a different game while overlay was shown
        }
    }

    fun onActivityVisible() {
        Log.d(TAG, "━━━ ACTIVITY VISIBLE ━━━")
        isActivityVisible = true

        // Check if music is enabled before resuming
        val musicEnabled = prefsManager.musicEnabled
        if (!musicEnabled) {
            Log.d(TAG, "Music disabled - not resuming")
            wasMusicPlayingBeforeInvisible = false
            return
        }

        // Resume music if it was playing before visibility was lost
        if (wasMusicPlayingBeforeInvisible) {
            Log.d(TAG, "Resuming music (was playing before invisible)")

            // Restart the music source that was playing
            if (currentMusicSource != null) {
                Log.d(TAG, "Restarting music from source: $currentMusicSource")
                startMusic(currentMusicSource!!)
            } else {
                Log.d(TAG, "No music source to resume")
                isMusicPlaying = false
            }
        } else {
            Log.d(TAG, "Not resuming music (was not playing)")
        }
    }

    fun onActivityInvisible() {
        Log.d(TAG, "━━━ ACTIVITY INVISIBLE ━━━")
        isActivityVisible = false

        // Pause music if currently playing
        if (musicPlayer?.isPlaying == true) {
            Log.d(TAG, "Pausing music (activity not visible)")

            wasMusicPlayingBeforeInvisible = true

            // Fade out then pause
            fadeVolume(currentVolume, 0f, CROSS_FADE_DURATION) {
                musicPlayer?.pause()
            }
        } else {
            Log.d(TAG, "Music not playing - no pause needed")
            wasMusicPlayingBeforeInvisible = false
        }
    }

    fun cleanup() {
        Log.d(TAG, "Cleaning up music resources")

        // Cancel any pending operations
        volumeFadeRunnable?.let(handler::removeCallbacks)

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

    /**
     * Set callback for when playback state changes (playing/paused).
     * @param listener Callback with Boolean parameter: true = playing, false = paused
     */
    fun setOnPlaybackStateChangedListener(listener: (Boolean) -> Unit) {
        onPlaybackStateChanged = listener
    }

    // ========== MUSIC CONTROL ==========

    /**
     * Start playing music from a new source.
     */
    private fun startMusic(source: MusicSource) {
        Log.d(TAG, "Starting music from source: $source")

        // Load playlist for this source
        val playlist = loadPlaylist(source)
        if (playlist.isEmpty()) {
            Log.d(TAG, "No music files found for source: $source")
            isMusicPlaying = false
            return
        }

        Log.d(TAG, "Loaded playlist with ${playlist.size} tracks")

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

        Log.d(TAG, "Stopping music")

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
        Log.d(TAG, "Cross-fading to source: $newSource")

        // Save reference to old player BEFORE changing musicPlayer
        val oldPlayer = musicPlayer

        // Load new playlist
        val newPlaylist = loadPlaylist(newSource)
        if (newPlaylist.isEmpty()) {
            Log.d(TAG, "No music files found for new source")
            stopMusic()
            return
        }

        // Update state
        currentMusicSource = newSource
        currentPlaylist = newPlaylist
        currentTrackIndex = 0

        // Reset volume state for new playback - start from silence
        currentVolume = 0f
        targetVolume = NORMAL_VOLUME
        isMusicDucked = false
        wasMusicPausedForVideo = false

        // Fade out and release old player independently
        if (oldPlayer != null && oldPlayer.isPlaying) {
            Log.d(TAG, "Fading out old player independently")

            // Create independent fade for old player
            val fadeSteps = (CROSS_FADE_DURATION / 50).toInt()
            val volumeStep = 1.0f / fadeSteps
            var currentStep = 0

            val oldPlayerFadeRunnable = object : Runnable {
                override fun run() {
                    if (currentStep < fadeSteps) {
                        currentStep++
                        val stepVolume = (1.0f - (volumeStep * currentStep)).coerceAtLeast(0f)
                        try {
                            oldPlayer.setVolume(stepVolume, stepVolume)
                            handler.postDelayed(this, 50)
                        } catch (_: Exception) {
                            Log.d(TAG, "Old player already released")
                        }
                    } else {
                        // Fade complete - release old player
                        try {
                            oldPlayer.stop()
                            oldPlayer.release()
                            Log.d(TAG, "Old player released after fade")
                        } catch (_: Exception) {
                            Log.d(TAG, "Error releasing old player")
                        }
                    }
                }
            }
            handler.post(oldPlayerFadeRunnable)
        } else if (oldPlayer != null) {
            // Old player exists but isn't playing - release it immediately
            Log.d(TAG, "Releasing old player (not playing)")
            try {
                oldPlayer.release()
            } catch (e: Exception) {
                Log.d(TAG, "Error releasing old player: ${e.message}")
            }
        }

        // Start new player immediately (it will fade in from 0)
        playTrack(newPlaylist[0])
        isMusicPlaying = true
    }

    /**
     * Play a specific audio track.
     */
    private fun playTrack(file: File) {
        Log.d(TAG, "Playing track: ${file.name}")

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
                    Log.d(TAG, "Track prepared, starting playback")
                    mp.start()
                    // Notify that playback started
                    onPlaybackStateChanged?.invoke(true)
                    // Fade in from 0 to target volume
                    fadeVolume(0f, targetVolume, CROSS_FADE_DURATION)
                }
                setOnCompletionListener {
                    Log.d(TAG, "Track completed, playing next")
                    playNextTrack()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    playNextTrack() // Skip to next track on error
                    true
                }
                prepareAsync()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error playing track: ${file.name}", e)
            playNextTrack()
        }
    }

    /**
     * Play the next track in the playlist.
     */
    private fun playNextTrack() {
        if (currentPlaylist.isEmpty()) {
            Log.d(TAG, "No playlist loaded")
            return
        }

        // Move to next track (loop back to start if at end)
        currentTrackIndex = (currentTrackIndex + 1) % currentPlaylist.size
        Log.d(TAG, "Next track index: $currentTrackIndex / ${currentPlaylist.size}")

        playTrack(currentPlaylist[currentTrackIndex])
    }

    // ========== PUBLIC PLAYBACK CONTROLS START ==========
    /**
     * Pause music playback (called by UI controls).
     */
    fun pauseMusic() {
        musicPlayer?.let { player ->
            if (player.isPlaying) {
                Log.d(TAG, "Pausing music via user control")
                player.pause()
                onPlaybackStateChanged?.invoke(false)
            }
        }
    }

    /**
     * Resume music playback (called by UI controls).
     */
    fun resumeMusic() {
        musicPlayer?.let { player ->
            if (!player.isPlaying) {
                Log.d(TAG, "Resuming music via user control")
                player.start()
                onPlaybackStateChanged?.invoke(true)
            }
        }
    }

    /**
     * Skip to next track in playlist (called by UI controls).
     */
    fun skipToNextTrack() {
        Log.d(TAG, "Skipping to next track via user control")
        playNextTrack()
    }

    /**
     * Check if music is currently playing.
     */
    fun isPlaying(): Boolean {
        return musicPlayer?.isPlaying ?: false
    }

    /**
     * Check if music player exists and is paused (not playing, but not released).
     */
    fun isPaused(): Boolean {
        val player = musicPlayer ?: return false
        return try {
            // If player exists and is NOT playing, it's paused
            !player.isPlaying
        } catch (_: IllegalStateException) {
            // Player was released
            false
        }
    }
    // ========== PUBLIC PLAYBACK CONTROLS END ==========

    // ========== VOLUME CONTROL ==========

    /**
     * Duck music volume for video playback.
     */
    private fun duckMusic() {
        Log.d(TAG, "Ducking music to ${DUCKED_VOLUME * 100}%")
        isMusicDucked = true
        fadeVolume(currentVolume, DUCKED_VOLUME, DUCK_FADE_DURATION)
    }

    /**
     * Restore music volume after video ends.
     */
    private fun restoreMusicVolume() {
        Log.d(TAG, "Restoring music to ${NORMAL_VOLUME * 100}%")
        isMusicDucked = false
        fadeVolume(currentVolume, NORMAL_VOLUME, DUCK_FADE_DURATION)
    }

    /**
     * Pause music for video playback.
     */
    private fun pauseMusicForVideo() {
        Log.d(TAG, "Pausing music for video")
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
        Log.d(TAG, "Resuming music after video")
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

        // SAFETY: Check if player is valid before setting volume
        try {
            if (player.isPlaying || !player.isPlaying) { // Triggers IllegalStateException if released
                player.setVolume(fromVolume, fromVolume)
            }
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Player already released, canceling fade: ${e.message}")
            return
        }

        val startTime = System.currentTimeMillis()
        val volumeDelta = toVolume - fromVolume

        volumeFadeRunnable = object : Runnable {
            override fun run() {
                // SAFETY: Check if player still exists and is valid
                try {
                    // Verify player is still the same instance and not released
                    if (player.isPlaying || !player.isPlaying) { // Triggers IllegalStateException if released
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
                } catch (_: IllegalStateException) {
                    // Player was released during fade - this is normal during cross-fades
                    Log.d(TAG, "Fade canceled: player was released")
                    // Don't call onComplete - the release was intentional
                }
            }
        }

        handler.post(volumeFadeRunnable!!)
    }

    // ========== PLAYLIST MANAGEMENT ==========

    /**
     * Load all audio files from a music source.
     * Scans recursively through all subdirectories.
     */
    private fun loadPlaylist(source: MusicSource): List<File> {
        val sourcePath = source.getPath(getMusicPath())
        val sourceDir = File(sourcePath)

        Log.d(TAG, "Loading playlist from: $sourcePath")

        if (!sourceDir.exists() || !sourceDir.isDirectory) {
            Log.d(TAG, "Music directory does not exist: $sourcePath")

            // If system-specific folder doesn't exist, try generic as fallback
            if (source is MusicSource.System) {
                Log.d(TAG, "System folder not found, falling back to generic")
                return loadPlaylist(MusicSource.Generic)
            }

            return emptyList()
        }

        // Find all audio files recursively, excluding "systems" subfolder for generic source
        val audioFiles = scanAudioFilesRecursively(sourceDir, excludeSystemsFolder = (source is MusicSource.Generic))

        if (audioFiles.isEmpty()) {
            Log.d(TAG, "No audio files found in: $sourcePath")

            // If system-specific folder is empty, try generic as fallback
            if (source is MusicSource.System) {
                Log.d(TAG, "System folder empty, falling back to generic")
                return loadPlaylist(MusicSource.Generic)
            }

            return emptyList()
        }

        // Sort alphabetically then shuffle
        val sortedFiles = audioFiles.sortedBy { it.absolutePath }
        val shuffledFiles = sortedFiles.shuffled()

        Log.d(TAG, "Found ${shuffledFiles.size} audio files (shuffled):")
        shuffledFiles.take(5).forEach { file ->
            Log.d(TAG, "  - ${file.relativeTo(sourceDir).path}")
        }
        if (shuffledFiles.size > 5) {
            Log.d(TAG, "  ... and ${shuffledFiles.size - 5} more")
        }

        return shuffledFiles
    }

    /**
     * Recursively scan a directory for audio files.
     *
     * @param directory The directory to scan
     * @param excludeSystemsFolder If true, skip the "systems" subfolder (for generic music)
     * @return List of audio files found
     */
    private fun scanAudioFilesRecursively(directory: File, excludeSystemsFolder: Boolean = false): List<File> {
        val audioFiles = mutableListOf<File>()

        val filesToProcess = mutableListOf(directory)

        while (filesToProcess.isNotEmpty()) {
            val currentDir = filesToProcess.removeAt(0)

            // Skip if not a directory
            if (!currentDir.isDirectory) continue

            // Skip "systems" folder if this is generic music scan
            if (excludeSystemsFolder && currentDir.name == AppConstants.Paths.MUSIC_SYSTEMS_SUBDIR && currentDir.parentFile == directory) {
                Log.d(TAG, "Skipping systems folder in generic scan: ${currentDir.absolutePath}")
                continue
            }

            currentDir.listFiles()?.forEach { file ->
                when {
                    file.isFile && AUDIO_EXTENSIONS.any { ext ->
                        file.name.endsWith(".$ext", ignoreCase = true)
                    } -> {
                        // Audio file found - add to list
                        audioFiles.add(file)
                    }
                    file.isDirectory -> {
                        // Subdirectory found - add to processing queue
                        filesToProcess.add(file)
                    }
                }
            }
        }

        return audioFiles
    }

    // ========== STATE LOGIC ==========

    /**
     * Check if music is globally enabled.
     */
    private fun isMusicEnabled(): Boolean {
        return prefsManager.musicEnabled
    }

    /**
     * Determine if music should play for a given state.
     */
    private fun shouldPlayMusicForState(state: AppState): Boolean {
        return when (state) {
            is AppState.SystemBrowsing -> {
                prefsManager.musicSystemEnabled
            }
            is AppState.GameBrowsing -> {
                prefsManager.musicGameEnabled
            }
            is AppState.GamePlaying -> {
                false // Never play music during gameplay
            }
            is AppState.Screensaver -> {
                prefsManager.musicScreensaverEnabled
            }
        }
    }

    /**
     * Get the music source for a given state.
     */
    private fun getMusicSourceForState(state: AppState): MusicSource? {
        return when (state) {
            is AppState.SystemBrowsing -> {
                if (state.systemName.isNotEmpty()) {
                    MusicSource.System(state.systemName)
                } else {
                    MusicSource.Generic
                }
            }
            is AppState.GameBrowsing -> {
                if (state.systemName.isNotEmpty()) {
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
     * Resolve the actual music source after fallback logic.
     *
     * This determines what source will ACTUALLY be used after the loadPlaylist()
     * fallback logic is applied. This prevents unnecessary cross-fades when
     * multiple systems fall back to the same Generic source.
     *
     * @param requestedSource The initially requested source
     * @return The actual source that will be used, or null if no music available
     */
    private fun resolveActualSource(requestedSource: MusicSource): MusicSource? {
        val baseMusicPath = getMusicPath()

        // For Generic source, check if it exists
        if (requestedSource is MusicSource.Generic) {
            val sourcePath = requestedSource.getPath(baseMusicPath)
            return if (hasAudioFiles(sourcePath)) requestedSource else null
        }

        // For System source, check if system folder exists with audio files
        if (requestedSource is MusicSource.System) {
            val sourcePath = requestedSource.getPath(baseMusicPath)

            // If system folder has audio files, use it
            if (hasAudioFiles(sourcePath)) {
                return requestedSource
            }

            // System folder doesn't exist/is empty - will fall back to Generic
            Log.d(TAG, "System folder not found/empty, will use generic fallback")
            val genericPath = MusicSource.Generic.getPath(baseMusicPath)
            return if (hasAudioFiles(genericPath)) MusicSource.Generic else null
        }

        return null
    }

    /**
     * Check if a directory exists and contains audio files (recursively).
     */
    private fun hasAudioFiles(path: String): Boolean {
        val dir = File(path)

        if (!dir.exists() || !dir.isDirectory) {
            return false
        }

        // Use recursive scan to check for audio files
        val excludeSystems = !path.contains("/systems/") // Exclude systems folder only for generic path
        val audioFiles = scanAudioFilesRecursively(dir, excludeSystemsFolder = excludeSystems)

        return audioFiles.isNotEmpty()
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
package com.esde.companion.managers

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import com.esde.companion.data.AppConstants
import com.esde.companion.MediaFileLocator
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import java.io.File

/**
 * Manages video playback using ExoPlayer.
 *
 * Handles:
 * - Finding video files for games
 * - Playing videos with delay
 * - Video lifecycle (play, pause, release)
 * - Volume control
 * - Completion callbacks
 *
 * Benefits:
 * - Centralized video logic
 * - Easier to test video behavior
 * - Clean video lifecycle management
 */
class VideoManager(
    private val context: Context,
    private val prefsManager: PreferencesManager,
    private val mediaFileLocator: MediaFileLocator,
    private val videoView: PlayerView
) {

    companion object {
        private const val TAG = "VideoManager"
    }

    private var exoPlayer: ExoPlayer? = null
    private var videoDelayHandler: Handler? = null
    private var videoDelayRunnable: Runnable? = null

    private var onVideoStarted: (() -> Unit)? = null
    private var onVideoEnded: (() -> Unit)? = null

    // ========== INITIALIZATION ==========

    init {
        setupVideoView()
    }

    private fun setupVideoView() {
        videoView.useController = false
        videoView.visibility = View.GONE
    }

    // ========== VIDEO PLAYBACK ==========

    /**
     * Attempt to play video for a game after configured delay.
     *
     * @param systemName ES-DE system name
     * @param gameFilename Game filename (may include subfolders)
     * @param onStarted Callback when video actually starts playing
     * @param onEnded Callback when video completes
     * @return true if video was found and will play, false otherwise
     */
    fun playVideoWithDelay(
        systemName: String,
        gameFilename: String,
        onStarted: (() -> Unit)? = null,
        onEnded: (() -> Unit)? = null
    ): Boolean {
        if (!prefsManager.videoEnabled) {
            android.util.Log.d(TAG, "Video disabled in settings")
            return false
        }

        // Store callbacks
        this.onVideoStarted = onStarted
        this.onVideoEnded = onEnded

        // Find video file using MediaFileLocator
        val videoPath = mediaFileLocator.findVideoFile(systemName, gameFilename)

        if (videoPath == null) {
            android.util.Log.d(TAG, "No video found for: $systemName / $gameFilename")
            return false
        }

        val videoFile = File(videoPath)
        if (!videoFile.exists()) {
            android.util.Log.d(TAG, "Video file does not exist: $videoPath")
            return false
        }

        android.util.Log.d(TAG, "Found video: ${videoFile.absolutePath}")

        // Cancel any pending video
        cancelVideoDelay()

        // Schedule video playback
        val delay = (prefsManager.videoDelay * AppConstants.Timing.VIDEO_DELAY_MULTIPLIER).toLong()
        android.util.Log.d(TAG, "Scheduling video playback in ${delay}ms")

        videoDelayHandler = Handler(Looper.getMainLooper())
        videoDelayRunnable = Runnable {
            playVideoNow(videoFile)
        }
        videoDelayHandler?.postDelayed(videoDelayRunnable!!, delay)

        return true
    }

    /**
     * Play video immediately (internal).
     */
    private fun playVideoNow(videoFile: File) {
        android.util.Log.d(TAG, "Playing video now: ${videoFile.absolutePath}")

        try {
            // Release existing player
            releasePlayer()

            // Create new player
            exoPlayer = ExoPlayer.Builder(context).build()
            videoView.player = exoPlayer

            // Enable video looping
            exoPlayer?.repeatMode = Player.REPEAT_MODE_ONE
            android.util.Log.d(TAG, "Video repeat mode enabled")

            // Set up player listener
            exoPlayer?.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            android.util.Log.d(TAG, "Video ready to play")
                            videoView.visibility = View.VISIBLE
                            updateVideoVolume()
                            onVideoStarted?.invoke()
                        }
                        Player.STATE_ENDED -> {
                            android.util.Log.d(TAG, "Video ended naturally")
                            videoView.visibility = View.GONE
                            // Don't invoke callback here - releasePlayer() will do it
                            // This prevents double-invocation
                            releasePlayer()
                        }
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    android.util.Log.e(TAG, "Video playback error: ${error.message}")
                    android.util.Log.e(TAG, "Error code: ${error.errorCode}")

                    // Log specific error details
                    if (error is androidx.media3.exoplayer.ExoPlaybackException) {
                        when (error.type) {
                            androidx.media3.exoplayer.ExoPlaybackException.TYPE_SOURCE -> {
                                android.util.Log.e(TAG, "Source error - file may be corrupted or inaccessible")
                            }
                            androidx.media3.exoplayer.ExoPlaybackException.TYPE_RENDERER -> {
                                android.util.Log.e(TAG, "Renderer error - codec may not support this video format")
                            }
                            androidx.media3.exoplayer.ExoPlaybackException.TYPE_UNEXPECTED -> {
                                android.util.Log.e(TAG, "Unexpected error during playback")
                            }
                        }
                    }

                    // Hide video view and clean up - fallback to background image
                    videoView.visibility = View.GONE
                    releasePlayer()
                }
            })

            // Prepare and play
            val mediaItem = MediaItem.fromUri(videoFile.toURI().toString())
            exoPlayer?.setMediaItem(mediaItem)
            exoPlayer?.prepare()
            exoPlayer?.playWhenReady = true

        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error playing video", e)
            releasePlayer()
            // Don't invoke callback here - releasePlayer() will do it
        }
    }

    /**
     * Cancel pending video playback.
     */
    fun cancelVideoDelay() {
        videoDelayRunnable?.let {
            videoDelayHandler?.removeCallbacks(it)
            android.util.Log.d(TAG, "Cancelled pending video")
        }
        videoDelayHandler = null
        videoDelayRunnable = null
    }

    /**
     * Stop and hide current video.
     */
    fun stopVideo() {
        android.util.Log.d(TAG, "Stopping video")
        cancelVideoDelay()
        releasePlayer()
        videoView.visibility = View.GONE
    }

    /**
     * Stop current video immediately when scrolling to new game.
     *
     * This is different from stopVideo() in that it's specifically designed
     * to be called before starting a new video, so it:
     * - Cancels any OLD pending video delays
     * - Releases the current player
     * - Hides the video view
     * - But does NOT interfere with NEW video delays that will be scheduled after this call
     *
     * @return true if a video was stopped, false if no video was playing
     */
    fun stopCurrentVideoForNewGame(): Boolean {
        val wasPlaying = exoPlayer != null && videoView.visibility == View.VISIBLE

        if (wasPlaying) {
            android.util.Log.d(TAG, "Stopping current video for new game")

            // Cancel any pending OLD video delays
            cancelVideoDelay()

            // Release player and hide view
            releasePlayer()
            videoView.visibility = View.GONE
        }

        return wasPlaying
    }

    // ========== VOLUME CONTROL ==========

    /**
     * Update video volume based on system settings.
     *
     * Supports per-display volume on devices like Ayn Thor.
     */
    fun updateVideoVolume() {
        val player = exoPlayer ?: return

        // Check if video audio is disabled in settings
        if (!prefsManager.videoAudioEnabled) {
            player.volume = 0f
            android.util.Log.d(TAG, "Video audio disabled - volume set to 0")
            return
        }

        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE)
                    as android.media.AudioManager

            // Try secondary screen volume first (Ayn Thor)
            val secondaryVolume = android.provider.Settings.System.getInt(
                context.contentResolver,
                "secondary_screen_volume_level",
                -1
            )

            val volume = if (secondaryVolume >= 0) {
                // Use secondary screen volume
                val maxVolume = android.provider.Settings.System.getInt(
                    context.contentResolver,
                    "secondary_screen_volume_max",
                    15
                )
                secondaryVolume.toFloat() / maxVolume.toFloat()
            } else {
                // Use standard STREAM_MUSIC volume
                val currentVolume = audioManager.getStreamVolume(
                    android.media.AudioManager.STREAM_MUSIC
                )
                val maxVolume = audioManager.getStreamMaxVolume(
                    android.media.AudioManager.STREAM_MUSIC
                )
                currentVolume.toFloat() / maxVolume.toFloat()
            }

            player.volume = volume
            android.util.Log.d(TAG, "Video volume set to: $volume")

        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error updating video volume", e)
        }
    }

    // ========== LIFECYCLE ==========

    /**
     * Release ExoPlayer resources.
     */
    fun releasePlayer() {
        // Check if we're releasing an active player
        val hadActivePlayer = exoPlayer != null

        exoPlayer?.release()
        exoPlayer = null
        videoView.player = null
        android.util.Log.d(TAG, "Player released")

        // If we had an active player, notify that video ended
        // This ensures music is restored even if video didn't complete naturally
        if (hadActivePlayer) {
            onVideoEnded?.invoke()
            onVideoEnded = null // Clear callback after invoking
        }
    }

    /**
     * Cleanup all resources.
     * Call in Activity.onDestroy()
     */
    fun cleanup() {
        cancelVideoDelay()
        releasePlayer()
    }
}
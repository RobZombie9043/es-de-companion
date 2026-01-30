package com.esde.companion.managers

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import com.esde.companion.MediaFileLocator
import com.esde.companion.OverlayWidget
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
        val delay = (prefsManager.videoDelay * 500).toLong() // videoDelay is 0-10, multiply by 500ms
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
                            android.util.Log.d(TAG, "Video ended")
                            videoView.visibility = View.GONE
                            onVideoEnded?.invoke()
                            releasePlayer()
                        }
                    }
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
            onVideoEnded?.invoke()
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
        exoPlayer?.release()
        exoPlayer = null
        videoView.player = null
        android.util.Log.d(TAG, "Player released")
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
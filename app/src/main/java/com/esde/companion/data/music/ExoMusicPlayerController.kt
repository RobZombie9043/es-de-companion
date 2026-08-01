package com.esde.companion.data.music

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.esde.companion.domain.model.MusicTrack
import com.esde.companion.domain.repository.MusicPlayerController
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Headless ExoPlayer (audio only, no PlayerView) held for the app's whole process
 * lifetime - unlike VideoOverlayScreen's per-composable player, this one is constructed
 * once in AppContainer and never released, since background music has no "screen" to be
 * scoped to. repeatMode is REPEAT_MODE_OFF (not REPEAT_MODE_ONE like the video overlay)
 * so STATE_ENDED actually fires, which is what drives [observeTrackCompletion].
 */
class ExoMusicPlayerController(context: Context) : MusicPlayerController {

    private val player =
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }

    private val trackCompletions = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        player.addListener(
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        trackCompletions.tryEmit(Unit)
                    }
                }
            },
        )
    }

    override fun playTrack(track: MusicTrack) {
        player.setMediaItem(MediaItem.fromUri(Uri.fromFile(File(track.filePath))))
        player.prepare()
        player.playWhenReady = true
    }

    override fun pause() {
        player.playWhenReady = false
    }

    override fun resume() {
        player.playWhenReady = true
    }

    override fun setVolume(fraction: Float) {
        player.volume = fraction
    }

    override fun observeTrackCompletion(): Flow<Unit> = trackCompletions.asSharedFlow()
}

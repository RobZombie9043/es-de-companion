@file:OptIn(UnstableApi::class)

package com.esde.companion.ui.video

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import java.io.File

/**
 * Opaque full-screen video cover - see MainActivity's showVideoOverlay for the gating
 * that decides when this is composed at all.
 *
 * [isPlaying] tracks Player.isPlaying (actual decoded frames rendering), not merely
 * "play() was called" - there's a real gap between the two while ExoPlayer buffers, and
 * the covering Box/AndroidView below is only rendered once isPlaying is true. Before
 * that (including the whole [delaySeconds] window), this composable renders nothing,
 * letting whatever's underneath (the widget canvas) show through untouched.
 *
 * A new [videoPath] recreates [player] via remember(videoPath), which also resets
 * [isPlaying] back to false - so switching games correctly drops back to "show widgets"
 * for the new delay window rather than carrying over the previous video's playing state.
 *
 * [onPlaybackEvent] reports [VideoPlaybackEvent.PlayingChanged] on every
 * onIsPlayingChanged, [VideoPlaybackEvent.Started] additionally whenever it becomes true -
 * including a resume after a mid-video rebuffer, not just the very first frame - and
 * [VideoPlaybackEvent.Error] on a player error.
 */
@Composable
fun VideoOverlayScreen(
    videoPath: String,
    delaySeconds: Int,
    audioEnabled: Boolean,
    modifier: Modifier = Modifier,
    onPlaybackEvent: (VideoPlaybackEvent) -> Unit = {},
) {
    val context = LocalContext.current

    var isPlaying by remember(videoPath) { mutableStateOf(false) }

    val player =
        remember(videoPath) {
            ExoPlayer.Builder(context).build().apply {
                addListener(
                    object : Player.Listener {
                        override fun onIsPlayingChanged(playing: Boolean) {
                            isPlaying = playing
                            onPlaybackEvent(VideoPlaybackEvent.PlayingChanged(playing))
                            if (playing) onPlaybackEvent(VideoPlaybackEvent.Started)
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            onPlaybackEvent(VideoPlaybackEvent.Error(error.message ?: error.toString()))
                        }
                    },
                )
                setMediaItem(MediaItem.fromUri(Uri.fromFile(File(videoPath))))
                repeatMode = Player.REPEAT_MODE_ONE
                prepare()
            }
        }

    LaunchedEffect(player, delaySeconds) {
        delay(delaySeconds * 1000L)
        player.play()
    }

    LaunchedEffect(player, audioEnabled) {
        player.volume = if (audioEnabled) 1f else 0f
    }

    DisposableEffect(player) {
        onDispose {
            // player.release() doesn't reliably re-fire onIsPlayingChanged, so without
            // this explicit call a video that becomes ineligible mid-playback (context
            // change, app backgrounded) could leave the last "true" stuck, permanently
            // ducking background music.
            onPlaybackEvent(VideoPlaybackEvent.PlayingChanged(false))
            player.release()
        }
    }

    if (isPlaying) {
        Box(modifier = modifier.background(Color.Black)) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                    }
                },
                // update (unlike factory) re-runs on every recomposition, which is what
                // rebinds this PlayerView to a new ExoPlayer instance when videoPath
                // changes - factory alone only fires once, on first mount, and would
                // otherwise leave this View pointed at a since-released player forever.
                update = { playerView ->
                    playerView.player = player
                    playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@file:OptIn(UnstableApi::class)

package com.esde.companion.ui.video

import android.content.Context
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import java.io.File

/**
 * Opaque full-screen video cover - see MainActivity's showVideoOverlay for the gating
 * that decides when this is composed at all.
 *
 * [videoPath] is the *target* video; what's actually attached to the on-screen
 * [PlayerView] is tracked separately as `displayedPlayer`/`displayedPath`. A nonzero
 * [delaySeconds] is a deliberate "don't show a video yet" pause - intended to keep videos
 * from flickering past while the user is just scrolling the game list - so the previous
 * game's video is dropped immediately (back to the widget canvas) rather than held onto
 * through that wait; see `dropDisplayedVideoForDelay`. What *is* bridged is the purely
 * technical buffering gap right before playback: once the wait (if any) is over, the
 * previous video (only present when [delaySeconds] is 0, since a nonzero delay already
 * dropped it above) keeps playing until the next one has real decoded frames ready
 * (`Player.isPlaying` true), at which point the swap happens in a single step and the
 * outgoing player is released - the same pre-decode-before-swap principle as
 * CrossfadeAsyncImage, applied to video. The very first video (no prior `displayedPlayer`
 * to hold onto) still renders nothing until it's ready, same as before this change.
 *
 * The incoming player is built muted (`volume = 0f`) and only unmuted at the moment
 * it's promoted to `displayedPlayer` - both players are genuinely decoding audio during
 * the overlap window (audio isn't gated by which one is attached to the visible
 * [PlayerView]), so without this a transitioning video would briefly double up on audio
 * with the outgoing one.
 *
 * [onPlaybackEvent] only fires for the currently-*displayed* player's state changes
 * (gated by identity inside the listener), not for an incoming player still buffering -
 * otherwise a still-buffering player's very first `isPlaying = true` would report
 * "playing" a moment before it's actually promoted/audible. The promotion itself
 * explicitly fires [VideoPlaybackEvent.PlayingChanged]\(true\) and
 * [VideoPlaybackEvent.Started] once, since the listener's gate wouldn't otherwise catch
 * that specific transition (the player wasn't "displayed" yet at the instant it fired).
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

    var displayedPath by remember { mutableStateOf<String?>(null) }
    var displayedPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    LaunchedEffect(videoPath) {
        if (videoPath == displayedPath) return@LaunchedEffect
        val displayedSlot =
            DisplayedVideoSlot(
                current = { displayedPlayer },
                promote = { player ->
                    displayedPlayer = player
                    displayedPath = videoPath
                },
                clear = {
                    displayedPlayer = null
                    displayedPath = null
                },
            )
        transitionToVideo(
            context = context,
            videoPath = videoPath,
            settings = VideoTransitionSettings(delaySeconds, audioEnabled),
            displayedSlot = displayedSlot,
            onPlaybackEvent = onPlaybackEvent,
        )
    }

    LaunchedEffect(displayedPlayer, audioEnabled) {
        displayedPlayer?.volume = if (audioEnabled) 1f else 0f
    }

    DisposableEffect(Unit) {
        onDispose {
            // release() doesn't reliably re-fire onIsPlayingChanged, so without this
            // explicit call a video that becomes ineligible (context change, app
            // backgrounded) could leave the last "true" stuck, permanently ducking
            // background music.
            if (displayedPlayer != null) onPlaybackEvent(VideoPlaybackEvent.PlayingChanged(false))
            displayedPlayer?.release()
        }
    }

    val player = displayedPlayer
    if (player != null) {
        Box(modifier = modifier.background(Color.Black)) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                    }
                },
                // update (unlike factory) re-runs on every recomposition, which is what
                // rebinds this PlayerView to a new ExoPlayer instance when the displayed
                // player changes - factory alone only fires once, on first mount, and
                // would otherwise leave this View pointed at a since-released player
                // forever.
                update = { playerView ->
                    playerView.player = player
                    playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * The currently-displayed player, bundled with how to replace it - grouped into one type
 * (rather than two separate lambda parameters) purely to keep [transitionToVideo] under
 * this project's [LongParameterList] limit.
 */
private class DisplayedVideoSlot(
    val current: () -> ExoPlayer?,
    val promote: (ExoPlayer) -> Unit,
    val clear: () -> Unit,
)

/** Bundled for the same [LongParameterList] reason as [DisplayedVideoSlot]. */
private class VideoTransitionSettings(
    val delaySeconds: Int,
    val audioEnabled: Boolean,
)

/**
 * Prepares [videoPath] off-screen (muted) and, once it has real decoded frames ready,
 * hands it to [DisplayedVideoSlot.promote] and releases whatever
 * [DisplayedVideoSlot.current] returned beforehand - see [VideoOverlayScreen]'s kdoc for
 * why the promotion happens in one step rather than dropping through "nothing displayed"
 * while this prepares.
 */
private suspend fun transitionToVideo(
    context: Context,
    videoPath: String,
    settings: VideoTransitionSettings,
    displayedSlot: DisplayedVideoSlot,
    onPlaybackEvent: (VideoPlaybackEvent) -> Unit,
) {
    // A configured delay is a deliberate "don't show a video yet" pause (so that quickly
    // scrolling through the game list doesn't play a video per game) - the widget canvas
    // should be visible for that whole wait, not whatever the previously-browsed game's
    // video happened to be. Drop it immediately rather than bridging into the wait, and
    // only bridge (below) across the technical buffering gap that follows. A zero delay
    // has no such wait to occupy, so the previous video keeps playing straight into that
    // buffering gap, same as before this distinction existed.
    if (settings.delaySeconds > 0) dropDisplayedVideoForDelay(displayedSlot, onPlaybackEvent)

    val ready = CompletableDeferred<Unit>()
    val incoming = createIncomingPlayer(context, videoPath, displayedSlot.current, ready, onPlaybackEvent)

    try {
        delay(settings.delaySeconds * 1000L)
        incoming.play()
        ready.await()

        incoming.volume = if (settings.audioEnabled) 1f else 0f
        val outgoing = displayedSlot.current()
        displayedSlot.promote(incoming)
        onPlaybackEvent(VideoPlaybackEvent.PlayingChanged(true))
        onPlaybackEvent(VideoPlaybackEvent.Started)
        outgoing?.release()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (expectedError: PlaybackException) {
        // Already reported via the listener's onPlayerError above (an Error event) - fall
        // through to cleanup below, leaving whatever was already displayed (if anything)
        // playing untouched rather than dropping through to the widget canvas. No
        // PlayingChanged here: the outgoing player (if any) is still genuinely playing.
    } finally {
        if (displayedSlot.current() !== incoming) incoming.release()
    }
}

/**
 * Releases whatever's currently displayed (if anything) and clears the slot, dropping back
 * to the widget canvas - see the call site in [transitionToVideo] for why this only runs
 * when there's a nonzero delay to wait out.
 */
private fun dropDisplayedVideoForDelay(
    displayedSlot: DisplayedVideoSlot,
    onPlaybackEvent: (VideoPlaybackEvent) -> Unit,
) {
    val outgoing = displayedSlot.current() ?: return
    displayedSlot.clear()
    onPlaybackEvent(VideoPlaybackEvent.PlayingChanged(false))
    outgoing.release()
}

/**
 * Builds a muted, prepared-but-not-yet-playing [ExoPlayer] for [videoPath], wired so that
 * [ready] completes once it actually starts rendering frames (or completes exceptionally
 * on a player error) - the readiness signal the caller awaits before promoting it to the
 * displayed player. [displayedPlayerProvider] is polled on every `isPlaying` change (not
 * captured once) so the listener only forwards to [onPlaybackEvent] once this exact
 * player instance has actually been promoted - see [VideoOverlayScreen]'s kdoc.
 */
private fun createIncomingPlayer(
    context: Context,
    videoPath: String,
    displayedPlayerProvider: () -> ExoPlayer?,
    ready: CompletableDeferred<Unit>,
    onPlaybackEvent: (VideoPlaybackEvent) -> Unit,
): ExoPlayer {
    val incoming = ExoPlayer.Builder(context).build()
    incoming.volume = 0f
    incoming.setMediaItem(MediaItem.fromUri(Uri.fromFile(File(videoPath))))
    incoming.repeatMode = Player.REPEAT_MODE_ONE
    incoming.addListener(
        object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                if (incoming === displayedPlayerProvider()) {
                    onPlaybackEvent(VideoPlaybackEvent.PlayingChanged(playing))
                    if (playing) onPlaybackEvent(VideoPlaybackEvent.Started)
                }
                if (playing) ready.complete(Unit)
            }

            override fun onPlayerError(error: PlaybackException) {
                onPlaybackEvent(VideoPlaybackEvent.Error(error.message ?: error.toString()))
                ready.completeExceptionally(error)
            }
        },
    )
    incoming.prepare()
    return incoming
}

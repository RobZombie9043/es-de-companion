@file:OptIn(UnstableApi::class)

package com.esde.companion.ui.widgets

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
import com.esde.companion.domain.model.PillarboxMode
import com.esde.companion.domain.model.ScaleMode
import com.esde.companion.domain.model.WidgetContent
import com.esde.companion.ui.video.VideoPlaybackEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import java.io.File

private const val MILLIS_PER_SECOND = 1000L

/**
 * Widget-canvas rendering of a [WidgetContent.Video] - sized to its widget's placed
 * bounds (via [modifier]) rather than a full-screen overlay, the widget-ized replacement
 * for the retired `ui/video/VideoOverlayScreen`. The double-buffered ExoPlayer prepare-
 * then-promote mechanics below are carried over unchanged from that screen - see its
 * original kdoc history for why: the incoming player is built muted and only promoted/
 * unmuted once it has real decoded frames ready, so a video swap (browsing to a new game)
 * never flashes blank or double-plays audio during the handoff.
 *
 * [content.scaleMode] maps to [PlayerView]'s resize mode (Contain -> RESIZE_MODE_FIT,
 * Cover -> RESIZE_MODE_ZOOM, ExoPlayer's crop-to-fill mode). [content.pillarboxMode] sets
 * the background behind the video - only visible under Contain, where a mismatched aspect
 * ratio leaves empty space around the frame; Cover always fills its bounds, so the
 * background never shows through regardless of this setting.
 */
@Composable
internal fun WidgetVideoContent(
    content: WidgetContent.Video,
    onPlaybackEvent: (VideoPlaybackEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    var displayedPath by remember { mutableStateOf<String?>(null) }
    var displayedPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    LaunchedEffect(content.path) {
        if (content.path == displayedPath) return@LaunchedEffect
        val displayedSlot =
            DisplayedVideoSlot(
                current = { displayedPlayer },
                promote = { player ->
                    displayedPlayer = player
                    displayedPath = content.path
                },
                clear = {
                    displayedPlayer = null
                    displayedPath = null
                },
            )
        transitionToVideo(
            context = context,
            videoPath = content.path,
            settings = VideoTransitionSettings(content.delaySeconds, content.audioEnabled),
            displayedSlot = displayedSlot,
            onPlaybackEvent = onPlaybackEvent,
        )
    }

    LaunchedEffect(displayedPlayer, content.audioEnabled) {
        displayedPlayer?.volume = if (content.audioEnabled) 1f else 0f
    }

    DisposableEffect(Unit) {
        onDispose {
            // release() doesn't reliably re-fire onIsPlayingChanged, so without this
            // explicit call a video that becomes ineligible (browsed away from, widget
            // removed) could leave the last "true" stuck, permanently ducking background
            // music - see VideoOverlayScreen's original kdoc for the same reasoning.
            if (displayedPlayer != null) onPlaybackEvent(VideoPlaybackEvent.PlayingChanged(false))
            displayedPlayer?.release()
        }
    }

    val player = displayedPlayer
    if (player != null) {
        Box(modifier = modifier.background(content.pillarboxMode.toBackgroundColor())) {
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
                    playerView.resizeMode = content.scaleMode.toResizeMode()
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private fun ScaleMode.toResizeMode(): Int =
    when (this) {
        ScaleMode.Fit -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        ScaleMode.Fill -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    }

private fun PillarboxMode.toBackgroundColor(): Color =
    when (this) {
        PillarboxMode.Black -> Color.Black
        PillarboxMode.Transparent -> Color.Transparent
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
 * [DisplayedVideoSlot.current] returned beforehand - see [WidgetVideoContent]'s kdoc for
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
        delay(settings.delaySeconds * MILLIS_PER_SECOND)
        incoming.play()
        ready.await()

        incoming.volume = if (settings.audioEnabled) 1f else 0f
        val outgoing = displayedSlot.current()
        displayedSlot.promote(incoming)
        onPlaybackEvent(VideoPlaybackEvent.PlayingChanged(true))
        onPlaybackEvent(VideoPlaybackEvent.Started(videoPath))
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
 * player instance has actually been promoted - see [WidgetVideoContent]'s kdoc.
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
                    if (playing) onPlaybackEvent(VideoPlaybackEvent.Started(videoPath))
                }
                if (playing) ready.complete(Unit)
            }

            override fun onPlayerError(error: PlaybackException) {
                onPlaybackEvent(VideoPlaybackEvent.Error(videoPath, error.message ?: error.toString()))
                ready.completeExceptionally(error)
            }
        },
    )
    incoming.prepare()
    return incoming
}

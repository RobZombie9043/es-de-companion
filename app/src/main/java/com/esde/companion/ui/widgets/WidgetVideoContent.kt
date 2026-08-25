@file:OptIn(UnstableApi::class)

package com.esde.companion.ui.widgets

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.esde.companion.R
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
 * Three things below are kept long-lived/fixed across the whole browsing session, all in
 * service of the same goal: never touch an [ExoPlayer]'s actual video-output Surface
 * attachment during an ordinary transition, since that turned out to be the real cost
 * stuttering a concurrently-running logo slide animation whenever a nonzero Start Delay
 * repeatedly drops/re-shows the displayed video mid-browse. (1) The two [ExoPlayer]
 * instances themselves are pooled via [VideoPlayerPool] rather than built/released per
 * browsed game. (2) Each pooled player gets its own dedicated [PlayerView], created once
 * in [AndroidView]'s `factory` with `player` assigned there and *never reassigned again* -
 * earlier attempts still called `playerView.player = x` on every transition (whether or
 * not the View/Player objects themselves were freshly built), which makes ExoPlayer
 * renegotiate the decoder's output Surface on every swap; real, measurable work, not
 * "comparatively free" the way a plain View property write normally is. (3) Only a
 * Compose-level [alpha] toggles which of the two PlayerViews is visible - not the
 * underlying `View.visibility`, since a hidden/`GONE` `SurfaceView` (PlayerView's default
 * video output) can itself tear down and recreate its Surface, reintroducing the exact
 * cost this is trying to avoid. (4) That default `SurfaceView` output is itself swapped
 * for a `TextureView` (`res/layout/widget_video_player_view.xml`'s `app:surface_type`,
 * inflated in `factory` rather than constructing `PlayerView(ctx)` directly - there's no
 * programmatic setter) - two overlapping `SurfaceView`s distinguished only by Compose
 * `alpha` don't reliably composite in view z-order (each renders via its own separate
 * hardware surface), confirmed on-device as the hidden one intermittently winning: the
 * previous video visibly frozen on screen while the newly-promoted one's audio plays.
 * `TextureView` draws as an ordinary View, so it always respects Compose's alpha/z-order.
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
    val playerPool = remember { VideoPlayerPool(context) }

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
            playerPool = playerPool,
            videoPath = content.path,
            settings = VideoTransitionSettings(content.delaySeconds, content.audioEnabled, content.loopEnabled),
            displayedSlot = displayedSlot,
            onPlaybackEvent = onPlaybackEvent,
        )
    }

    LaunchedEffect(displayedPlayer, content.audioEnabled) {
        displayedPlayer?.volume = if (content.audioEnabled) 1f else 0f
    }

    DisposableEffect(Unit) {
        onDispose {
            // The real ExoPlayer.release() calls only ever happen here, once, when this
            // widget is genuinely removed from the canvas - see VideoPlayerPool's kdoc.
            // release() doesn't reliably re-fire onIsPlayingChanged, so without this
            // explicit call a video that becomes ineligible (browsed away from, widget
            // removed) could leave the last "true" stuck, permanently ducking background
            // music - see VideoOverlayScreen's original kdoc for the same reasoning.
            if (displayedPlayer != null) onPlaybackEvent(VideoPlaybackEvent.PlayingChanged(false))
            playerPool.releaseAll()
        }
    }

    val player = displayedPlayer
    val backgroundColor = if (player != null) content.pillarboxMode.toBackgroundColor() else Color.Transparent
    Box(modifier = modifier.background(backgroundColor)) {
        // One PlayerView per pooled player, each permanently bound to that exact player in
        // `factory` and never reassigned - see the class kdoc for why `playerView.player = x`
        // reassignment was itself the remaining stutter source, not merely which View/Player
        // objects get built. Only a Compose-level `alpha` toggles which one is visible; the
        // hidden one still has real (muted-or-stopped) content bound to its own Surface, so
        // nothing about its ExoPlayer<->Surface attachment ever needs to change.
        for (pooledPlayer in playerPool.players) {
            key(pooledPlayer) {
                AndroidView(
                    factory = { ctx ->
                        // Inflated, not `PlayerView(ctx)` directly - see
                        // res/layout/widget_video_player_view.xml's kdoc for why this
                        // needs `app:surface_type="texture_view"`, the one thing about a
                        // PlayerView only settable via XML/AttributeSet.
                        val playerView = LayoutInflater.from(ctx).inflate(R.layout.widget_video_player_view, null)
                        (playerView as PlayerView).apply { this.player = pooledPlayer }
                    },
                    update = { playerView -> playerView.resizeMode = content.scaleMode.toResizeMode() },
                    modifier = Modifier.fillMaxSize().alpha(if (pooledPlayer === player) 1f else 0f),
                )
            }
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
    val loopEnabled: Boolean,
)

/** Bundled for the same [LongParameterList] reason as [DisplayedVideoSlot] - keeps
 * [prepareIncomingPlayer] at 5 parameters instead of 6. */
private class IncomingVideoRequest(
    val path: String,
    val loopEnabled: Boolean,
)

/**
 * Prepares [videoPath] off-screen (muted) and, once it has real decoded frames ready,
 * hands it to [DisplayedVideoSlot.promote] and returns whatever [DisplayedVideoSlot.current]
 * returned beforehand to [playerPool] - see [WidgetVideoContent]'s kdoc for why the
 * promotion happens in one step rather than dropping through "nothing displayed" while
 * this prepares.
 */
private suspend fun transitionToVideo(
    playerPool: VideoPlayerPool,
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
    if (settings.delaySeconds > 0) dropDisplayedVideoForDelay(playerPool, displayedSlot, onPlaybackEvent)

    val ready = CompletableDeferred<Unit>()
    val request = IncomingVideoRequest(videoPath, settings.loopEnabled)
    val incoming = prepareIncomingPlayer(playerPool, request, displayedSlot, ready, onPlaybackEvent)

    try {
        delay(settings.delaySeconds * MILLIS_PER_SECOND)
        incoming.play()
        ready.await()

        incoming.volume = if (settings.audioEnabled) 1f else 0f
        val outgoing = displayedSlot.current()
        displayedSlot.promote(incoming)
        onPlaybackEvent(VideoPlaybackEvent.PlayingChanged(true))
        onPlaybackEvent(VideoPlaybackEvent.Started(videoPath))
        outgoing?.let(playerPool::returnToPool)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (expectedError: PlaybackException) {
        // Already reported via the listener's onPlayerError above (an Error event) - fall
        // through to cleanup below, leaving whatever was already displayed (if anything)
        // playing untouched rather than dropping through to the widget canvas. No
        // PlayingChanged here: the outgoing player (if any) is still genuinely playing.
    } finally {
        if (displayedSlot.current() !== incoming) playerPool.returnToPool(incoming)
    }
}

/**
 * Returns whatever's currently displayed (if anything) to [playerPool] and clears the
 * slot, dropping back to the widget canvas - see the call site in [transitionToVideo] for
 * why this only runs when there's a nonzero delay to wait out. A cheap `ExoPlayer.stop()`
 * via [VideoPlayerPool.returnToPool], not a teardown - see that class's kdoc.
 */
private fun dropDisplayedVideoForDelay(
    playerPool: VideoPlayerPool,
    displayedSlot: DisplayedVideoSlot,
    onPlaybackEvent: (VideoPlaybackEvent) -> Unit,
) {
    val outgoing = displayedSlot.current() ?: return
    displayedSlot.clear()
    onPlaybackEvent(VideoPlaybackEvent.PlayingChanged(false))
    playerPool.returnToPool(outgoing)
}

/**
 * Borrows a muted, prepared-but-not-yet-playing [ExoPlayer] from [playerPool] for
 * [request], wired so that [ready] completes once it actually starts rendering frames
 * (or completes exceptionally on a player error) - the readiness signal the caller awaits
 * before promoting it to the displayed player. [displayedSlot] is polled on every
 * `isPlaying` change (not captured once) so the listener only forwards to
 * [onPlaybackEvent] once this exact player instance has actually been promoted - see
 * [WidgetVideoContent]'s kdoc.
 *
 * A [request] with [IncomingVideoRequest.loopEnabled] false reaching [Player.STATE_ENDED]
 * is treated as "done displaying," not "hold on the last frame": [displayedSlot] is cleared
 * (dropping the widget back to a transparent view of whatever's behind it, since
 * [WidgetVideoContent]'s background only ever renders while something is displayed) and the
 * player is returned to [playerPool], the same cleanup [dropDisplayedVideoForDelay] does for
 * a Start Delay wait - a persistent frozen frame (or an opaque pillarbox color sitting over
 * the canvas indefinitely) reads as a broken widget, not a finished one.
 */
private fun prepareIncomingPlayer(
    playerPool: VideoPlayerPool,
    request: IncomingVideoRequest,
    displayedSlot: DisplayedVideoSlot,
    ready: CompletableDeferred<Unit>,
    onPlaybackEvent: (VideoPlaybackEvent) -> Unit,
): ExoPlayer =
    playerPool.borrow(exclude = displayedSlot.current(), loopEnabled = request.loopEnabled) { incoming ->
        incoming.volume = 0f
        incoming.setMediaItem(MediaItem.fromUri(Uri.fromFile(File(request.path))))
        incoming.prepare()
        object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                if (incoming === displayedSlot.current()) {
                    onPlaybackEvent(VideoPlaybackEvent.PlayingChanged(playing))
                    if (playing) {
                        onPlaybackEvent(VideoPlaybackEvent.Started(request.path))
                    } else if (!request.loopEnabled && incoming.playbackState == Player.STATE_ENDED) {
                        displayedSlot.clear()
                        playerPool.returnToPool(incoming)
                    }
                }
                if (playing) ready.complete(Unit)
            }

            override fun onPlayerError(error: PlaybackException) {
                onPlaybackEvent(VideoPlaybackEvent.Error(request.path, error.message ?: error.toString()))
                ready.completeExceptionally(error)
            }
        }
    }

/**
 * A small reusable pool of exactly two [ExoPlayer] instances, held for [WidgetVideoContent]'s
 * whole lifetime rather than built/released on every browsed game. `ExoPlayer.Builder(
 * context).build()` initializes renderers/audio tracks, and `ExoPlayer.release()` tears
 * them down again - both heavy enough that doing either on every game-browse event could
 * visibly stutter a concurrently-running logo slide animation, confirmed worst with a
 * nonzero Start Delay configured (which synchronously tears the *displayed* player down
 * the moment a new game is browsed, via [dropDisplayedVideoForDelay], rather than only
 * once buffering finishes). [borrow]/[returnToPool] replace those two calls with the much
 * cheaper `ExoPlayer.stop()`/`setMediaItem()`/`prepare()` for every ordinary transition -
 * a real `release()` only ever happens once each, in [releaseAll], when this composable
 * itself leaves composition.
 *
 * Since the pool only ever holds two players and at most one is ever "displayed" at a
 * time, [borrow]'s `exclude` parameter unambiguously picks the other one.
 */
private class VideoPlayerPool(context: Context) {
    /** Exposed so [WidgetVideoContent] can give each one its own permanently-bound
     * [PlayerView] - see that composable's kdoc. */
    val players = List(POOL_SIZE) { ExoPlayer.Builder(context).build() }
    private val attachedListeners = mutableMapOf<ExoPlayer, Player.Listener>()

    /**
     * Hands back whichever pooled player isn't [exclude], reset via `stop()`/
     * `clearMediaItems()` first so it starts from a clean slate, then configured by
     * [configure] (set the media item, build a fresh listener, call `prepare()`) - a
     * fresh listener every borrow, rather than accumulating one per borrow on the same
     * long-lived player instance, replaces whatever listener the previous borrow attached.
     */
    fun borrow(
        exclude: ExoPlayer?,
        loopEnabled: Boolean,
        configure: (ExoPlayer) -> Player.Listener,
    ): ExoPlayer {
        val player = players.first { it !== exclude }
        player.stop()
        player.clearMediaItems()
        attachedListeners.remove(player)?.let(player::removeListener)
        val listener = configure(player)
        attachedListeners[player] = listener
        player.addListener(listener)
        player.repeatMode = if (loopEnabled) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        return player
    }

    /** Cheaply idles a player that's no longer displayed/needed right now, keeping it
     * around in the pool for the next [borrow] rather than releasing it. */
    fun returnToPool(player: ExoPlayer) {
        player.stop()
    }

    /** The only place `ExoPlayer.release()` is actually called - see the class kdoc. */
    fun releaseAll() {
        players.forEach { it.release() }
    }

    private companion object {
        const val POOL_SIZE = 2
    }
}

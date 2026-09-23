package com.esde.companion.ui.widgets

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlin.random.Random

private const val PAN_ZOOM_MAX_SCALE = 1.12f
private const val PAN_ZOOM_DURATION_MS = 20_000

/** Which diagonal the ambient pan drifts toward - each axis independently +1/-1, re-picked
 * once per full drift cycle by [rememberPanZoomHandle] (see its kdoc). */
internal data class PanZoomDirection(val dirX: Int, val dirY: Int)

internal fun randomPanZoomDirection(): PanZoomDirection =
    PanZoomDirection(
        dirX = if (Random.nextBoolean()) 1 else -1,
        dirY = if (Random.nextBoolean()) 1 else -1,
    )

/**
 * Pan translation (x, y pixels) at the current [scale] so the zoomed image never reveals
 * empty space beyond its bounds - re-derives `overflow = size * (scale - 1) / 2` per axis,
 * evaluated at *this frame's* scale (not a fixed max), so the edge stays flush at every
 * point along the easing curve, not just at the animation's endpoints.
 */
internal fun panZoomTranslation(
    scale: Float,
    size: IntSize,
    direction: PanZoomDirection,
): Pair<Float, Float> {
    val overflowX = size.width * (scale - 1f) / 2f
    val overflowY = size.height * (scale - 1f) / 2f
    return direction.dirX * overflowX to direction.dirY * overflowY
}

/**
 * [rememberPanZoomHandle]'s three outputs: [current]/[previous] are the independent transforms
 * for `CrossfadeAsyncImage`'s incoming/outgoing layers, and [onImagePromoted] is the trigger
 * the caller must invoke - synchronously, from the exact same call that promotes a new image
 * to the visible layer - to freeze the outgoing look and reset the incoming one. All three are
 * no-ops when pan/zoom is disabled.
 */
internal data class PanZoomHandle(
    val current: Modifier,
    val previous: Modifier,
    val onImagePromoted: () -> Unit,
)

/**
 * Continuous slow zoom/pan ("Ken Burns") transform for opaque backdrop-style image widgets
 * - see [com.esde.companion.domain.model.supportsPanZoom]/`panZoomActive` for eligibility.
 *
 * `progress` is computed purely from elapsed frame time (via [withFrameMillis]) relative to a
 * `cycleAnchorMillis` timestamp, rather than driven by an [androidx.compose.animation.core.Animatable]/
 * `animateTo` loop. This matters because [PanZoomHandle.onImagePromoted] must be callable
 * *synchronously* - with zero frame of lag - from `CrossfadeAsyncImage`'s own coroutine, at the
 * exact instant it promotes a new model to the visible layer (see that composable's
 * `onModelPromoted` kdoc for why that timing has to be exact). A plain state write
 * (`cycleAnchorMillis = elapsedMillis`) achieves that trivially; an `Animatable`-based design
 * would need a *second* coroutine to call `snapTo`/`animateTo` on demand, and interrupting one
 * coroutine's in-flight `animateTo` from another coroutine on the same `Animatable` cancels
 * that mutation outright - silently killing the continuous drift loop after the first image
 * change. A single frame-clock coroutine plus plain state reads/writes sidesteps that
 * entirely: nothing ever contends over exclusive access to an animation.
 *
 * [PanZoomHandle.onImagePromoted] first snapshots the *current* live scale/direction into
 * `frozenScale`/`frozenDirection` - a static, non-animating look applied to
 * [PanZoomHandle.previous] (`CrossfadeAsyncImage`'s outgoing layer) for the remainder of that
 * crossfade - then resets `cycleAnchorMillis` (restarting the `0 -> 1 -> 0` cycle from scratch)
 * and picks a fresh random direction for [PanZoomHandle.current] (the incoming layer, fading in
 * from `alpha = 0f`). Because the reset is applied only to the incoming layer and happens at
 * the exact moment that layer starts fading in, it's never seen as a visible snap - every
 * newly-shown image reliably starts from a clean, unzoomed frame while the outgoing image holds
 * whatever look it already had.
 */
@Composable
internal fun rememberPanZoomHandle(enabled: Boolean): PanZoomHandle {
    if (!enabled) return PanZoomHandle(current = Modifier, previous = Modifier, onImagePromoted = {})

    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var direction by remember { mutableStateOf(randomPanZoomDirection()) }
    var cycleAnchorMillis by remember { mutableLongStateOf(0L) }
    var elapsedMillis by remember { mutableLongStateOf(0L) }
    var frozenScale by remember { mutableFloatStateOf(1f) }
    var frozenDirection by remember { mutableStateOf(direction) }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis { elapsedMillis = it }
        }
    }

    fun liveProgress(): Float {
        val cyclePosition = (elapsedMillis - cycleAnchorMillis) % (2 * PAN_ZOOM_DURATION_MS)
        val linear =
            if (cyclePosition < PAN_ZOOM_DURATION_MS) {
                cyclePosition.toFloat() / PAN_ZOOM_DURATION_MS
            } else {
                1f - (cyclePosition - PAN_ZOOM_DURATION_MS).toFloat() / PAN_ZOOM_DURATION_MS
            }
        return FastOutSlowInEasing.transform(linear)
    }

    val currentModifier =
        Modifier
            .onSizeChanged { boxSize = it }
            .graphicsLayer {
                val scale = 1f + liveProgress() * (PAN_ZOOM_MAX_SCALE - 1f)
                scaleX = scale
                scaleY = scale
                val (tx, ty) = panZoomTranslation(scale, boxSize, direction)
                translationX = tx
                translationY = ty
            }

    val previousModifier =
        Modifier.graphicsLayer {
            scaleX = frozenScale
            scaleY = frozenScale
            val (tx, ty) = panZoomTranslation(frozenScale, boxSize, frozenDirection)
            translationX = tx
            translationY = ty
        }

    val onImagePromoted: () -> Unit = {
        frozenScale = 1f + liveProgress() * (PAN_ZOOM_MAX_SCALE - 1f)
        frozenDirection = direction
        cycleAnchorMillis = elapsedMillis
        direction = randomPanZoomDirection()
    }

    return PanZoomHandle(current = currentModifier, previous = previousModifier, onImagePromoted = onImagePromoted)
}

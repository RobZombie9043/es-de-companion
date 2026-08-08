package com.esde.companion.ui.widgets

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.esde.companion.ui.main.identityKeyOf
import kotlin.random.Random

private const val PAN_ZOOM_MAX_SCALE = 1.12f
private const val PAN_ZOOM_DURATION_MS = 20_000

/** Which diagonal the ambient pan drifts toward - each axis independently +1/-1, picked
 * once per [rememberPanZoomModifier] restart (see its kdoc). */
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
 * Continuous slow zoom/pan ("Ken Burns") transform for opaque backdrop-style image widgets
 * - see [com.esde.companion.domain.model.supportsPanZoom]/`panZoomActive` for eligibility.
 * Restarts (fresh random direction, scale reset to 1f) whenever [model]'s identity changes,
 * mirroring how [AnimatedLogoImage] and [com.esde.companion.ui.main.CrossfadeAsyncImage]
 * already key their own per-model state on [identityKeyOf] - reusing the same [model] value
 * passed to those guarantees they never disagree about what counts as "a new image."
 *
 * Returns a plain [Modifier] rather than wrapping composable, so it composes directly into
 * the [Modifier] chain already passed to `CrossfadeAsyncImage` at the call site in
 * `WidgetCanvas.kt`, with zero changes needed to that shared file.
 */
@Composable
internal fun rememberPanZoomModifier(
    enabled: Boolean,
    model: Any?,
): Modifier {
    if (!enabled) return Modifier

    return key(identityKeyOf(model)) {
        val direction = remember { randomPanZoomDirection() }
        var boxSize by remember { mutableStateOf(IntSize.Zero) }
        val infiniteTransition = rememberInfiniteTransition(label = "panZoom")
        val progress by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(PAN_ZOOM_DURATION_MS, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "panZoomProgress",
        )

        Modifier
            .onSizeChanged { boxSize = it }
            .graphicsLayer {
                val scale = 1f + progress * (PAN_ZOOM_MAX_SCALE - 1f)
                scaleX = scale
                scaleY = scale
                val (tx, ty) = panZoomTranslation(scale, boxSize, direction)
                translationX = tx
                translationY = ty
            }
    }
}

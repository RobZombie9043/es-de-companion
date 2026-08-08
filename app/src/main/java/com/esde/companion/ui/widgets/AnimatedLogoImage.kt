package com.esde.companion.ui.widgets

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import coil3.compose.rememberAsyncImagePainter
import coil3.imageLoader
import coil3.request.SuccessResult
import com.esde.companion.domain.model.LogoTransitionMode
import com.esde.companion.domain.model.NavigationDirection
import com.esde.companion.ui.main.identityKeyOf
import com.esde.companion.ui.main.requestFor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

private const val SCALE_FROM = 0.95f
private const val GLINT_SWEEP_DURATION_MS = 1800
private const val GLINT_PAUSE_MS = 7000L
private const val GLINT_PEAK_ALPHA = 0.35f

/** Alpha of the dark fringe flanking the bright peak (see the colorStops in the glint
 * drawWithContent block) - a plain white highlight mixed via SrcAtop is nearly invisible
 * on logos that are already close to white, since alpha-blending white toward white barely
 * shifts the result. Flanking the peak with a subtle darkening on both sides gives the
 * sweep contrast against *any* base color, including white - the same "chrome shine" look
 * a specular highlight has in real materials, rather than relying on brightness alone. */
private const val GLINT_FRINGE_ALPHA = 0.2f

/** Thickness of the visible glint streak (the gradient's transparent-peak-transparent
 * span), fixed and independent of the widget's own size - see [glintBandOffsets]'s kdoc
 * for why this must be decoupled from [GLINT_TILT_DEGREES]/height, not derived from them. */
private const val GLINT_BAND_WIDTH_PX = 300f

/** Extra margin added on top of [glintBandOffsets]'s exact geometric overshoot
 * requirement, to stay clear of the corner even after Float rounding - the exact value
 * puts the worst-case corner exactly at the gradient's t=1 boundary, which floating-point
 * error could nudge back inside [0,1] by a hair, letting a barely-perceptible sliver
 * through right at rest. */
private const val GLINT_OVERSHOOT_SAFETY_MARGIN_PX = 4f

/** Diagonal tilt of the band, in degrees from vertical. */
private const val GLINT_TILT_DEGREES = 30f
private val GLINT_TILT_RADIANS = Math.toRadians(GLINT_TILT_DEGREES.toDouble()).toFloat()
private val GLINT_TILT_COS = cos(GLINT_TILT_RADIANS)
private val GLINT_TILT_SIN = sin(GLINT_TILT_RADIANS)
private val GLINT_TILT_TAN = tan(GLINT_TILT_RADIANS)

/**
 * Animates logo-style/transparent overlay content (system logos, custom system logos,
 * game marquees) on content change, per [mode]. Unlike [com.esde.companion.ui.main.CrossfadeAsyncImage],
 * this never keeps a previous layer around to blend with - cross-fading two overlapping
 * transparent images causes a visible double-exposure (see CLAUDE.md), so this is always
 * a hard content swap, animated only via offset/scale transforms on the new layer.
 *
 * [direction] is which way the user just navigated (see NavigationDirectionTracker) - Slide
 * enters from the *same* side as travel (pressed Right -> enters from the Right, etc.).
 * When it isn't known (no preceding directional press, a non-directional button, or
 * edit-mode preview, which has no live AppState to derive a direction from at all), Slide
 * falls back to a scale-in instead of guessing a side to slide from.
 *
 * Like CrossfadeAsyncImage, [model] is pre-decoded via imageLoader.execute() before being
 * swapped in, so even [LogoTransitionMode.None] avoids a blank-frame flash on cache
 * misses - a small improvement over the plain AsyncImage this replaces.
 *
 * [glintEnabled] (this widget's own per-widget Logo Glint toggle, Configure Widget dialog) is
 * an independent ambient loop - a periodic diagonal light-sweep - orthogonal to [mode]: it
 * can run alongside any entrance
 * transition, since it isn't triggered by content changes the way [mode] is. Its pause/sweep
 * cycle restarts on every content change (keyed on [model]'s identity, not just
 * [glintEnabled]), so each freshly-shown image gets its own first sweep rather than picking
 * up wherever the previous image's cycle happened to be - that first sweep waits
 * [durationMillis] (the same duration [mode]'s own Slide/Scale entrance animation runs over)
 * before firing, so it doesn't race across a logo still sliding/scaling into place; only
 * *repeat* sweeps on the same image (while it's still displayed) wait [GLINT_PAUSE_MS]
 * apart. See
 * [glintBandOffsets] for the sweep geometry.
 */
@Composable
fun AnimatedLogoImage(
    model: Any?,
    contentDescription: String?,
    contentScale: ContentScale,
    mode: LogoTransitionMode,
    direction: NavigationDirection? = null,
    glintEnabled: Boolean = false,
    modifier: Modifier = Modifier,
    durationMillis: Int = 500,
) {
    val context = LocalContext.current
    var currentModel by remember { mutableStateOf(model) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }
    val glintProgress = remember { Animatable(0f) }

    // Keyed on identityKeyOf(model) (not currentModel) as well as glintEnabled, so scrolling
    // to a new system/game restarts this loop from scratch - without that, the pause/sweep
    // cycle ran independently of content changes, and the timing of the first sweep on a
    // freshly-shown image was whatever was left over from the previous image's cycle.
    // Snapping to 0f immediately on restart clears any in-flight sweep the old image was
    // mid-way through, so a model change never leaves a partial streak frozen on screen.
    // The first sweep for each image waits durationMillis - the same duration the Slide/Scale
    // entrance transition (below) animates over - before firing, so the glint doesn't race
    // across a logo that's still sliding/scaling into place. Only the *repeat* sweeps on the
    // same image (while it keeps being displayed, well after the entrance settles) wait
    // GLINT_PAUSE_MS apart.
    LaunchedEffect(glintEnabled, identityKeyOf(model)) {
        glintProgress.snapTo(0f)
        if (!glintEnabled) return@LaunchedEffect
        delay(durationMillis.toLong())
        while (true) {
            glintProgress.animateTo(1f, animationSpec = tween(GLINT_SWEEP_DURATION_MS, easing = LinearEasing))
            delay(GLINT_PAUSE_MS)
            glintProgress.snapTo(0f)
        }
    }

    LaunchedEffect(identityKeyOf(model)) {
        if (identityKeyOf(model) == identityKeyOf(currentModel)) return@LaunchedEffect

        // A model that fails to load (e.g. a system with no bundled logo asset) resolves
        // to null here, same as a genuinely absent model - so a missing logo clears to
        // blank instead of leaving the previous system/game's logo stuck on screen.
        val resolvedModel =
            model?.takeIf {
                val result = context.imageLoader.execute(requestFor(context, it))
                result is SuccessResult
            }

        currentModel = resolvedModel

        if (resolvedModel == null) {
            offsetX.snapTo(0f)
            offsetY.snapTo(0f)
            scale.snapTo(1f)
            return@LaunchedEffect
        }

        when (mode) {
            LogoTransitionMode.None -> {
                offsetX.snapTo(0f)
                offsetY.snapTo(0f)
                scale.snapTo(1f)
            }

            LogoTransitionMode.Slide -> {
                if (direction == null) {
                    // No direction to slide from - scale-in instead of guessing a side.
                    offsetX.snapTo(0f)
                    offsetY.snapTo(0f)
                    scale.snapTo(SCALE_FROM)
                    scale.animateTo(1f, animationSpec = tween(durationMillis))
                } else {
                    val (startX, startY) = slideStartOffset(direction, boxSize)
                    offsetX.snapTo(startX)
                    offsetY.snapTo(startY)
                    scale.snapTo(1f)
                    coroutineScope {
                        launch { offsetX.animateTo(0f, animationSpec = tween(durationMillis)) }
                        launch { offsetY.animateTo(0f, animationSpec = tween(durationMillis)) }
                    }
                }
            }

            LogoTransitionMode.Scale -> {
                offsetX.snapTo(0f)
                offsetY.snapTo(0f)
                scale.snapTo(SCALE_FROM)
                scale.animateTo(1f, animationSpec = tween(durationMillis))
            }
        }
    }

    Box(
        modifier =
            modifier
                .onSizeChanged { boxSize = it }
                .clipToBounds(),
    ) {
        currentModel?.let { curModel ->
            key(identityKeyOf(curModel)) {
                val painter =
                    rememberAsyncImagePainter(
                        model = requestFor(context, curModel),
                        contentScale = contentScale,
                    )
                Image(
                    painter = painter,
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationX = offsetX.value
                                translationY = offsetY.value
                                scaleX = scale.value
                                scaleY = scale.value
                                // Offscreen compositing is required so the glint gradient below
                                // composites against this layer's own drawn alpha (the logo's
                                // actual opaque pixel shape) via BlendMode.SrcAtop, rather than
                                // against whatever's already drawn behind it in the window.
                                if (glintEnabled) compositingStrategy = CompositingStrategy.Offscreen
                            }
                            .drawWithContent {
                                drawContent()
                                if (glintEnabled) {
                                    val (start, end) = glintBandOffsets(glintProgress.value, size)
                                    drawRect(
                                        brush =
                                            Brush.linearGradient(
                                                colorStops =
                                                    arrayOf(
                                                        0f to Color.Transparent,
                                                        0.35f to Color.Black.copy(alpha = GLINT_FRINGE_ALPHA),
                                                        0.5f to Color.White.copy(alpha = GLINT_PEAK_ALPHA),
                                                        0.65f to Color.Black.copy(alpha = GLINT_FRINGE_ALPHA),
                                                        1f to Color.Transparent,
                                                    ),
                                                start = start,
                                                end = end,
                                            ),
                                        blendMode = BlendMode.SrcAtop,
                                    )
                                }
                            },
                )
            }
        }
    }
}

/** Where a Slide-in should start from, in (x, y) pixels relative to its resting position
 * (0, 0) - the same side as [direction] (travel Right -> enters from the Right, etc.).
 * Only called once [direction] is known to be non-null - see the null fallback to a
 * scale-in in the caller above. Extracted as a plain function (no Composable/Compose
 * types beyond IntSize) so it's unit-testable without a Compose test environment. */
internal fun slideStartOffset(
    direction: NavigationDirection,
    boxSize: IntSize,
): Pair<Float, Float> =
    when (direction) {
        NavigationDirection.Left -> -boxSize.width.toFloat() to 0f
        NavigationDirection.Right -> boxSize.width.toFloat() to 0f
        NavigationDirection.Up -> 0f to -boxSize.height.toFloat()
        NavigationDirection.Down -> 0f to boxSize.height.toFloat()
    }

/**
 * Diagonal glint sweep band endpoints at [progress] (0f..1f) across a layer of [size].
 *
 * The band is a short, fixed-length segment ([GLINT_BAND_WIDTH_PX], independent of the
 * widget's own size) tilted [GLINT_TILT_DEGREES] from vertical and centered on the
 * widget's vertical midpoint - a linear gradient's visible (non-fully-transparent) extent
 * is exactly the distance between its two anchor points, so keeping that distance fixed
 * keeps the streak a consistent thin band regardless of how tall the widget is. The
 * previous implementation anchored the two points at the widget's top and bottom edges
 * directly (`0f` to `size.height`), which made the band's visible width scale with the
 * widget's height instead of staying thin, and covered much more of the logo than
 * intended on tall widgets.
 *
 * [progress] sweeps the band's center from fully off the left edge to fully off the right
 * edge. The overshoot margin added on both ends must be large enough that every corner of
 * the widget projects to t &lt;= 0 (before the gradient) or t &gt;= 1 (past it) at rest -
 * otherwise a corner can fall inside the gradient's visible [0f, 1f] range between sweeps,
 * showing a faint residual glint instead of none at all.
 *
 * The exact worst case is the corner diagonally opposite the tilt's lean (with this
 * tilt/travel direction, that's the top-left corner (0, 0)): projecting it onto the band's
 * axis and solving for where it lands exactly at t=1 gives
 * `overshoot = halfBand / cos(tilt) + (height / 2) * tan(tilt)`. A dividing-by-cos term is
 * required, not just `halfBand` on its own - an earlier version of this formula used
 * `halfBand + height * tan(tilt)`, which has enough slack for a thin band relative to the
 * widget's height, but becomes provably insufficient once the band's half-width approaches
 * or exceeds the widget's own height (e.g. widening [GLINT_BAND_WIDTH_PX] enough triggers
 * this - confirmed as a real, if barely-perceptible, regression via the corner-projection
 * unit test). [GLINT_OVERSHOOT_SAFETY_MARGIN_PX] pads this further so Float rounding at the
 * exact t=1 boundary can't nudge a corner back inside [0, 1].
 */
internal fun glintBandOffsets(
    progress: Float,
    size: Size,
): Pair<Offset, Offset> {
    val halfBand = GLINT_BAND_WIDTH_PX / 2f
    val overshoot = halfBand / GLINT_TILT_COS + (size.height / 2f) * GLINT_TILT_TAN + GLINT_OVERSHOOT_SAFETY_MARGIN_PX
    val travel = size.width + 2 * overshoot
    val centerX = -overshoot + progress * travel
    val centerY = size.height / 2f
    return Offset(centerX - halfBand * GLINT_TILT_COS, centerY - halfBand * GLINT_TILT_SIN) to
        Offset(centerX + halfBand * GLINT_TILT_COS, centerY + halfBand * GLINT_TILT_SIN)
}

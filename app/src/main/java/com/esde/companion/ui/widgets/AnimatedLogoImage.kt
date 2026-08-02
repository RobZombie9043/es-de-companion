package com.esde.companion.ui.widgets

import androidx.compose.animation.core.Animatable
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
import kotlinx.coroutines.launch

private const val SCALE_FROM = 0.95f

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
 */
@Composable
fun AnimatedLogoImage(
    model: Any?,
    contentDescription: String?,
    contentScale: ContentScale,
    mode: LogoTransitionMode,
    direction: NavigationDirection? = null,
    modifier: Modifier = Modifier,
    durationMillis: Int = 500,
) {
    val context = LocalContext.current
    var currentModel by remember { mutableStateOf(model) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }

    LaunchedEffect(identityKeyOf(model)) {
        if (identityKeyOf(model) == identityKeyOf(currentModel)) return@LaunchedEffect

        // A model that fails to load (e.g. a system with no bundled logo asset) resolves
        // to null here, same as a genuinely absent model - so a missing logo clears to
        // blank instead of leaving the previous system/game's logo stuck on screen.
        val resolvedModel = model?.takeIf {
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
        modifier = modifier
            .onSizeChanged { boxSize = it }
            .clipToBounds(),
    ) {
        currentModel?.let { curModel ->
            key(identityKeyOf(curModel)) {
                val painter = rememberAsyncImagePainter(
                    model = requestFor(context, curModel),
                    contentScale = contentScale,
                )
                Image(
                    painter = painter,
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = offsetX.value
                            translationY = offsetY.value
                            scaleX = scale.value
                            scaleY = scale.value
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
internal fun slideStartOffset(direction: NavigationDirection, boxSize: IntSize): Pair<Float, Float> =
    when (direction) {
        NavigationDirection.Left -> -boxSize.width.toFloat() to 0f
        NavigationDirection.Right -> boxSize.width.toFloat() to 0f
        NavigationDirection.Up -> 0f to -boxSize.height.toFloat()
        NavigationDirection.Down -> 0f to boxSize.height.toFloat()
    }

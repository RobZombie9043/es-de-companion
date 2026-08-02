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
import com.esde.companion.ui.main.identityKeyOf
import com.esde.companion.ui.main.requestFor

private const val SCALE_FROM = 0.95f

/**
 * Animates logo-style/transparent overlay content (system logos, custom system logos,
 * game marquees) on content change, per [mode]. Unlike [com.esde.companion.ui.main.CrossfadeAsyncImage],
 * this never keeps a previous layer around to blend with - cross-fading two overlapping
 * transparent images causes a visible double-exposure (see CLAUDE.md), so this is always
 * a hard content swap, animated only via offset/scale transforms on the new layer.
 *
 * Slide direction is hardcoded to "from the right" for now - parsing actual navigation
 * direction from the ES-DE log is future work, not implemented here.
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
    modifier: Modifier = Modifier,
    durationMillis: Int = 500,
) {
    val context = LocalContext.current
    var currentModel by remember { mutableStateOf(model) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    val offsetX = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }

    LaunchedEffect(identityKeyOf(model)) {
        if (identityKeyOf(model) == identityKeyOf(currentModel)) return@LaunchedEffect

        if (model != null) {
            val result = context.imageLoader.execute(requestFor(context, model))
            if (result !is SuccessResult) return@LaunchedEffect
        }

        currentModel = model

        when (mode) {
            LogoTransitionMode.None -> {
                offsetX.snapTo(0f)
                scale.snapTo(1f)
            }

            LogoTransitionMode.Slide -> {
                offsetX.snapTo(boxSize.width.toFloat())
                scale.snapTo(1f)
                offsetX.animateTo(0f, animationSpec = tween(durationMillis))
            }

            LogoTransitionMode.Scale -> {
                offsetX.snapTo(0f)
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
                            scaleX = scale.value
                            scaleY = scale.value
                        },
                )
            }
        }
    }
}

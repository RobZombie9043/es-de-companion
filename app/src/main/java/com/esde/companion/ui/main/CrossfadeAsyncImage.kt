package com.esde.companion.ui.main

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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.rememberAsyncImagePainter
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import java.io.File

/**
 * Crossfades from whatever model was previously shown to [model], never showing a
 * blank/partial frame.
 *
 * The earlier version of this composable swapped the visible model directly and let
 * Coil fetch+decode it live behind the new layer. That's invisible for already-cached
 * images (the fetch resolves near-instantly) but leaves the new layer in a Loading
 * state - rendering nothing - for however long a real fetch/decode takes, which is
 * exactly the gap that showed as a white flash on first-time navigation.
 *
 * Instead, [model] is first fully decoded via a direct imageLoader.execute() call and
 * only swapped into the visible layer once that succeeds. From then on,
 * rememberAsyncImagePainter for that same model hits Coil's memory cache immediately -
 * the same fast path already confirmed smooth for re-navigation - so every navigation
 * behaves like a cache hit from the crossfade's point of view.
 *
 * [model]'s identity for both the crossfade trigger and Coil's memory cache key is
 * derived via [identityKeyOf] rather than trusting [model] directly. A plain `File`'s
 * equals()/hashCode() is path-only, so replacing a file's *content* at the same
 * downloaded_media path (re-scraped box art, swapped screenshot, etc.) would otherwise
 * be invisible both to this composable's LaunchedEffect(model)/key(model) triggers and
 * to Coil's default memory cache key - the old bitmap would keep showing indefinitely.
 * Folding File.lastModified() into the identity fixes both at once: a content swap now
 * looks like a genuinely new model, so it re-triggers the crossfade and gets a fresh
 * cache entry. Non-File models (bundled asset path strings) are never mutated at a
 * fixed path, so they keep using their own value as-is.
 */
@Composable
fun CrossfadeAsyncImage(
    model: Any?,
    contentDescription: String?,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
    durationMillis: Int = 250,
) {
    val context = LocalContext.current
    var previousModel by remember { mutableStateOf<Any?>(null) }
    var currentModel by remember { mutableStateOf(model) }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(identityKeyOf(model)) {
        if (identityKeyOf(model) == identityKeyOf(currentModel)) return@LaunchedEffect

        if (model == null) {
            previousModel = currentModel
            currentModel = null
            alpha.snapTo(0f)
            alpha.animateTo(1f, animationSpec = tween(durationMillis))
            previousModel = null
            return@LaunchedEffect
        }

        val request = requestFor(context, model)
        val result = context.imageLoader.execute(request)

        if (result !is SuccessResult) {
            return@LaunchedEffect
        }

        previousModel = currentModel
        currentModel = model
        alpha.snapTo(0f)
        alpha.animateTo(1f, animationSpec = tween(durationMillis))
        previousModel = null
    }

    Box(modifier = modifier) {
        previousModel?.let { prevModel ->
            key(identityKeyOf(prevModel)) {
                val painter = rememberAsyncImagePainter(
                    model = requestFor(context, prevModel),
                    contentScale = contentScale,
                )
                Image(
                    painter = painter,
                    contentDescription = null,
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

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
                        .alpha(alpha.value),
                )
            }
        }
    }
}
package com.esde.companion.ui.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
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
import coil3.request.SuccessResult

/**
 * Reproduces android.view.animation.DecelerateInterpolator's default (factor = 1.0)
 * curve - fast start, easing into the landing point - since Compose has no built-in
 * equivalent and the legacy Glide-based transition used it explicitly.
 */
private val DecelerateEasing = Easing { fraction -> 1f - (1f - fraction) * (1f - fraction) }

/**
 * Per-layer modifiers for [CrossfadeAsyncImage]'s outgoing ([previous]) and incoming
 * ([current]) `Image`s.
 */
data class CrossfadeLayerModifiers(
    val previous: Modifier = Modifier,
    val current: Modifier = Modifier,
)

/**
 * [durationMillis]/[layerModifiers]/[onModelPromoted] bundled into one type purely to keep
 * [CrossfadeAsyncImage]'s own parameter count under this project's `LongParameterList` limit,
 * same reasoning as e.g. `SelfHealConfig`/`WidgetContentDisplayOptions` - not a single
 * logically-related trio otherwise.
 *
 * [onModelPromoted] fires with the new model exactly when [CrossfadeAsyncImage] promotes it
 * to the visible "current" layer and starts its fade-in - i.e. only once the pre-decode
 * described in [CrossfadeAsyncImage]'s own kdoc has actually succeeded, not merely when the
 * caller's `model` argument changes. It fires synchronously, inline in the same coroutine
 * that does the promotion - never suspended/deferred to a later frame - so a caller whose
 * per-layer transform needs to reset in lockstep with the *visual* transition (see
 * `PanZoomImage.kt`'s `rememberPanZoomHandle`) can do so with zero lag. Reacting instead to
 * the caller's own `model` argument changing doesn't work: for an uncached image there's a
 * real decode delay between "model requested" and "model promoted," so resetting on the raw
 * `model` change would reset the transform on the *still-displayed outgoing* image well
 * before the fade actually begins.
 */
data class CrossfadeTransitionOptions(
    val durationMillis: Int = 250,
    val layerModifiers: CrossfadeLayerModifiers = CrossfadeLayerModifiers(),
    val onModelPromoted: (Any?) -> Unit = {},
)

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
 *
 * When [CrossfadeTransitionOptions.durationMillis] is 0 or less (ImageTransitionMode.None),
 * alpha snaps straight to its end value instead of animating - this still goes through the
 * pre-decode-then-swap path above, so "no visible transition" doesn't reopen the blank-frame
 * flash the whole mechanism exists to prevent.
 *
 * The outgoing (previous) layer stays fully opaque - since both layers are the same size,
 * a static backdrop plus a fading-in foreground already looks like a clean crossfade.
 *
 * [CrossfadeTransitionOptions.layerModifiers] are applied to the outgoing/incoming `Image`
 * individually, in addition to [modifier] (which stays on the wrapping `Box` and so still
 * applies equally to both layers - sizing/blur/etc.). Defaults to a no-op
 * [CrossfadeLayerModifiers], so every call site that doesn't pass it renders exactly as
 * before. This exists for callers (see `PanZoomImage.kt`'s `rememberPanZoomLayerModifiers`)
 * that need the two layers to carry genuinely independent transforms rather than one shared
 * one - e.g. an outgoing layer frozen at its last look while the incoming layer starts fresh.
 */
@Composable
fun CrossfadeAsyncImage(
    model: Any?,
    contentDescription: String?,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
    transitionOptions: CrossfadeTransitionOptions = CrossfadeTransitionOptions(),
) {
    val durationMillis = transitionOptions.durationMillis
    val layerModifiers = transitionOptions.layerModifiers
    val onModelPromoted = transitionOptions.onModelPromoted
    val context = LocalContext.current
    var previousModel by remember { mutableStateOf<Any?>(null) }
    var currentModel by remember { mutableStateOf(model) }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(identityKeyOf(model)) {
        if (identityKeyOf(model) == identityKeyOf(currentModel)) return@LaunchedEffect

        suspend fun animateIn() {
            if (durationMillis <= 0) {
                alpha.snapTo(1f)
                return
            }
            alpha.snapTo(0f)
            alpha.animateTo(1f, animationSpec = tween(durationMillis, easing = DecelerateEasing))
        }

        if (model == null) {
            previousModel = currentModel
            currentModel = null
            onModelPromoted(null)
            animateIn()
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
        onModelPromoted(model)
        animateIn()
        previousModel = null
    }

    Box(modifier = modifier) {
        previousModel?.let { prevModel ->
            CrossfadeLayer(
                model = prevModel,
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize().then(layerModifiers.previous),
            )
        }

        currentModel?.let { curModel ->
            CrossfadeLayer(
                model = curModel,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize().alpha(alpha.value).then(layerModifiers.current),
            )
        }
    }
}

@Composable
private fun CrossfadeLayer(
    model: Any,
    contentDescription: String?,
    contentScale: ContentScale,
    modifier: Modifier,
) {
    key(identityKeyOf(model)) {
        val painter =
            rememberAsyncImagePainter(
                model = requestFor(LocalContext.current, model),
                contentScale = contentScale,
            )
        Image(
            painter = painter,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier,
        )
    }
}

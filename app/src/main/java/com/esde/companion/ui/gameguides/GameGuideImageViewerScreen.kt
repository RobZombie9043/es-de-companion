package com.esde.companion.ui.gameguides

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.esde.companion.ui.main.CrossfadeAsyncImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 4f

// How far past MIN_SCALE/MAX_SCALE a live pinch may stretch before the release-time spring
// below pulls it back - without this, scale/offset never leave [MIN_SCALE, MAX_SCALE] during
// the gesture (hard-clamped, same as before), so there'd be nothing out-of-bounds left to
// visibly spring back from on release.
private const val SCALE_OVERSCROLL = 0.5f

// Same damping/stiffness family as ui/main/MainScreen.kt's DRAWER_SETTLE_SPRING - the house
// precedent for a physical "snap back into place" motion.
private fun <T> zoomSnapSpring(): SpringSpec<T> {
    return spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
}

/**
 * Renders a [com.esde.companion.domain.model.GameGuideFormat.Image] guide - the simplest of
 * the three viewer branches, since a single static image has no pages/reading progress to
 * persist (unlike [GameGuidePdfViewerScreen]/the text viewer). Pinch-zoom only, no paging.
 */
@Composable
fun GameGuideImageViewerScreen(
    state: GameGuidesUiState.Viewing,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onClose)

    val scale = remember { Animatable(MIN_SCALE) }
    val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val coroutineScope = rememberCoroutineScope()

    val imageModifier =
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                translationX = offset.value.x
                translationY = offset.value.y
            }
            .pointerInput(Unit) { detectPinchZoomWithSnapBack(scale, offset, coroutineScope) }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // key(...) on the guide's own file path - a fresh mount per guide, never a
        // when-branch remount for the same conceptual image, so CrossfadeAsyncImage's
        // pre-decode-then-swap protection isn't bypassed (see its own kdoc / this project's
        // CLAUDE.md Known Gotchas on that constraint).
        key(state.contentFilePath) {
            CrossfadeAsyncImage(
                model = state.contentFilePath,
                contentDescription = state.guide.title,
                contentScale = ContentScale.Fit,
                modifier = imageModifier,
            )
        }
        Surface(
            modifier = Modifier.align(Alignment.TopEnd).padding(24.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f),
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Exit",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/**
 * A custom gesture loop (not `detectTransformGestures`) - needed to distinguish "the gesture is
 * still live" (`snapTo`, instant, keeps the drag responsive) from "the gesture just ended"
 * (`animateTo` with [zoomSnapSpring], the release-time bounce back into bounds) -
 * `detectTransformGestures`'s own `onGesture` callback has no separate hook for gesture-end,
 * and calling a suspend `animateTo` from inside its per-event callback would stall live event
 * processing for the animation's duration.
 *
 * `AwaitPointerEventScope` (entered via `awaitEachGesture`) is a `@RestrictsSuspension` scope -
 * it can't directly call arbitrary suspend functions like `Animatable.snapTo`/`animateTo`.
 * [coroutineScope]`.launch { }` (a regular, non-suspend call, using the composable-scoped
 * `rememberCoroutineScope` from the caller rather than this `PointerInputScope`'s own) sidesteps
 * that restriction; `snapTo`/`animateTo` for one event/settle are bundled into a single `launch`
 * each so scale-then-offset stays ordered within it.
 */
private suspend fun PointerInputScope.detectPinchZoomWithSnapBack(
    scale: Animatable<Float, AnimationVector1D>,
    offset: Animatable<Offset, AnimationVector2D>,
    coroutineScope: CoroutineScope,
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            val zoomChange = event.calculateZoom()
            val panChange = event.calculatePan()
            if (zoomChange != 1f || panChange != Offset.Zero) {
                val newScale =
                    (scale.value * zoomChange).coerceIn(MIN_SCALE - SCALE_OVERSCROLL, MAX_SCALE + SCALE_OVERSCROLL)
                val newOffset = if (newScale > MIN_SCALE) offset.value + panChange else Offset.Zero
                coroutineScope.launch {
                    scale.snapTo(newScale)
                    offset.snapTo(newOffset)
                }
                event.changes.forEach { if (it.positionChanged()) it.consume() }
            }
        } while (event.changes.any { it.pressed })
        val settledScale = scale.value.coerceIn(MIN_SCALE, MAX_SCALE)
        coroutineScope.launch {
            scale.animateTo(settledScale, zoomSnapSpring())
            if (settledScale <= MIN_SCALE) offset.animateTo(Offset.Zero, zoomSnapSpring())
        }
    }
}

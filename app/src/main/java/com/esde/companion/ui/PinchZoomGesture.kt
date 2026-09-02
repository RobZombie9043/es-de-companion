package com.esde.companion.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChanged
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Same damping/stiffness family as `ui/main/MainScreen.kt`'s `DRAWER_SETTLE_SPRING` - the
 * house precedent for a physical "snap back into place" motion. */
private fun <T> zoomSnapSpring(): SpringSpec<T> {
    return spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
}

/** [minScale]/[maxScale] are the hard-clamped bounds scale settles to on release; [overscroll]
 * is how far past them a live pinch may stretch before that release-time spring pulls it back -
 * without this, scale/offset would never leave `[minScale, maxScale]` during the gesture, so
 * there'd be nothing out-of-bounds left to visibly spring back from. Bundled into one value to
 * keep [detectPinchZoomWithSnapBack] within this project's `LongParameterList` limit, same
 * reasoning as `SelfHealConfig`/`BackupRepositories` elsewhere. */
internal data class PinchZoomBounds(
    val minScale: Float,
    val maxScale: Float,
    val overscroll: Float,
)

/**
 * A custom gesture loop (not `detectTransformGestures`) - shared by
 * [com.esde.companion.ui.gameguides.GameGuideImageViewerScreen] and
 * [com.esde.companion.ui.pdf.PdfPageViewer], the app's two pinch-zoomable image/page viewers.
 * Needed to distinguish "the gesture is still live" (`snapTo`, instant, keeps the drag
 * responsive) from "the gesture just ended" (`animateTo` with a spring, the release-time bounce
 * back into bounds) - `detectTransformGestures`'s own `onGesture` callback has no separate hook
 * for gesture-end, and calling a suspend `animateTo` from inside its per-event callback would
 * stall live event processing for the animation's duration.
 *
 * `AwaitPointerEventScope` (entered via `awaitEachGesture`) is a `@RestrictsSuspension` scope -
 * it can't directly call arbitrary suspend functions like `Animatable.snapTo`/`animateTo`.
 * [coroutineScope]`.launch { }` (a regular, non-suspend call, using a composable-scoped
 * `rememberCoroutineScope` from the caller rather than this `PointerInputScope`'s own) sidesteps
 * that restriction; `snapTo`/`animateTo` for one event/settle are bundled into a single `launch`
 * each so scale-then-offset stays ordered within it.
 */
internal suspend fun PointerInputScope.detectPinchZoomWithSnapBack(
    scale: Animatable<Float, AnimationVector1D>,
    offset: Animatable<Offset, AnimationVector2D>,
    coroutineScope: CoroutineScope,
    bounds: PinchZoomBounds,
) {
    val (minScale, maxScale, overscroll) = bounds
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            val zoomChange = event.calculateZoom()
            val panChange = event.calculatePan()
            if (zoomChange != 1f || panChange != Offset.Zero) {
                val newScale = (scale.value * zoomChange).coerceIn(minScale - overscroll, maxScale + overscroll)
                val newOffset = if (newScale > minScale) offset.value + panChange else Offset.Zero
                coroutineScope.launch {
                    scale.snapTo(newScale)
                    offset.snapTo(newOffset)
                }
                event.changes.forEach { if (it.positionChanged()) it.consume() }
            }
        } while (event.changes.any { it.pressed })
        val settledScale = scale.value.coerceIn(minScale, maxScale)
        coroutineScope.launch {
            scale.animateTo(settledScale, zoomSnapSpring())
            if (settledScale <= minScale) offset.animateTo(Offset.Zero, zoomSnapSpring())
        }
    }
}

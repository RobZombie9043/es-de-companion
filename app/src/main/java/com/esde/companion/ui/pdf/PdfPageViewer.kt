package com.esde.companion.ui.pdf

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 4f
private const val SWIPE_THRESHOLD_PX = 120f
private const val CONTROLS_AUTO_HIDE_MS = 4_000L

/** What [PdfPageViewer] renders - the currently-rendered page bitmap (null until the first
 * render completes), the current/total page indices, and a content-description label
 * ("Game manual"/a guide's own title) for accessibility. */
data class PdfPageViewerState(
    val currentBitmap: ImageBitmap?,
    val currentPage: Int,
    val pageCount: Int,
    val contentDescriptionLabel: String,
)

/** [onWidthMeasured] reports the real measured content width once, before any render -
 * callers use it to size their own page-rendering (see GameManualViewModel.setTargetWidth
 * for the pattern this was extracted from). */
data class PdfPageViewerActions(
    val onWidthMeasured: (Int) -> Unit,
    val onNextPage: () -> Unit,
    val onPreviousPage: () -> Unit,
    val onExit: () -> Unit,
)

/**
 * Opaque full-screen paged-PDF viewer: pinch-zoom/pan (Animatable + manual snapTo, matching
 * the rest of the app), swipe/fling page-turning when unzoomed, hardware D-pad left/right,
 * and tap-to-reveal exit/page-counter controls that auto-hide after a few seconds. Extracted
 * from the original GameManualScreen (which is now a thin wrapper over this) so
 * GameGuidePdfViewerScreen can reuse the exact same gesture/chrome behavior for an imported
 * PDF guide, rather than duplicating it.
 */
@Composable
fun PdfPageViewer(
    state: PdfPageViewerState,
    actions: PdfPageViewerActions,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    // Only composed while this screen is showing (an AnimatedVisibility in the caller), so
    // it's registered later than MainScreen's own BackHandler and wins Compose's LIFO back
    // dispatch - without it, back would fall through to MainScreen's handler underneath
    // and do nothing, same pattern as FolderContentsPopup/LongPressSettingsMenu.
    BackHandler(onBack = actions.onExit)

    var controlsVisible by remember { mutableStateOf(true) }

    // Auto-hide a few seconds after showing - restarts whenever controlsVisible flips
    // back to true (including from the tap handler below), so each reveal gets its own
    // full timeout rather than a single countdown from first composition.
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(CONTROLS_AUTO_HIDE_MS)
            controlsVisible = false
        }
    }

    val boxModifier =
        modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                when (event.key) {
                    Key.DirectionRight -> {
                        actions.onNextPage()
                        true
                    }
                    Key.DirectionLeft -> {
                        actions.onPreviousPage()
                        true
                    }
                    else -> false
                }
            }

    BoxWithConstraints(modifier = boxModifier) {
        val widthPx = with(LocalDensity.current) { maxWidth.roundToPx() }
        LaunchedEffect(widthPx) { actions.onWidthMeasured(widthPx) }

        val scale = remember { Animatable(MIN_SCALE) }
        val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
        val scope = rememberCoroutineScope()

        // Reset zoom/pan whenever the page changes, so a zoomed-in page doesn't carry
        // its transform over to the next one.
        LaunchedEffect(state.currentPage) {
            scale.snapTo(MIN_SCALE)
            offset.snapTo(Offset.Zero)
        }

        val zoomState = PdfPageZoomState(scale, offset, scope)
        PdfPageImage(
            state = state,
            actions = actions,
            zoom = zoomState,
            controlsVisible = controlsVisible,
            onControlsVisibleChanged = { controlsVisible = it },
        )
        PdfPageViewerOverlayControls(state = state, actions = actions, controlsVisible = controlsVisible)
    }
}

/** [scale]/[offset]/[scope] bundled into one, keeping [PdfPageImage] under detekt's
 * LongParameterList threshold. */
private data class PdfPageZoomState(
    val scale: Animatable<Float, *>,
    val offset: Animatable<Offset, *>,
    val scope: CoroutineScope,
)

/** The rendered page bitmap plus every gesture detector (pinch-zoom/pan, swipe-to-turn-page,
 * tap-to-reveal-controls) - pulled out of [PdfPageViewer] purely to keep that function under
 * detekt's LongMethod threshold. */
@Composable
private fun PdfPageImage(
    state: PdfPageViewerState,
    actions: PdfPageViewerActions,
    zoom: PdfPageZoomState,
    controlsVisible: Boolean,
    onControlsVisibleChanged: (Boolean) -> Unit,
) {
    val (scale, offset, scope) = zoom
    val hapticFeedback = LocalHapticFeedback.current
    val bitmap = state.currentBitmap ?: return
    var dragTotal = 0f
    val pageDescription = "${state.contentDescriptionLabel}, page ${state.currentPage + 1} of ${state.pageCount}"
    val imageModifier =
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                translationX = offset.value.x
                translationY = offset.value.y
            }
            .pointerInput(state.currentPage) {
                detectTransformGestures { _, pan, zoomDelta, _ ->
                    scope.launch {
                        val newScale = (scale.value * zoomDelta).coerceIn(MIN_SCALE, MAX_SCALE)
                        scale.snapTo(newScale)
                        offset.snapTo(if (newScale > MIN_SCALE) offset.value + pan else Offset.Zero)
                    }
                }
            }
            .pointerInput(state.currentPage, scale.value) {
                // Swipe-to-turn-page only while unzoomed - otherwise a pinched-in
                // pan would fight this gesture detector.
                if (scale.value > MIN_SCALE) return@pointerInput
                detectHorizontalDragGestures(
                    onDragEnd = {
                        when {
                            dragTotal <= -SWIPE_THRESHOLD_PX -> actions.onNextPage()
                            dragTotal >= SWIPE_THRESHOLD_PX -> actions.onPreviousPage()
                        }
                        dragTotal = 0f
                    },
                ) { change, dragAmount ->
                    change.consume()
                    dragTotal += dragAmount
                }
            }
            .pointerInput(Unit) {
                // Separate tap-only detector for revealing/hiding controls - kept
                // independent of the transform/drag detectors above so a plain
                // tap (no pan, no drag past threshold) isn't swallowed by either.
                detectTapGestures(
                    onTap = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onControlsVisibleChanged(!controlsVisible)
                    },
                )
            }
    Image(
        bitmap = bitmap,
        contentDescription = pageDescription,
        contentScale = ContentScale.Fit,
        modifier = imageModifier,
    )
}

/** The tap-to-reveal page-counter/exit controls - pulled out of [PdfPageViewer] for the same
 * LongMethod reasoning as [PdfPageImage]. A [BoxScope] extension (not a plain top-level
 * composable) since both controls anchor themselves via [Modifier.align] - a [BoxScope]-scoped
 * modifier that only resolves with that receiver in scope, same as the original inline code's
 * [BoxWithConstraints] content lambda provided implicitly. */
@Composable
private fun BoxScope.PdfPageViewerOverlayControls(
    state: PdfPageViewerState,
    actions: PdfPageViewerActions,
    controlsVisible: Boolean,
) {
    AnimatedVisibility(
        visible = controlsVisible && state.pageCount > 0,
        modifier = Modifier.align(Alignment.BottomEnd),
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Surface(
            modifier = Modifier.padding(24.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f),
        ) {
            Text(
                text = "${state.currentPage + 1} / ${state.pageCount}",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }

    AnimatedVisibility(
        visible = controlsVisible,
        modifier = Modifier.align(Alignment.TopEnd),
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Surface(
            modifier = Modifier.padding(24.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f),
        ) {
            IconButton(onClick = actions.onExit) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Exit",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

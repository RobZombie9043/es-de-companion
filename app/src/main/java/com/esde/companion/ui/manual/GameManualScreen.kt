package com.esde.companion.ui.manual

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 4f
private const val SWIPE_THRESHOLD_PX = 120f
private const val CONTROLS_AUTO_HIDE_MS = 4_000L

/**
 * Opaque full-screen manual PDF viewer, drawn as a sibling of WidgetOverlay in
 * MainActivity whenever Settings > UI Settings > Game Playing Behavior is GameManual and
 * a manual was resolved for the current game - same "cover the plain main screen only"
 * placement as the existing Dim/Black behaviors. Reverts automatically when AppState
 * leaves PlayingGame; [onExit] additionally lets the user dismiss it early without
 * waiting for the game to end (see MainActivity's manualDismissed flag).
 *
 * The exit button and page counter are tap-to-reveal - a single tap (while unzoomed)
 * toggles [controlsVisible], which then auto-hides after a few seconds, matching a
 * typical video-player-controls pattern so they don't stay permanently on top of the
 * manual page.
 *
 * Page turning is swipe/fling when unzoomed, plus hardware D-pad left/right - verify on
 * device that the vendor ROM's physical buttons actually surface as standard KeyEvents
 * reaching Compose focus; if not, this needs to move up to MainActivity's key dispatch,
 * the same way the back-button burst quirk did.
 *
 * Pinch-zoom/pan uses Animatable + manual snapTo (not AnchoredDraggableState), matching
 * the rest of the app.
 */
@Composable
fun GameManualScreen(
    viewModel: GameManualViewModel,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentBitmap by viewModel.currentBitmap.collectAsStateWithLifecycle()
    val currentPage by viewModel.currentPage.collectAsStateWithLifecycle()
    val pageCount by viewModel.pageCount.collectAsStateWithLifecycle()

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val hapticFeedback = LocalHapticFeedback.current

    // Only composed while this screen is showing (AnimatedVisibility in MainActivity), so
    // it's registered later than MainScreen's own BackHandler and wins Compose's LIFO back
    // dispatch - without it, back would fall through to MainScreen's handler underneath
    // and do nothing, same pattern as FolderContentsPopup/LongPressSettingsMenu.
    BackHandler(onBack = onExit)

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

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.Black)
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { event ->
                    when (event.key) {
                        Key.DirectionRight -> {
                            viewModel.nextPage()
                            true
                        }
                        Key.DirectionLeft -> {
                            viewModel.previousPage()
                            true
                        }
                        else -> false
                    }
                },
    ) {
        val widthPx = with(LocalDensity.current) { maxWidth.roundToPx() }
        LaunchedEffect(widthPx) { viewModel.setTargetWidth(widthPx) }

        val scale = remember { Animatable(MIN_SCALE) }
        val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
        val scope = rememberCoroutineScope()

        // Reset zoom/pan whenever the page changes, so a zoomed-in page doesn't carry
        // its transform over to the next one.
        LaunchedEffect(currentPage) {
            scale.snapTo(MIN_SCALE)
            offset.snapTo(Offset.Zero)
        }

        currentBitmap?.let { bitmap ->
            var dragTotal = 0f
            Image(
                bitmap = bitmap,
                contentDescription = "Game manual, page ${currentPage + 1} of $pageCount",
                contentScale = ContentScale.Fit,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale.value
                            scaleY = scale.value
                            translationX = offset.value.x
                            translationY = offset.value.y
                        }
                        .pointerInput(currentPage) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scope.launch {
                                    val newScale = (scale.value * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                                    scale.snapTo(newScale)
                                    offset.snapTo(if (newScale > MIN_SCALE) offset.value + pan else Offset.Zero)
                                }
                            }
                        }
                        .pointerInput(currentPage, scale.value) {
                            // Swipe-to-turn-page only while unzoomed - otherwise a pinched-in
                            // pan would fight this gesture detector.
                            if (scale.value > MIN_SCALE) return@pointerInput
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    when {
                                        dragTotal <= -SWIPE_THRESHOLD_PX -> viewModel.nextPage()
                                        dragTotal >= SWIPE_THRESHOLD_PX -> viewModel.previousPage()
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
                                    controlsVisible = !controlsVisible
                                },
                            )
                        },
            )
        }

        AnimatedVisibility(
            visible = controlsVisible && pageCount > 0,
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
                    text = "${currentPage + 1} / $pageCount",
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
                IconButton(onClick = onExit) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Exit manual",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

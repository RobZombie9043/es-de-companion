package com.esde.companion.ui.gameguides

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.background
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.esde.companion.ui.PinchZoomBounds
import com.esde.companion.ui.detectPinchZoomWithSnapBack
import com.esde.companion.ui.main.CrossfadeAsyncImage

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 4f

// How far past MIN_SCALE/MAX_SCALE a live pinch may stretch before the release-time spring
// pulls it back - without this, scale/offset never leave [MIN_SCALE, MAX_SCALE] during the
// gesture (hard-clamped), so there'd be nothing out-of-bounds left to visibly spring back from
// on release.
private const val SCALE_OVERSCROLL = 0.5f

/**
 * Renders a [com.esde.companion.domain.model.GameGuideFormat.Image] guide - the simplest of
 * the three viewer branches, since a single static image has no pages/reading progress to
 * persist (unlike [GameGuidePdfViewerScreen]/the text viewer). Pinch-zoom only, no paging.
 * Zoom/pan gesture handling ([com.esde.companion.ui.detectPinchZoomWithSnapBack]) is shared
 * with [com.esde.companion.ui.pdf.PdfPageViewer], this app's other pinch-zoomable page/image
 * viewer.
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
            .pointerInput(Unit) {
                val bounds = PinchZoomBounds(minScale = MIN_SCALE, maxScale = MAX_SCALE, overscroll = SCALE_OVERSCROLL)
                detectPinchZoomWithSnapBack(scale, offset, coroutineScope, bounds)
            }

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

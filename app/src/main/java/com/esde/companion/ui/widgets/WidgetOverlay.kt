package com.esde.companion.ui.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.imageLoader
import com.esde.companion.ui.main.requestFor
import com.esde.companion.ui.theme.LocalIsDarkTheme
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Top-level entry point for the widget system: measures available space to derive the
 * grid (see [gridDimensionsFor]), feeds that to [viewModel], and renders whatever it
 * resolves - the same fallback background MainScreenImages used to show when there's no
 * active canvas, with a centered "Waiting for ES-DE" message once the grid is known and
 * genuinely nothing applies (Idle / not connected - see [WidgetCanvasState.Disconnected]).
 */
@Composable
fun WidgetOverlay(
    viewModel: WidgetsViewModel,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val grid = remember(maxWidth, maxHeight) { gridDimensionsFor(maxWidth, maxHeight) }
        LaunchedEffect(grid) { viewModel.setGridDimensions(grid) }

        val canvasState by viewModel.canvasState.collectAsStateWithLifecycle()

        when (val state = canvasState) {
            WidgetCanvasState.Unmeasured ->
                AsyncImage(
                    model = fallbackBackgroundAssetPath(LocalIsDarkTheme.current),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )

            WidgetCanvasState.Disconnected ->
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = fallbackBackgroundAssetPath(LocalIsDarkTheme.current),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Text(
                        text = "Waiting for ES-DE",
                        color = if (LocalIsDarkTheme.current) Color.White else Color.Black,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center).padding(8.dp),
                    )
                }

            is WidgetCanvasState.Showing -> {
                val displayed = rememberDisplayedCanvas(state)
                WidgetCanvas(
                    widgets = displayed.widgets,
                    contentByWidgetId = displayed.contentByWidgetId,
                    navigationDirection = displayed.navigationDirection,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/**
 * Holds the previously-displayed canvas on screen until [target]'s backdrop image (see
 * [backdropWidgetOf]) is fully decoded into Coil's memory cache, then swaps - rather than
 * swapping immediately and letting the freshly-mounted [com.esde.companion.ui.main.CrossfadeAsyncImage]
 * render a blank frame while it fetches+decodes live (see [canvasSwapNeedsHold]'s kdoc for
 * why a canvas swap causes that gap). Ordinary same-canvas navigation (same backdrop widget
 * id) swaps through immediately with no added latency, since CrossfadeAsyncImage's own
 * instance-continuity already crossfades correctly in that case.
 *
 * The preload is best-effort: [BACKDROP_HOLD_TIMEOUT_MILLIS] caps how long the previous
 * canvas is held so a slow/corrupt/missing file can never get the display stuck, and the
 * swap to [target] always happens afterward regardless of whether the preload succeeded.
 */
@Composable
private fun rememberDisplayedCanvas(target: WidgetCanvasState.Showing): WidgetCanvasState.Showing {
    var displayed by remember { mutableStateOf(target) }
    val context = LocalContext.current
    val isDarkTheme = LocalIsDarkTheme.current

    LaunchedEffect(target) {
        if (!canvasSwapNeedsHold(displayed, target)) {
            displayed = target
            return@LaunchedEffect
        }

        val backdropWidget = backdropWidgetOf(target.widgets, target.contentByWidgetId)
        val backdropContent = backdropWidget?.let { target.contentByWidgetId[it.id] }
        val model = backdropContent?.let { backdropModelOf(it, isDarkTheme) }
        if (model != null) {
            withTimeoutOrNull(BACKDROP_HOLD_TIMEOUT_MILLIS) {
                context.imageLoader.execute(requestFor(context, model))
            }
        }

        displayed = target
    }

    return displayed
}

private const val BACKDROP_HOLD_TIMEOUT_MILLIS = 1500L

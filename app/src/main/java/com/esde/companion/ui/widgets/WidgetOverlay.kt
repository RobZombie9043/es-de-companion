package com.esde.companion.ui.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.esde.companion.ui.theme.LocalIsDarkTheme

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

            is WidgetCanvasState.Showing ->
                WidgetCanvas(
                    widgets = state.widgets,
                    contentByWidgetId = state.contentByWidgetId,
                    navigationDirection = state.navigationDirection,
                    modifier = Modifier.fillMaxSize(),
                )
        }
    }
}

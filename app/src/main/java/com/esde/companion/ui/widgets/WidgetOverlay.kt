package com.esde.companion.ui.widgets

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.esde.companion.ui.theme.LocalIsDarkTheme

/**
 * Top-level entry point for the widget system: measures available space to derive the
 * grid (see [gridDimensionsFor]), feeds that to [viewModel], and renders whatever it
 * resolves - the same fallback background MainScreenImages used to show for
 * MainScreenImageState.None when there's no active canvas (Idle, or grid not measured yet).
 */
@Composable
fun WidgetOverlay(viewModel: WidgetsViewModel, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val grid = remember(maxWidth, maxHeight) { gridDimensionsFor(maxWidth, maxHeight) }
        LaunchedEffect(grid) { viewModel.setGridDimensions(grid) }

        val canvasState by viewModel.canvasState.collectAsStateWithLifecycle()
        val imageTransitionMode by viewModel.imageTransitionMode.collectAsStateWithLifecycle()
        val logoTransitionMode by viewModel.logoTransitionMode.collectAsStateWithLifecycle()

        when (val state = canvasState) {
            WidgetCanvasState.None ->
                AsyncImage(
                    model = fallbackBackgroundAssetPath(LocalIsDarkTheme.current),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )

            is WidgetCanvasState.Showing ->
                WidgetCanvas(
                    widgets = state.widgets,
                    contentByWidgetId = state.contentByWidgetId,
                    imageTransitionMode = imageTransitionMode,
                    logoTransitionMode = logoTransitionMode,
                    navigationDirection = state.navigationDirection,
                    modifier = Modifier.fillMaxSize(),
                )
        }
    }
}
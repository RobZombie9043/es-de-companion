package com.esde.companion.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/**
 * Shared sizing for the small always-on-top corner controls that float over MainScreen
 * regardless of what's underneath: the music FAB and its MusicControlsOverlay panel
 * (MainActivity, top-start corner), the Settings button (MainScreen, top-end corner), and
 * the Edit Widgets options button (EditWidgetsOverlay, top-end corner). Centralized so the
 * three stay vertically aligned by construction - same size, same edge padding - rather
 * than needing matching magic numbers kept in sync by hand across three files.
 */
val CORNER_BUTTON_SIZE = 56.dp // Matches Material3 FloatingActionButton's default container size.
val CORNER_BUTTON_EDGE_PADDING = 16.dp

/** Same corner radius as MusicControlsOverlay's panel and AppDock's menu, so every
 * translucent overlay surface in the app reads as one consistent family of shapes. */
private val CORNER_BUTTON_SHAPE = RoundedCornerShape(16.dp)

/**
 * Drop-in replacement for a Material3 [androidx.compose.material3.FloatingActionButton],
 * used by the music FAB, Settings button, and Edit Widgets options button so all three
 * pick up the exact same theme-aware styling already used by the App Dock, App Drawer,
 * and music controls panel - black background/white icon in dark theme, white
 * background/black icon in light theme - rather than Material3's default purple FAB
 * colors, at the shared Settings > UI Settings > Overlay Opacity value rather than fully
 * opaque.
 */
@Composable
fun CornerFab(
    onClick: () -> Unit,
    opacityPercent: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val backgroundColor = if (isDarkTheme) Color.Black else Color.White
    val contentColor = if (isDarkTheme) Color.White else Color.Black

    Box(
        modifier = modifier
            .size(CORNER_BUTTON_SIZE)
            .clip(CORNER_BUTTON_SHAPE)
            .background(backgroundColor.copy(alpha = opacityPercent / 100f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

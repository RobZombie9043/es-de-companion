package com.esde.companion.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.esde.companion.domain.model.FabPosition
import com.esde.companion.ui.theme.isDarkSurface

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

/** Black-on-dark/white-on-light background+content color pair shared by [CornerFab] and
 * [WideCornerFab], factored out so both pick up the same theme-aware styling by construction. */
@Composable
private fun cornerButtonColors(): Pair<Color, Color> {
    val isDarkTheme = isDarkSurface()
    return if (isDarkTheme) Color.Black to Color.White else Color.White to Color.Black
}

/**
 * Drop-in replacement for a Material3 [androidx.compose.material3.FloatingActionButton],
 * used by the music FAB, Settings button, and Edit Widgets options button so all three
 * pick up the exact same theme-aware styling already used by the App Dock, App Drawer,
 * and music controls panel - black background/white icon in dark theme, white
 * background/black icon in light theme - rather than Material3's default purple FAB
 * colors, at the shared Settings > UI Settings > Overlay Opacity value rather than fully
 * opaque.
 *
 * [onDoubleClick], when non-null, adds Compose's usual tap/double-tap disambiguation delay
 * to every click on this button (see [combinedClickable]) - used only by the Custom App
 * FAB (single tap launches on this screen, double tap on the other, mirroring the App
 * Dock's launch convention). Left null for every other caller (Settings, Music, App
 * Drawer, Game Manual), which keeps their existing immediate single-tap response.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CornerFab(
    onClick: () -> Unit,
    opacityPercent: Int,
    modifier: Modifier = Modifier,
    onDoubleClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val (backgroundColor, contentColor) = cornerButtonColors()

    Box(
        modifier =
            modifier
                .size(CORNER_BUTTON_SIZE)
                .clip(CORNER_BUTTON_SHAPE)
                .background(backgroundColor.copy(alpha = opacityPercent / 100f))
                .combinedClickable(onClick = onClick, onDoubleClick = onDoubleClick),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

/**
 * Same theming/shape as [CornerFab] but fixed height / wrap-content width instead of a fixed
 * square - for FAB content whose width varies with what it displays (Clock's time text,
 * SystemStatus's icon row). Used by ClockFabContent/SystemStatusFabContent/
 * ClockAndSystemStatusFabContent (see ui/SystemStatusFabContent.kt).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WideCornerFab(
    onClick: () -> Unit,
    opacityPercent: Int,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val (backgroundColor, contentColor) = cornerButtonColors()

    Row(
        modifier =
            modifier
                .height(CORNER_BUTTON_SIZE)
                .widthIn(min = CORNER_BUTTON_SIZE)
                .wrapContentWidth()
                .clip(CORNER_BUTTON_SHAPE)
                .background(backgroundColor.copy(alpha = opacityPercent / 100f))
                .combinedClickable(onClick = onClick)
                .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

/** Maps a [FabPosition] onto the [Alignment] a corner-anchored composable aligns itself by. */
fun FabPosition.toAlignment(): Alignment =
    when (this) {
        FabPosition.TopStart -> Alignment.TopStart
        FabPosition.TopEnd -> Alignment.TopEnd
        FabPosition.BottomStart -> Alignment.BottomStart
        FabPosition.BottomEnd -> Alignment.BottomEnd
    }

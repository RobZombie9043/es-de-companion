package com.esde.companion.ui.retroachievements

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Both RetroAchievements screens (achievement summary, system games browser) render
 * directly over an arbitrary background image (see [RetroAchievementsScreen]'s kdoc), so
 * their text/icons need a theme-derived color rather than each `Text`/`Icon` call picking
 * whatever default Material color happens to apply, which wouldn't reliably contrast with
 * that image. [themedContentColor] (white in dark theme, black in light) is provided once
 * at each screen's root via `LocalContentColor` rather than passed to every call site.
 * [themedTileColor] is the opposite - the translucent "button" tiles' own background.
 */
@Composable
internal fun themedIsDarkTheme(): Boolean = MaterialTheme.colorScheme.surface.luminance() < LUMINANCE_THRESHOLD

@Composable
internal fun themedContentColor(): Color = if (themedIsDarkTheme()) Color.White else Color.Black

@Composable
internal fun themedTileColor(): Color = if (themedIsDarkTheme()) Color.Black else Color.White

internal const val OVERLAY_PERCENT_DIVISOR = 100f

private const val LUMINANCE_THRESHOLD = 0.5f

/**
 * Both RetroAchievements screens are siblings composed after (drawn on top of) MainScreen,
 * not descendants of it - so unlike MainScreen's own content, nothing here automatically wins
 * hit-testing against MainScreen's whole-screen swipe-to-open-drawer `detectVerticalDragGestures`
 * underneath it. A swipe starting over one of these screens' non-interactive areas (the top
 * bar, an empty/loading message, between list rows) fell through and opened the App Drawer.
 * Consuming vertical drags at each screen's root first blocks that fallthrough while leaving
 * taps and the achievement/game list's own scroll gesture alone - Compose dispatches the Main
 * pointer-input pass child-to-parent, so a descendant's scrollable/clickable still gets first
 * crack at consuming the gesture.
 */
internal fun Modifier.blockAppDrawerSwipeFallthrough(): Modifier =
    pointerInput(Unit) {
        detectVerticalDragGestures { change, _ -> change.consume() }
    }

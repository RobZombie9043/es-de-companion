package com.esde.companion.ui.retroachievements

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

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

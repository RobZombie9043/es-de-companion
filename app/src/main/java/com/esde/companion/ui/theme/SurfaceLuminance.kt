package com.esde.companion.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.luminance

private const val DARK_SURFACE_LUMINANCE_THRESHOLD = 0.5f

/**
 * True when [MaterialTheme.colorScheme.surface] is dark enough that content drawn on
 * it needs light-colored icons/text for contrast. Shared primitive for every call site
 * that independently derived this from surface luminance (App Dock, App Drawer, corner
 * FABs, Music overlay, RetroAchievements theming).
 */
@Composable
internal fun isDarkSurface(): Boolean = MaterialTheme.colorScheme.surface.luminance() < DARK_SURFACE_LUMINANCE_THRESHOLD

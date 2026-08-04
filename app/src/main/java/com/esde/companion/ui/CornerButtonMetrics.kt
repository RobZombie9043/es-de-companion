package com.esde.companion.ui

import androidx.compose.ui.unit.dp

/**
 * Shared sizing for the small always-on-top corner controls that float over MainScreen
 * regardless of what's underneath: the music FAB and its MusicControlsOverlay panel
 * (MainActivity, top-start corner) and the Settings button (MainScreen, top-end corner).
 * Centralized so the three stay vertically aligned by construction - same size, same edge
 * padding - rather than needing matching magic numbers kept in sync by hand across two
 * files. The Settings button used to live inside a Material3 TopAppBar, whose own default
 * height didn't match the FAB's and threw the row out of alignment; it's now sized/placed
 * with these same constants instead.
 */
val CORNER_BUTTON_SIZE = 56.dp // Matches Material3 FloatingActionButton's default container size.
val CORNER_BUTTON_EDGE_PADDING = 16.dp

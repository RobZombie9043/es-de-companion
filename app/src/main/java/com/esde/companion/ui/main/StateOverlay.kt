package com.esde.companion.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.esde.companion.domain.model.EsdeConnectionState

/**
 * The floating panel showing "what ES-DE is doing right now" (AppState/args plus cover
 * image lookup status) on top of the main screen. Deliberately a self-contained,
 * small composable - toggling it on/off, positioning it, and animating that toggle are
 * MainScreen's job; this only knows how to render its own content given the data it's
 * handed.
 *
 * Text is forced white rather than following MaterialTheme's default on-surface color:
 * this panel now sits over a full-screen backdrop image (fanart/screenshots), not a
 * plain themed background, so the text needs to reliably contrast against arbitrary
 * artwork rather than the current color scheme. The semi-transparent dark-leaning
 * surface behind it (below) provides the contrast backdrop for that white text.
 */
@Composable
fun StateOverlay(
    connectionState: EsdeConnectionState,
    coverImageStatus: CoverImageStatus?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.6f),
    ) {
        Text(
            text = connectionState.toDisplayText() + coverImageStatus.toDisplayText(),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            modifier = Modifier.padding(16.dp),
        )
    }
}
package com.esde.companion.ui.music

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.esde.companion.domain.model.MusicPlaybackState

/** Material3 IconButton's default minimum touch target size - the two of these in the Row
 * below never carry an explicit size modifier, so this is their real reserved width. */
private val ICON_BUTTON_SIZE = 48.dp

private val ROW_HORIZONTAL_PADDING = 12.dp
private val TITLE_END_PADDING = 8.dp

/** Below this luminance, the surface reads as dark mode - same threshold
 * CornerButtonMetrics/AppDock/AppDrawer use for their own theme detection. */
private const val DARK_THEME_LUMINANCE_THRESHOLD = 0.5f

/**
 * Small track title + play/pause + next card, revealed by MainActivity's music FAB.
 * Renders nothing while [MusicPlaybackState.Stopped] (no track loaded yet).
 *
 * Height matches the FAB's for a short single-line title (see MainActivity's
 * `heightIn(min = CORNER_BUTTON_SIZE)` on the caller side, so the two read as one row) but
 * is otherwise content-driven, not fixed - a title that needs a second line grows the card
 * downward rather than being clipped or forced back onto one line.
 *
 * The title is deliberately *not* a plain `Text` sized by its own layout pass. Compose's
 * Text lays out a wrapped paragraph using the entire width it was offered, regardless of
 * how much shorter the widest rendered line ends up being - so a two-line title (e.g. a
 * long first line, a short trailing word on the second) leaves a visible gap between the
 * text and the icons, which sit at the edge of that fully-claimed width rather than up
 * against the text.
 *
 * Fixing this needs to know how wide the text *actually* rendered, which needs a real
 * measurement - so [rememberTextMeasurer] measures the title once, at the true available
 * width (the card's own incoming max width, minus the icons' fixed reservation and
 * paddings, read via [BoxWithConstraints]), and the visible `Text` below is then sized to
 * exactly that result's widest line via `Modifier.width(...)`. This was originally done by
 * feeding a live Text's own `onTextLayout` result back into its own width constraint, but
 * that free-runs: re-measuring at exactly the previous frame's result can come back a hair
 * narrower purely from px<->dp rounding, which then wraps the same content differently and
 * narrows again the frame after - a shrinking spiral that (confirmed on-device) can run
 * away to almost nothing over a few frames. A single explicit measurement has no such
 * feedback loop to begin with.
 */
@Composable
fun MusicControlsOverlay(
    viewModel: MusicControlsViewModel,
    opacityPercent: Int = 100,
    modifier: Modifier = Modifier,
) {
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()

    val track =
        when (val state = playbackState) {
            is MusicPlaybackState.Playing -> state.track
            is MusicPlaybackState.Paused -> state.track
            MusicPlaybackState.Stopped -> null
        }

    if (track == null) return

    val isPlaying = playbackState is MusicPlaybackState.Playing
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < DARK_THEME_LUMINANCE_THRESHOLD
    val backgroundColor = if (isDarkTheme) Color.Black else Color.White
    val contentColor = if (isDarkTheme) Color.White else Color.Black

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor.copy(alpha = opacityPercent / 100f),
        tonalElevation = 4.dp,
    ) {
        BoxWithConstraints {
            val density = LocalDensity.current
            val textMeasurer = rememberTextMeasurer()
            val titleStyle = MaterialTheme.typography.bodyMedium

            val reservedForIconsAndPadding = ROW_HORIZONTAL_PADDING * 2 + ICON_BUTTON_SIZE * 2 + TITLE_END_PADDING
            val availableForTitlePx =
                with(density) {
                    (maxWidth - reservedForIconsAndPadding).toPx().toInt().coerceAtLeast(0)
                }

            val titleLayout =
                remember(track.title, availableForTitlePx, titleStyle) {
                    textMeasurer.measure(
                        text = track.title,
                        style = titleStyle,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 2,
                        constraints = Constraints(maxWidth = availableForTitlePx),
                    )
                }
            val titleWidth =
                with(density) {
                    (0 until titleLayout.lineCount)
                        .maxOf { titleLayout.getLineRight(it) - titleLayout.getLineLeft(it) }
                        .toDp()
                }

            Row(
                modifier = Modifier.padding(horizontal = ROW_HORIZONTAL_PADDING, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = track.title,
                    style = titleStyle,
                    color = contentColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(titleWidth).padding(end = TITLE_END_PADDING),
                )
                IconButton(onClick = viewModel::togglePlayPause) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = contentColor,
                    )
                }
                IconButton(onClick = viewModel::next) {
                    Icon(imageVector = Icons.Filled.SkipNext, contentDescription = "Next track", tint = contentColor)
                }
            }
        }
    }
}

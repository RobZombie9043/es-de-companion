package com.esde.companion.ui.music

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.esde.companion.domain.model.MusicPlaybackState

/**
 * Small track title + play/pause + next card, revealed by MainActivity's music FAB.
 * Renders nothing while [MusicPlaybackState.Stopped] (no track loaded yet).
 */
@Composable
fun MusicControlsOverlay(viewModel: MusicControlsViewModel, modifier: Modifier = Modifier) {
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()

    val track = when (val state = playbackState) {
        is MusicPlaybackState.Playing -> state.track
        is MusicPlaybackState.Paused -> state.track
        MusicPlaybackState.Stopped -> null
    }

    if (track == null) return

    val isPlaying = playbackState is MusicPlaybackState.Playing

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(end = 8.dp),
            )
            IconButton(onClick = viewModel::togglePlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                )
            }
            IconButton(onClick = viewModel::next) {
                Icon(imageVector = Icons.Filled.SkipNext, contentDescription = "Next track")
            }
        }
    }
}

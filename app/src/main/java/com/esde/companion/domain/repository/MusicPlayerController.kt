package com.esde.companion.domain.repository

import com.esde.companion.domain.model.MusicTrack
import kotlinx.coroutines.flow.Flow

interface MusicPlayerController {
    fun playTrack(track: MusicTrack)

    fun pause()

    fun resume()

    fun setVolume(fraction: Float)

    fun observeTrackCompletion(): Flow<Unit>

    /** Emits a human-readable message each time the underlying player fails to play the
     * current track (e.g. an unreadable/corrupt file). */
    fun observePlaybackError(): Flow<String>
}

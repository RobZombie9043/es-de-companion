package com.esde.companion.domain.repository

import com.esde.companion.domain.model.MusicTrack
import kotlinx.coroutines.flow.Flow

interface MusicPlayerController {
    fun playTrack(track: MusicTrack)

    fun pause()

    fun resume()

    fun setVolume(fraction: Float)

    fun observeTrackCompletion(): Flow<Unit>
}

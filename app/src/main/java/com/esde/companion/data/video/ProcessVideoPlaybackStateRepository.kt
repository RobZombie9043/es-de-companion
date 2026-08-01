package com.esde.companion.data.video

import com.esde.companion.domain.repository.VideoPlaybackStateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory only, mirroring [com.esde.companion.data.activity.ProcessActivityVisibilityRepository].
 * Defaults to false (no video known to be playing yet) - the safe default for ducking,
 * opposite of ActivityVisibilityRepository's default-true.
 */
class ProcessVideoPlaybackStateRepository : VideoPlaybackStateRepository {

    private val isPlaying = MutableStateFlow(false)

    override fun observeIsPlaying(): Flow<Boolean> = isPlaying

    override fun setIsPlaying(playing: Boolean) {
        isPlaying.value = playing
    }
}

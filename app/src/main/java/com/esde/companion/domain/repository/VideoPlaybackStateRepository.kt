package com.esde.companion.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Whether the video overlay is actually audibly playing right now - the seam
 * [com.esde.companion.domain.music.MusicPlaybackCoordinator] uses to duck background
 * music, since that state otherwise only exists as private composable state inside
 * VideoOverlayScreen. Same interface shape as [ActivityVisibilityRepository].
 */
interface VideoPlaybackStateRepository {
    fun observeIsPlaying(): Flow<Boolean>

    fun setIsPlaying(playing: Boolean)
}

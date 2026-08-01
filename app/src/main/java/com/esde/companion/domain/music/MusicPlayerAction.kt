package com.esde.companion.domain.music

import com.esde.companion.domain.model.MusicDuckingMode

private const val DUCKED_VOLUME_FRACTION = 0.2f
private const val FULL_VOLUME_FRACTION = 1f

sealed class MusicPlayerAction {
    data object NoTrackLoaded : MusicPlayerAction()

    data object Paused : MusicPlayerAction()

    data class Playing(val volumeFraction: Float) : MusicPlayerAction()
}

/**
 * Resolves what the music player should be doing right now from independent signals -
 * eligibility loss pauses in place rather than stopping, and ducking is applied
 * independently of pool/track changes so it reacts every snapshot, not just on switches.
 */
object MusicPlayerActionResolver {

    fun resolve(
        hasCurrentTrack: Boolean,
        eligible: Boolean,
        userPaused: Boolean,
        isDuckedByVideo: Boolean,
        duckingMode: MusicDuckingMode,
    ): MusicPlayerAction {
        if (!hasCurrentTrack) return MusicPlayerAction.NoTrackLoaded
        if (!eligible || userPaused) return MusicPlayerAction.Paused
        if (isDuckedByVideo && duckingMode == MusicDuckingMode.Pause) return MusicPlayerAction.Paused

        val volumeFraction =
            if (isDuckedByVideo && duckingMode == MusicDuckingMode.LowerVolume) {
                DUCKED_VOLUME_FRACTION
            } else {
                FULL_VOLUME_FRACTION
            }
        return MusicPlayerAction.Playing(volumeFraction)
    }
}

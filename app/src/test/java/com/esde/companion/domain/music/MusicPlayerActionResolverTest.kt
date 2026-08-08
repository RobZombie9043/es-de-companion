package com.esde.companion.domain.music

import com.esde.companion.domain.model.MusicDuckingMode
import org.junit.Assert.assertEquals
import org.junit.Test

class MusicPlayerActionResolverTest {
    @Test
    fun `no current track always resolves to NoTrackLoaded regardless of every other input`() {
        assertEquals(
            MusicPlayerAction.NoTrackLoaded,
            MusicPlayerActionResolver.resolve(
                hasCurrentTrack = false,
                eligible = true,
                userPaused = false,
                isDuckedByVideo = false,
                duckingMode = MusicDuckingMode.Unchanged,
            ),
        )
    }

    @Test
    fun `ineligible pauses in place rather than clearing the track`() {
        assertEquals(
            MusicPlayerAction.Paused,
            MusicPlayerActionResolver.resolve(
                hasCurrentTrack = true,
                eligible = false,
                userPaused = false,
                isDuckedByVideo = false,
                duckingMode = MusicDuckingMode.Unchanged,
            ),
        )
    }

    @Test
    fun `userPaused takes precedence and pauses even while eligible and not ducked`() {
        assertEquals(
            MusicPlayerAction.Paused,
            MusicPlayerActionResolver.resolve(
                hasCurrentTrack = true,
                eligible = true,
                userPaused = true,
                isDuckedByVideo = false,
                duckingMode = MusicDuckingMode.Unchanged,
            ),
        )
    }

    @Test
    fun `ducked with Pause mode pauses`() {
        assertEquals(
            MusicPlayerAction.Paused,
            MusicPlayerActionResolver.resolve(
                hasCurrentTrack = true,
                eligible = true,
                userPaused = false,
                isDuckedByVideo = true,
                duckingMode = MusicDuckingMode.Pause,
            ),
        )
    }

    @Test
    fun `ducked with LowerVolume mode plays at reduced volume`() {
        assertEquals(
            MusicPlayerAction.Playing(0.2f),
            MusicPlayerActionResolver.resolve(
                hasCurrentTrack = true,
                eligible = true,
                userPaused = false,
                isDuckedByVideo = true,
                duckingMode = MusicDuckingMode.LowerVolume,
            ),
        )
    }

    @Test
    fun `ducked with Unchanged mode plays at full volume - ducking is independent of the mode being inert`() {
        assertEquals(
            MusicPlayerAction.Playing(1f),
            MusicPlayerActionResolver.resolve(
                hasCurrentTrack = true,
                eligible = true,
                userPaused = false,
                isDuckedByVideo = true,
                duckingMode = MusicDuckingMode.Unchanged,
            ),
        )
    }

    @Test
    fun `not ducked plays at full volume regardless of ducking mode configured`() {
        assertEquals(
            MusicPlayerAction.Playing(1f),
            MusicPlayerActionResolver.resolve(
                hasCurrentTrack = true,
                eligible = true,
                userPaused = false,
                isDuckedByVideo = false,
                duckingMode = MusicDuckingMode.Pause,
            ),
        )
    }
}

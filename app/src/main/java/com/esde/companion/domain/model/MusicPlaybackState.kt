package com.esde.companion.domain.model

sealed class MusicPlaybackState {
    data object Stopped : MusicPlaybackState()

    data class Playing(val track: MusicTrack, val pool: MusicPool) : MusicPlaybackState()

    data class Paused(val track: MusicTrack, val pool: MusicPool) : MusicPlaybackState()
}

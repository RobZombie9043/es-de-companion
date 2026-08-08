package com.esde.companion.ui.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esde.companion.domain.model.MusicPlaybackState
import com.esde.companion.domain.music.MusicPlaybackCoordinator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MusicControlsViewModel(
    private val coordinator: MusicPlaybackCoordinator,
) : ViewModel() {
    val playbackState: StateFlow<MusicPlaybackState> =
        coordinator.playbackState.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = MusicPlaybackState.Stopped,
        )

    fun togglePlayPause() = coordinator.togglePlayPause()

    fun next() = coordinator.next()
}

package com.esde.companion.ui.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.esde.companion.AppContainer

class VideoOverlayViewModelFactory(
    private val appContainer: AppContainer,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(VideoOverlayViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return VideoOverlayViewModel(
            observeConnectionState = appContainer.observeConnectionStateUseCase,
            resolveGameMedia = appContainer.resolveGameMediaUseCase,
            observeVideoDelaySeconds = appContainer.observeVideoDelaySecondsUseCase,
            observeVideoAudioEnabled = appContainer.observeVideoAudioEnabledUseCase,
            observeVideoAspectRatioMode = appContainer.observeVideoAspectRatioModeUseCase,
        ) as T
    }
}
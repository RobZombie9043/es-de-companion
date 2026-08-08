package com.esde.companion.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.esde.companion.AppContainer

class MainViewModelFactory(
    private val appContainer: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(MainViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return MainViewModel(
            observeConnectionState = appContainer.observeConnectionStateUseCase,
            observeGamePlayingBehavior = appContainer.observeGamePlayingBehaviorUseCase,
            observeScreensaverBehavior = appContainer.observeScreensaverBehaviorUseCase,
            observeVideoPlaybackEnabled = appContainer.observeVideoPlaybackEnabledUseCase,
        ) as T
    }
}

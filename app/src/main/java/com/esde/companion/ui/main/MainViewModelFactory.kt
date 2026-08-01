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
            observeOverlayEnabled = appContainer.observeOverlayEnabledUseCase,
            observeGamePlayingBehavior = appContainer.observeGamePlayingBehaviorUseCase,
            observeScreensaverBehavior = appContainer.observeScreensaverBehaviorUseCase,
            resolveGameMedia = appContainer.resolveGameMediaUseCase,
            onboardingRepository = appContainer.onboardingRepository,
        ) as T
    }
}
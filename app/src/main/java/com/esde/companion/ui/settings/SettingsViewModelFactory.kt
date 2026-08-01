package com.esde.companion.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

import com.esde.companion.AppContainer

class SettingsViewModelFactory(
    private val appContainer: AppContainer,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return SettingsViewModel(
            onboardingRepository = appContainer.onboardingRepository,
            validateLogFolderUseCase = appContainer.validateEsdeLogFolderUseCase,
            validateMediaFolderUseCase = appContainer.validateEsdeMediaFolderUseCase,
            observeOverlayEnabledUseCase = appContainer.observeOverlayEnabledUseCase,
            setOverlayEnabledUseCase = appContainer.setOverlayEnabledUseCase,
            observeGamePlayingBehaviorUseCase = appContainer.observeGamePlayingBehaviorUseCase,
            setGamePlayingBehaviorUseCase = appContainer.setGamePlayingBehaviorUseCase,
            observeScreensaverBehaviorUseCase = appContainer.observeScreensaverBehaviorUseCase,
            setScreensaverBehaviorUseCase = appContainer.setScreensaverBehaviorUseCase,
            observeThemePreferenceUseCase = appContainer.observeThemePreferenceUseCase,
            setThemePreferenceUseCase = appContainer.setThemePreferenceUseCase,
            observeDrawerOpacityUseCase = appContainer.observeDrawerOpacityUseCase,
            setDrawerOpacityUseCase = appContainer.setDrawerOpacityUseCase,
            observeGridColumnsUseCase = appContainer.observeGridColumnsUseCase,
            setGridColumnsUseCase = appContainer.setGridColumnsUseCase,
            observeWidgetsLockedUseCase = appContainer.observeWidgetsLockedUseCase,
            setWidgetsLockedUseCase = appContainer.setWidgetsLockedUseCase,
        ) as T
    }
}
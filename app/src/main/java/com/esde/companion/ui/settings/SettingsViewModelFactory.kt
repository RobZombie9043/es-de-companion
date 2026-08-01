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
            observeDockEnabledUseCase = appContainer.observeDockEnabledUseCase,
            setDockEnabledUseCase = appContainer.setDockEnabledUseCase,
            observeDockMaxAppsUseCase = appContainer.observeDockMaxAppsUseCase,
            setDockMaxAppsUseCase = appContainer.setDockMaxAppsUseCase,
            observeDockSizeUseCase = appContainer.observeDockSizeUseCase,
            setDockSizeUseCase = appContainer.setDockSizeUseCase,
            observeDockOpacityUseCase = appContainer.observeDockOpacityUseCase,
            setDockOpacityUseCase = appContainer.setDockOpacityUseCase,
            observeWidgetsLockedUseCase = appContainer.observeWidgetsLockedUseCase,
            setWidgetsLockedUseCase = appContainer.setWidgetsLockedUseCase,
            observeVideoPlaybackEnabledUseCase = appContainer.observeVideoPlaybackEnabledUseCase,
            setVideoPlaybackEnabledUseCase = appContainer.setVideoPlaybackEnabledUseCase,
            observeVideoDelaySecondsUseCase = appContainer.observeVideoDelaySecondsUseCase,
            setVideoDelaySecondsUseCase = appContainer.setVideoDelaySecondsUseCase,
            observeVideoAudioEnabledUseCase = appContainer.observeVideoAudioEnabledUseCase,
            setVideoAudioEnabledUseCase = appContainer.setVideoAudioEnabledUseCase,
            observeVideoAspectRatioModeUseCase = appContainer.observeVideoAspectRatioModeUseCase,
            setVideoAspectRatioModeUseCase = appContainer.setVideoAspectRatioModeUseCase,
            observeMusicEnabledUseCase = appContainer.observeMusicEnabledUseCase,
            setMusicEnabledUseCase = appContainer.setMusicEnabledUseCase,
            observeMusicPlayWhileBrowsingSystemsUseCase = appContainer.observeMusicPlayWhileBrowsingSystemsUseCase,
            setMusicPlayWhileBrowsingSystemsUseCase = appContainer.setMusicPlayWhileBrowsingSystemsUseCase,
            observeMusicPlayWhileBrowsingGamesUseCase = appContainer.observeMusicPlayWhileBrowsingGamesUseCase,
            setMusicPlayWhileBrowsingGamesUseCase = appContainer.setMusicPlayWhileBrowsingGamesUseCase,
            observeMusicPlayDuringScreensaverUseCase = appContainer.observeMusicPlayDuringScreensaverUseCase,
            setMusicPlayDuringScreensaverUseCase = appContainer.setMusicPlayDuringScreensaverUseCase,
            observeMusicDuckingModeUseCase = appContainer.observeMusicDuckingModeUseCase,
            setMusicDuckingModeUseCase = appContainer.setMusicDuckingModeUseCase,
        ) as T
    }
}
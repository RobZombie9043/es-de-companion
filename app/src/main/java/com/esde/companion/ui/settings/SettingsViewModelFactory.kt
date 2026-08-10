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
            observeGamePlayingBehaviorUseCase = appContainer.observeGamePlayingBehaviorUseCase,
            setGamePlayingBehaviorUseCase = appContainer.setGamePlayingBehaviorUseCase,
            observeScreensaverBehaviorUseCase = appContainer.observeScreensaverBehaviorUseCase,
            setScreensaverBehaviorUseCase = appContainer.setScreensaverBehaviorUseCase,
            observeThemePreferenceUseCase = appContainer.observeThemePreferenceUseCase,
            setThemePreferenceUseCase = appContainer.setThemePreferenceUseCase,
            observeOverlayOpacityUseCase = appContainer.observeOverlayOpacityUseCase,
            setOverlayOpacityUseCase = appContainer.setOverlayOpacityUseCase,
            observeGridColumnsUseCase = appContainer.observeGridColumnsUseCase,
            setGridColumnsUseCase = appContainer.setGridColumnsUseCase,
            observeSortFoldersOnTopUseCase = appContainer.observeSortFoldersOnTopUseCase,
            setSortFoldersOnTopUseCase = appContainer.setSortFoldersOnTopUseCase,
            observeShowSearchBarUseCase = appContainer.observeShowSearchBarUseCase,
            setShowSearchBarUseCase = appContainer.setShowSearchBarUseCase,
            observeDockEnabledUseCase = appContainer.observeDockEnabledUseCase,
            setDockEnabledUseCase = appContainer.setDockEnabledUseCase,
            observeDockMaxAppsUseCase = appContainer.observeDockMaxAppsUseCase,
            setDockMaxAppsUseCase = appContainer.setDockMaxAppsUseCase,
            observeDockSizeUseCase = appContainer.observeDockSizeUseCase,
            setDockSizeUseCase = appContainer.setDockSizeUseCase,
            observeVideoPlaybackEnabledUseCase = appContainer.observeVideoPlaybackEnabledUseCase,
            setVideoPlaybackEnabledUseCase = appContainer.setVideoPlaybackEnabledUseCase,
            observeVideoDelaySecondsUseCase = appContainer.observeVideoDelaySecondsUseCase,
            setVideoDelaySecondsUseCase = appContainer.setVideoDelaySecondsUseCase,
            observeVideoAudioEnabledUseCase = appContainer.observeVideoAudioEnabledUseCase,
            setVideoAudioEnabledUseCase = appContainer.setVideoAudioEnabledUseCase,
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
            observeCloseCompanionOnQuitEnabledUseCase = appContainer.observeCloseCompanionOnQuitEnabledUseCase,
            setCloseCompanionOnQuitEnabledUseCase = appContainer.setCloseCompanionOnQuitEnabledUseCase,
            observeFabAssignmentsUseCase = appContainer.observeFabAssignmentsUseCase,
            setFabAssignmentUseCase = appContainer.setFabAssignmentUseCase,
            observeInstalledAppsUseCase = appContainer.observeInstalledAppsUseCase,
            observeLaunchEsdeOnStartEnabledUseCase = appContainer.observeLaunchEsdeOnStartEnabledUseCase,
            setLaunchEsdeOnStartEnabledUseCase = appContainer.setLaunchEsdeOnStartEnabledUseCase,
            observeDebugLoggingEnabledUseCase = appContainer.observeDebugLoggingEnabledUseCase,
            setDebugLoggingEnabledUseCase = appContainer.setDebugLoggingEnabledUseCase,
            exportConfigBackupUseCase = appContainer.exportConfigBackupUseCase,
            restoreConfigBackupUseCase = appContainer.restoreConfigBackupUseCase,
        ) as T
    }
}

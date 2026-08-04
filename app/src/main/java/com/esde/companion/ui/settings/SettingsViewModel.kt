package com.esde.companion.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esde.companion.data.storage.AllFilesAccessPermission
import com.esde.companion.domain.model.DockSize
import com.esde.companion.domain.model.ImageTransitionMode
import com.esde.companion.domain.model.LogoTransitionMode
import com.esde.companion.domain.model.MusicDuckingMode
import com.esde.companion.domain.model.ScreenBehavior
import com.esde.companion.domain.model.ThemePreference
import com.esde.companion.domain.repository.OnboardingRepository
import com.esde.companion.domain.usecase.ObserveDockEnabledUseCase
import com.esde.companion.domain.usecase.ObserveDockMaxAppsUseCase
import com.esde.companion.domain.usecase.ObserveDockSizeUseCase
import com.esde.companion.domain.usecase.ObserveGamePlayingBehaviorUseCase
import com.esde.companion.domain.usecase.ObserveGridColumnsUseCase
import com.esde.companion.domain.usecase.ObserveImageTransitionModeUseCase
import com.esde.companion.domain.usecase.ObserveLogoTransitionModeUseCase
import com.esde.companion.domain.usecase.ObserveMusicDuckingModeUseCase
import com.esde.companion.domain.usecase.ObserveMusicEnabledUseCase
import com.esde.companion.domain.usecase.ObserveMusicPlayDuringScreensaverUseCase
import com.esde.companion.domain.usecase.ObserveMusicPlayWhileBrowsingGamesUseCase
import com.esde.companion.domain.usecase.ObserveMusicPlayWhileBrowsingSystemsUseCase
import com.esde.companion.domain.usecase.ObserveOverlayOpacityUseCase
import com.esde.companion.domain.usecase.ObserveScreensaverBehaviorUseCase
import com.esde.companion.domain.usecase.ObserveThemePreferenceUseCase
import com.esde.companion.domain.usecase.ObserveVideoPlaybackEnabledUseCase
import com.esde.companion.domain.usecase.ObserveVideoDelaySecondsUseCase
import com.esde.companion.domain.usecase.ObserveVideoAudioEnabledUseCase
import com.esde.companion.domain.usecase.ObserveWidgetsLockedUseCase
import com.esde.companion.domain.usecase.SetDockEnabledUseCase
import com.esde.companion.domain.usecase.SetDockMaxAppsUseCase
import com.esde.companion.domain.usecase.SetDockSizeUseCase
import com.esde.companion.domain.usecase.SetGamePlayingBehaviorUseCase
import com.esde.companion.domain.usecase.SetGridColumnsUseCase
import com.esde.companion.domain.usecase.SetImageTransitionModeUseCase
import com.esde.companion.domain.usecase.SetLogoTransitionModeUseCase
import com.esde.companion.domain.usecase.SetMusicDuckingModeUseCase
import com.esde.companion.domain.usecase.SetMusicEnabledUseCase
import com.esde.companion.domain.usecase.SetMusicPlayDuringScreensaverUseCase
import com.esde.companion.domain.usecase.SetMusicPlayWhileBrowsingGamesUseCase
import com.esde.companion.domain.usecase.SetMusicPlayWhileBrowsingSystemsUseCase
import com.esde.companion.domain.usecase.SetOverlayOpacityUseCase
import com.esde.companion.domain.usecase.SetScreensaverBehaviorUseCase
import com.esde.companion.domain.usecase.SetThemePreferenceUseCase
import com.esde.companion.domain.usecase.SetVideoAudioEnabledUseCase
import com.esde.companion.domain.usecase.SetVideoDelaySecondsUseCase
import com.esde.companion.domain.usecase.SetVideoPlaybackEnabledUseCase
import com.esde.companion.domain.usecase.SetWidgetsLockedUseCase
import com.esde.companion.domain.usecase.ValidateEsdeLogFolderUseCase
import com.esde.companion.domain.usecase.ValidateEsdeMediaFolderUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val onboardingRepository: OnboardingRepository,
    private val validateLogFolderUseCase: ValidateEsdeLogFolderUseCase,
    private val validateMediaFolderUseCase: ValidateEsdeMediaFolderUseCase,
    private val observeGamePlayingBehaviorUseCase: ObserveGamePlayingBehaviorUseCase,
    private val setGamePlayingBehaviorUseCase: SetGamePlayingBehaviorUseCase,
    private val observeScreensaverBehaviorUseCase: ObserveScreensaverBehaviorUseCase,
    private val setScreensaverBehaviorUseCase: SetScreensaverBehaviorUseCase,
    private val observeThemePreferenceUseCase: ObserveThemePreferenceUseCase,
    private val setThemePreferenceUseCase: SetThemePreferenceUseCase,
    private val observeImageTransitionModeUseCase: ObserveImageTransitionModeUseCase,
    private val setImageTransitionModeUseCase: SetImageTransitionModeUseCase,
    private val observeLogoTransitionModeUseCase: ObserveLogoTransitionModeUseCase,
    private val setLogoTransitionModeUseCase: SetLogoTransitionModeUseCase,
    private val observeOverlayOpacityUseCase: ObserveOverlayOpacityUseCase,
    private val setOverlayOpacityUseCase: SetOverlayOpacityUseCase,
    private val observeGridColumnsUseCase: ObserveGridColumnsUseCase,
    private val setGridColumnsUseCase: SetGridColumnsUseCase,
    private val observeDockEnabledUseCase: ObserveDockEnabledUseCase,
    private val setDockEnabledUseCase: SetDockEnabledUseCase,
    private val observeDockMaxAppsUseCase: ObserveDockMaxAppsUseCase,
    private val setDockMaxAppsUseCase: SetDockMaxAppsUseCase,
    private val observeDockSizeUseCase: ObserveDockSizeUseCase,
    private val setDockSizeUseCase: SetDockSizeUseCase,
    private val observeWidgetsLockedUseCase: ObserveWidgetsLockedUseCase,
    private val setWidgetsLockedUseCase: SetWidgetsLockedUseCase,
    private val observeVideoPlaybackEnabledUseCase: ObserveVideoPlaybackEnabledUseCase,
    private val setVideoPlaybackEnabledUseCase: SetVideoPlaybackEnabledUseCase,
    private val observeVideoDelaySecondsUseCase: ObserveVideoDelaySecondsUseCase,
    private val setVideoDelaySecondsUseCase: SetVideoDelaySecondsUseCase,
    private val observeVideoAudioEnabledUseCase: ObserveVideoAudioEnabledUseCase,
    private val setVideoAudioEnabledUseCase: SetVideoAudioEnabledUseCase,
    private val observeMusicEnabledUseCase: ObserveMusicEnabledUseCase,
    private val setMusicEnabledUseCase: SetMusicEnabledUseCase,
    private val observeMusicPlayWhileBrowsingSystemsUseCase: ObserveMusicPlayWhileBrowsingSystemsUseCase,
    private val setMusicPlayWhileBrowsingSystemsUseCase: SetMusicPlayWhileBrowsingSystemsUseCase,
    private val observeMusicPlayWhileBrowsingGamesUseCase: ObserveMusicPlayWhileBrowsingGamesUseCase,
    private val setMusicPlayWhileBrowsingGamesUseCase: SetMusicPlayWhileBrowsingGamesUseCase,
    private val observeMusicPlayDuringScreensaverUseCase: ObserveMusicPlayDuringScreensaverUseCase,
    private val setMusicPlayDuringScreensaverUseCase: SetMusicPlayDuringScreensaverUseCase,
    private val observeMusicDuckingModeUseCase: ObserveMusicDuckingModeUseCase,
    private val setMusicDuckingModeUseCase: SetMusicDuckingModeUseCase,
) : ViewModel() {

    // Seeded with the real value up front - see OnboardingViewModel's kdoc for why
    // relying solely on the screen's ON_RESUME DisposableEffect isn't sufficient.
    private val _uiState = MutableStateFlow(
        SettingsUiState(permissionGranted = AllFilesAccessPermission.isGranted()),
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val logPath = onboardingRepository.observeLogFolderPath().first()
                ?: onboardingRepository.defaultLogFolderPath()
            val mediaPath = onboardingRepository.observeMediaFolderPath().first()
                ?: onboardingRepository.defaultMediaFolderPath()
            val customSystemImagesPath = onboardingRepository.observeCustomSystemImagesFolderPath().first()
            val customLogosPath = onboardingRepository.observeCustomLogosFolderPath().first()
            val customMusicPath = onboardingRepository.observeCustomMusicFolderPath().first()
            val gamePlayingBehavior = observeGamePlayingBehaviorUseCase().first()
            val screensaverBehavior = observeScreensaverBehaviorUseCase().first()
            val themePreference = observeThemePreferenceUseCase().first()
            val imageTransitionMode = observeImageTransitionModeUseCase().first()
            val logoTransitionMode = observeLogoTransitionModeUseCase().first()
            val overlayOpacityPercent = observeOverlayOpacityUseCase().first()
            val gridColumns = observeGridColumnsUseCase().first()
            val dockEnabled = observeDockEnabledUseCase().first()
            val dockMaxApps = observeDockMaxAppsUseCase().first()
            val dockSize = observeDockSizeUseCase().first()
            val widgetsLocked = observeWidgetsLockedUseCase().first()
            val videoPlaybackEnabled = observeVideoPlaybackEnabledUseCase().first()
            val videoDelaySeconds = observeVideoDelaySecondsUseCase().first()
            val videoAudioEnabled = observeVideoAudioEnabledUseCase().first()
            val musicEnabled = observeMusicEnabledUseCase().first()
            val musicPlayWhileBrowsingSystems = observeMusicPlayWhileBrowsingSystemsUseCase().first()
            val musicPlayWhileBrowsingGames = observeMusicPlayWhileBrowsingGamesUseCase().first()
            val musicPlayDuringScreensaver = observeMusicPlayDuringScreensaverUseCase().first()
            val musicDuckingMode = observeMusicDuckingModeUseCase().first()
            _uiState.value = _uiState.value.copy(
                logFolderPath = logPath,
                mediaFolderPath = mediaPath,
                customSystemImagesFolderPath = customSystemImagesPath,
                customLogosFolderPath = customLogosPath,
                customMusicFolderPath = customMusicPath,
                gamePlayingBehavior = gamePlayingBehavior,
                screensaverBehavior = screensaverBehavior,
                themePreference = themePreference,
                imageTransitionMode = imageTransitionMode,
                logoTransitionMode = logoTransitionMode,
                overlayOpacityPercent = overlayOpacityPercent,
                gridColumns = gridColumns,
                dockEnabled = dockEnabled,
                dockMaxApps = dockMaxApps,
                dockSize = dockSize,
                widgetsLocked = widgetsLocked,
                videoPlaybackEnabled = videoPlaybackEnabled,
                videoDelaySeconds = videoDelaySeconds,
                videoAudioEnabled = videoAudioEnabled,
                musicEnabled = musicEnabled,
                musicPlayWhileBrowsingSystems = musicPlayWhileBrowsingSystems,
                musicPlayWhileBrowsingGames = musicPlayWhileBrowsingGames,
                musicPlayDuringScreensaver = musicPlayDuringScreensaver,
                musicDuckingMode = musicDuckingMode,
            )
            validateLogFolder(logPath)
            validateMediaFolder(mediaPath)
            customSystemImagesPath?.let { validateCustomSystemImagesFolder(it) }
            customLogosPath?.let { validateCustomLogosFolder(it) }
            customMusicPath?.let { validateCustomMusicFolder(it) }
        }
    }

    fun refreshPermissionState(granted: Boolean) {
        _uiState.value = _uiState.value.copy(permissionGranted = granted)
    }

    fun onLogFolderPicked(path: String) {
        _uiState.value = _uiState.value.copy(logFolderPath = path)
        viewModelScope.launch {
            validateLogFolder(path)
            onboardingRepository.saveLogFolderPath(path)
        }
    }

    fun onMediaFolderPicked(path: String) {
        _uiState.value = _uiState.value.copy(mediaFolderPath = path)
        viewModelScope.launch {
            validateMediaFolder(path)
            onboardingRepository.saveMediaFolderPath(path)
        }
    }

    fun onGamePlayingBehaviorChanged(behavior: ScreenBehavior) {
        _uiState.value = _uiState.value.copy(gamePlayingBehavior = behavior)
        viewModelScope.launch { setGamePlayingBehaviorUseCase(behavior) }
    }

    fun onScreensaverBehaviorChanged(behavior: ScreenBehavior) {
        _uiState.value = _uiState.value.copy(screensaverBehavior = behavior)
        viewModelScope.launch { setScreensaverBehaviorUseCase(behavior) }
    }

    fun onVideoPlaybackEnabledChanged(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(videoPlaybackEnabled = enabled)
        viewModelScope.launch { setVideoPlaybackEnabledUseCase(enabled) }
    }

    fun onVideoDelaySecondsChanged(seconds: Int) {
        _uiState.value = _uiState.value.copy(videoDelaySeconds = seconds)
        viewModelScope.launch { setVideoDelaySecondsUseCase(seconds) }
    }

    fun onVideoAudioEnabledChanged(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(videoAudioEnabled = enabled)
        viewModelScope.launch { setVideoAudioEnabledUseCase(enabled) }
    }

    fun onMusicEnabledChanged(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(musicEnabled = enabled)
        viewModelScope.launch { setMusicEnabledUseCase(enabled) }
    }

    fun onMusicPlayWhileBrowsingSystemsChanged(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(musicPlayWhileBrowsingSystems = enabled)
        viewModelScope.launch { setMusicPlayWhileBrowsingSystemsUseCase(enabled) }
    }

    fun onMusicPlayWhileBrowsingGamesChanged(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(musicPlayWhileBrowsingGames = enabled)
        viewModelScope.launch { setMusicPlayWhileBrowsingGamesUseCase(enabled) }
    }

    fun onMusicPlayDuringScreensaverChanged(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(musicPlayDuringScreensaver = enabled)
        viewModelScope.launch { setMusicPlayDuringScreensaverUseCase(enabled) }
    }

    fun onMusicDuckingModeChanged(mode: MusicDuckingMode) {
        _uiState.value = _uiState.value.copy(musicDuckingMode = mode)
        viewModelScope.launch { setMusicDuckingModeUseCase(mode) }
    }

    fun onCustomMusicFolderPicked(path: String) {
        _uiState.value = _uiState.value.copy(customMusicFolderPath = path)
        viewModelScope.launch {
            validateCustomMusicFolder(path)
            onboardingRepository.saveCustomMusicFolderPath(path)
        }
    }

    fun onCustomMusicFolderCleared() {
        _uiState.value = _uiState.value.copy(customMusicFolderPath = null, customMusicFolderValidation = null)
        viewModelScope.launch { onboardingRepository.clearCustomMusicFolderPath() }
    }

    private suspend fun validateCustomMusicFolder(path: String) {
        _uiState.value = _uiState.value.copy(isValidatingCustomMusicFolder = true)
        val result = validateMediaFolderUseCase(path)
        _uiState.value = _uiState.value.copy(isValidatingCustomMusicFolder = false, customMusicFolderValidation = result)
    }

    fun onWidgetsLockedChanged(locked: Boolean) {
        _uiState.value = _uiState.value.copy(widgetsLocked = locked)
        viewModelScope.launch { setWidgetsLockedUseCase(locked) }
    }

    fun onThemePreferenceChanged(preference: ThemePreference) {
        _uiState.value = _uiState.value.copy(themePreference = preference)
        viewModelScope.launch { setThemePreferenceUseCase(preference) }
    }

    fun onImageTransitionModeChanged(mode: ImageTransitionMode) {
        _uiState.value = _uiState.value.copy(imageTransitionMode = mode)
        viewModelScope.launch { setImageTransitionModeUseCase(mode) }
    }

    fun onLogoTransitionModeChanged(mode: LogoTransitionMode) {
        _uiState.value = _uiState.value.copy(logoTransitionMode = mode)
        viewModelScope.launch { setLogoTransitionModeUseCase(mode) }
    }

    fun onOverlayOpacityChanged(percent: Int) {
        _uiState.value = _uiState.value.copy(overlayOpacityPercent = percent)
        viewModelScope.launch { setOverlayOpacityUseCase(percent) }
    }

    fun onGridColumnsChanged(columns: Int) {
        _uiState.value = _uiState.value.copy(gridColumns = columns)
        viewModelScope.launch { setGridColumnsUseCase(columns) }
    }

    fun onDockEnabledChanged(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(dockEnabled = enabled)
        viewModelScope.launch { setDockEnabledUseCase(enabled) }
    }

    fun onDockMaxAppsChanged(maxApps: Int) {
        _uiState.value = _uiState.value.copy(dockMaxApps = maxApps)
        viewModelScope.launch { setDockMaxAppsUseCase(maxApps) }
    }

    fun onDockSizeChanged(size: DockSize) {
        _uiState.value = _uiState.value.copy(dockSize = size)
        viewModelScope.launch { setDockSizeUseCase(size) }
    }

    private suspend fun validateLogFolder(path: String) {
        _uiState.value = _uiState.value.copy(isValidatingLogFolder = true)
        val result = validateLogFolderUseCase(path)
        _uiState.value = _uiState.value.copy(isValidatingLogFolder = false, logFolderValidation = result)
    }

    private suspend fun validateMediaFolder(path: String) {
        _uiState.value = _uiState.value.copy(isValidatingMediaFolder = true)
        val result = validateMediaFolderUseCase(path)
        _uiState.value = _uiState.value.copy(isValidatingMediaFolder = false, mediaFolderValidation = result)
    }

    fun onCustomSystemImagesFolderPicked(path: String) {
        _uiState.value = _uiState.value.copy(customSystemImagesFolderPath = path)
        viewModelScope.launch {
            validateCustomSystemImagesFolder(path)
            onboardingRepository.saveCustomSystemImagesFolderPath(path)
        }
    }

    fun onCustomSystemImagesFolderCleared() {
        _uiState.value = _uiState.value.copy(
            customSystemImagesFolderPath = null,
            customSystemImagesFolderValidation = null,
        )
        viewModelScope.launch { onboardingRepository.clearCustomSystemImagesFolderPath() }
    }

    fun onCustomLogosFolderPicked(path: String) {
        _uiState.value = _uiState.value.copy(customLogosFolderPath = path)
        viewModelScope.launch {
            validateCustomLogosFolder(path)
            onboardingRepository.saveCustomLogosFolderPath(path)
        }
    }

    fun onCustomLogosFolderCleared() {
        _uiState.value = _uiState.value.copy(customLogosFolderPath = null, customLogosFolderValidation = null)
        viewModelScope.launch { onboardingRepository.clearCustomLogosFolderPath() }
    }

    private suspend fun validateCustomSystemImagesFolder(path: String) {
        _uiState.value = _uiState.value.copy(isValidatingCustomSystemImagesFolder = true)
        val result = validateMediaFolderUseCase(path)
        _uiState.value = _uiState.value.copy(
            isValidatingCustomSystemImagesFolder = false,
            customSystemImagesFolderValidation = result,
        )
    }

    private suspend fun validateCustomLogosFolder(path: String) {
        _uiState.value = _uiState.value.copy(isValidatingCustomLogosFolder = true)
        val result = validateMediaFolderUseCase(path)
        _uiState.value = _uiState.value.copy(isValidatingCustomLogosFolder = false, customLogosFolderValidation = result)
    }
}
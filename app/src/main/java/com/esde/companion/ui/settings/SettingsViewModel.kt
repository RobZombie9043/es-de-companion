package com.esde.companion.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esde.companion.data.storage.AllFilesAccessPermission
import com.esde.companion.domain.model.DockSize
import com.esde.companion.domain.model.FabPosition
import com.esde.companion.domain.model.FabSlot
import com.esde.companion.domain.model.FabType
import com.esde.companion.domain.model.MusicDuckingMode
import com.esde.companion.domain.model.ScreenBehavior
import com.esde.companion.domain.model.ThemePreference
import com.esde.companion.domain.repository.OnboardingRepository
import com.esde.companion.domain.usecase.ExportConfigBackupUseCase
import com.esde.companion.domain.usecase.ObserveCloseCompanionOnQuitEnabledUseCase
import com.esde.companion.domain.usecase.ObserveDebugLoggingEnabledUseCase
import com.esde.companion.domain.usecase.ObserveDockEnabledUseCase
import com.esde.companion.domain.usecase.ObserveDockMaxAppsUseCase
import com.esde.companion.domain.usecase.ObserveDockSizeUseCase
import com.esde.companion.domain.usecase.ObserveFabAssignmentsUseCase
import com.esde.companion.domain.usecase.ObserveGamePlayingBehaviorUseCase
import com.esde.companion.domain.usecase.ObserveGridColumnsUseCase
import com.esde.companion.domain.usecase.ObserveInstalledAppsUseCase
import com.esde.companion.domain.usecase.ObserveLaunchEsdeOnStartEnabledUseCase
import com.esde.companion.domain.usecase.ObserveMusicDuckingModeUseCase
import com.esde.companion.domain.usecase.ObserveMusicEnabledUseCase
import com.esde.companion.domain.usecase.ObserveMusicPlayDuringScreensaverUseCase
import com.esde.companion.domain.usecase.ObserveMusicPlayWhileBrowsingGamesUseCase
import com.esde.companion.domain.usecase.ObserveMusicPlayWhileBrowsingSystemsUseCase
import com.esde.companion.domain.usecase.ObserveOverlayOpacityUseCase
import com.esde.companion.domain.usecase.ObserveScreensaverBehaviorUseCase
import com.esde.companion.domain.usecase.ObserveShowSearchBarUseCase
import com.esde.companion.domain.usecase.ObserveSortFoldersOnTopUseCase
import com.esde.companion.domain.usecase.ObserveThemePreferenceUseCase
import com.esde.companion.domain.usecase.ObserveVideoAudioEnabledUseCase
import com.esde.companion.domain.usecase.ObserveVideoDelaySecondsUseCase
import com.esde.companion.domain.usecase.ObserveVideoPlaybackEnabledUseCase
import com.esde.companion.domain.usecase.RestoreConfigBackupUseCase
import com.esde.companion.domain.usecase.SetCloseCompanionOnQuitEnabledUseCase
import com.esde.companion.domain.usecase.SetDebugLoggingEnabledUseCase
import com.esde.companion.domain.usecase.SetDockEnabledUseCase
import com.esde.companion.domain.usecase.SetDockMaxAppsUseCase
import com.esde.companion.domain.usecase.SetDockSizeUseCase
import com.esde.companion.domain.usecase.SetFabAssignmentUseCase
import com.esde.companion.domain.usecase.SetGamePlayingBehaviorUseCase
import com.esde.companion.domain.usecase.SetGridColumnsUseCase
import com.esde.companion.domain.usecase.SetLaunchEsdeOnStartEnabledUseCase
import com.esde.companion.domain.usecase.SetMusicDuckingModeUseCase
import com.esde.companion.domain.usecase.SetMusicEnabledUseCase
import com.esde.companion.domain.usecase.SetMusicPlayDuringScreensaverUseCase
import com.esde.companion.domain.usecase.SetMusicPlayWhileBrowsingGamesUseCase
import com.esde.companion.domain.usecase.SetMusicPlayWhileBrowsingSystemsUseCase
import com.esde.companion.domain.usecase.SetOverlayOpacityUseCase
import com.esde.companion.domain.usecase.SetScreensaverBehaviorUseCase
import com.esde.companion.domain.usecase.SetShowSearchBarUseCase
import com.esde.companion.domain.usecase.SetSortFoldersOnTopUseCase
import com.esde.companion.domain.usecase.SetThemePreferenceUseCase
import com.esde.companion.domain.usecase.SetVideoAudioEnabledUseCase
import com.esde.companion.domain.usecase.SetVideoDelaySecondsUseCase
import com.esde.companion.domain.usecase.SetVideoPlaybackEnabledUseCase
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
    private val observeOverlayOpacityUseCase: ObserveOverlayOpacityUseCase,
    private val setOverlayOpacityUseCase: SetOverlayOpacityUseCase,
    private val observeGridColumnsUseCase: ObserveGridColumnsUseCase,
    private val setGridColumnsUseCase: SetGridColumnsUseCase,
    private val observeSortFoldersOnTopUseCase: ObserveSortFoldersOnTopUseCase,
    private val setSortFoldersOnTopUseCase: SetSortFoldersOnTopUseCase,
    private val observeShowSearchBarUseCase: ObserveShowSearchBarUseCase,
    private val setShowSearchBarUseCase: SetShowSearchBarUseCase,
    private val observeDockEnabledUseCase: ObserveDockEnabledUseCase,
    private val setDockEnabledUseCase: SetDockEnabledUseCase,
    private val observeDockMaxAppsUseCase: ObserveDockMaxAppsUseCase,
    private val setDockMaxAppsUseCase: SetDockMaxAppsUseCase,
    private val observeDockSizeUseCase: ObserveDockSizeUseCase,
    private val setDockSizeUseCase: SetDockSizeUseCase,
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
    private val observeCloseCompanionOnQuitEnabledUseCase: ObserveCloseCompanionOnQuitEnabledUseCase,
    private val setCloseCompanionOnQuitEnabledUseCase: SetCloseCompanionOnQuitEnabledUseCase,
    private val observeFabAssignmentsUseCase: ObserveFabAssignmentsUseCase,
    private val setFabAssignmentUseCase: SetFabAssignmentUseCase,
    private val observeInstalledAppsUseCase: ObserveInstalledAppsUseCase,
    private val observeLaunchEsdeOnStartEnabledUseCase: ObserveLaunchEsdeOnStartEnabledUseCase,
    private val setLaunchEsdeOnStartEnabledUseCase: SetLaunchEsdeOnStartEnabledUseCase,
    private val observeDebugLoggingEnabledUseCase: ObserveDebugLoggingEnabledUseCase,
    private val setDebugLoggingEnabledUseCase: SetDebugLoggingEnabledUseCase,
    private val exportConfigBackupUseCase: ExportConfigBackupUseCase,
    private val restoreConfigBackupUseCase: RestoreConfigBackupUseCase,
) : ViewModel() {
    // Seeded with the real value up front - see OnboardingViewModel's kdoc for why
    // relying solely on the screen's ON_RESUME DisposableEffect isn't sufficient.
    private val _uiState =
        MutableStateFlow(
            SettingsUiState(permissionGranted = AllFilesAccessPermission.isGranted()),
        )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // Continuously collected, unlike everything else loaded once below - installed
        // apps can change independently of any setting this screen itself changes (the
        // user installing/uninstalling something while Settings happens to be open), so a
        // one-shot snapshot would go stale. Backs the Custom App FAB's app picker.
        viewModelScope.launch {
            observeInstalledAppsUseCase().collect { apps ->
                _uiState.value = _uiState.value.copy(installedApps = apps)
            }
        }
        viewModelScope.launch { reloadSettingsState() }
    }

    /**
     * Loads every setting this screen shows from its repository/use case, replacing
     * whatever's currently in [_uiState]. Runs once at startup (see [init]) and again after
     * a successful [restoreBackup] - a bulk restore changes settings out from under this
     * ViewModel's own individual `onXChanged` mutators, so the UI state needs a full reload
     * rather than trusting it's still in sync.
     */
    private suspend fun reloadSettingsState() {
        val loaded = readSettingsFromRepositories()
        _uiState.value = loaded
        validateLogFolder(loaded.logFolderPath)
        validateMediaFolder(loaded.mediaFolderPath)
        loaded.customSystemImagesFolderPath?.let { validateCustomSystemImagesFolder(it) }
        loaded.customLogosFolderPath?.let { validateCustomLogosFolder(it) }
        loaded.customMusicFolderPath?.let { validateCustomMusicFolder(it) }
    }

    /** The read half of [reloadSettingsState] - split out purely to stay under detekt's
     * LongMethod threshold once every field this ViewModel loads lives in one place. */
    private suspend fun readSettingsFromRepositories(): SettingsUiState {
        val logPath =
            onboardingRepository.observeLogFolderPath().first()
                ?: onboardingRepository.defaultLogFolderPath()
        val mediaPath =
            onboardingRepository.observeMediaFolderPath().first()
                ?: onboardingRepository.defaultMediaFolderPath()
        return _uiState.value.copy(
            logFolderPath = logPath,
            mediaFolderPath = mediaPath,
            customSystemImagesFolderPath = onboardingRepository.observeCustomSystemImagesFolderPath().first(),
            customLogosFolderPath = onboardingRepository.observeCustomLogosFolderPath().first(),
            customMusicFolderPath = onboardingRepository.observeCustomMusicFolderPath().first(),
            gamePlayingBehavior = observeGamePlayingBehaviorUseCase().first(),
            screensaverBehavior = observeScreensaverBehaviorUseCase().first(),
            themePreference = observeThemePreferenceUseCase().first(),
            overlayOpacityPercent = observeOverlayOpacityUseCase().first(),
            gridColumns = observeGridColumnsUseCase().first(),
            sortFoldersOnTop = observeSortFoldersOnTopUseCase().first(),
            showSearchBar = observeShowSearchBarUseCase().first(),
            dockEnabled = observeDockEnabledUseCase().first(),
            dockMaxApps = observeDockMaxAppsUseCase().first(),
            dockSize = observeDockSizeUseCase().first(),
            videoPlaybackEnabled = observeVideoPlaybackEnabledUseCase().first(),
            videoDelaySeconds = observeVideoDelaySecondsUseCase().first(),
            videoAudioEnabled = observeVideoAudioEnabledUseCase().first(),
            musicEnabled = observeMusicEnabledUseCase().first(),
            musicPlayWhileBrowsingSystems = observeMusicPlayWhileBrowsingSystemsUseCase().first(),
            musicPlayWhileBrowsingGames = observeMusicPlayWhileBrowsingGamesUseCase().first(),
            musicPlayDuringScreensaver = observeMusicPlayDuringScreensaverUseCase().first(),
            musicDuckingMode = observeMusicDuckingModeUseCase().first(),
            closeCompanionOnQuitEnabled = observeCloseCompanionOnQuitEnabledUseCase().first(),
            fabAssignments = observeFabAssignmentsUseCase().first(),
            launchEsdeOnStartEnabled = observeLaunchEsdeOnStartEnabledUseCase().first(),
            debugLoggingEnabled = observeDebugLoggingEnabledUseCase().first(),
        )
    }

    /** Settings > Setup > Backup & Restore's Export Backup action - see
     * [ExportConfigBackupUseCase]. Returns the serialized backup content; the caller (the
     * Composable that owns the SAF "Save As" picker result) writes it to the chosen Uri. */
    suspend fun exportBackupJson(): String = exportConfigBackupUseCase()

    /** Settings > Setup > Backup & Restore's Restore Backup action - see
     * [RestoreConfigBackupUseCase]. [jsonText] is the already-read contents of the
     * user-picked file (the caller reads it via ConfigBackupFileIo). Reloads this screen's
     * own state from the repositories on success, since a bulk restore changes settings
     * this ViewModel didn't itself mutate. */
    suspend fun restoreBackup(jsonText: String): Result<Unit> =
        restoreConfigBackupUseCase(jsonText).also { result ->
            if (result.isSuccess) reloadSettingsState()
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

    fun onCloseCompanionOnQuitEnabledChanged(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(closeCompanionOnQuitEnabled = enabled)
        viewModelScope.launch { setCloseCompanionOnQuitEnabledUseCase(enabled) }
    }

    // Resets any previously-selected custom app - switching type away from CustomApp and
    // back always starts from an unset selection rather than silently resurrecting a
    // stale one.
    fun onFabTypeChanged(
        position: FabPosition,
        fabType: FabType,
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(fabAssignments = setFabAssignmentUseCase(position, FabSlot(fabType)))
        }
    }

    fun onFabCustomAppChanged(
        position: FabPosition,
        packageName: String,
    ) {
        viewModelScope.launch {
            val slot = FabSlot(FabType.CustomApp, packageName)
            _uiState.value = _uiState.value.copy(fabAssignments = setFabAssignmentUseCase(position, slot))
        }
    }

    fun onLaunchEsdeOnStartEnabledChanged(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(launchEsdeOnStartEnabled = enabled)
        viewModelScope.launch { setLaunchEsdeOnStartEnabledUseCase(enabled) }
    }

    fun onDebugLoggingEnabledChanged(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(debugLoggingEnabled = enabled)
        viewModelScope.launch { setDebugLoggingEnabledUseCase(enabled) }
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

    fun onThemePreferenceChanged(preference: ThemePreference) {
        _uiState.value = _uiState.value.copy(themePreference = preference)
        viewModelScope.launch { setThemePreferenceUseCase(preference) }
    }

    fun onOverlayOpacityChanged(percent: Int) {
        _uiState.value = _uiState.value.copy(overlayOpacityPercent = percent)
        viewModelScope.launch { setOverlayOpacityUseCase(percent) }
    }

    fun onGridColumnsChanged(columns: Int) {
        _uiState.value = _uiState.value.copy(gridColumns = columns)
        viewModelScope.launch { setGridColumnsUseCase(columns) }
    }

    fun onSortFoldersOnTopChanged(sortOnTop: Boolean) {
        _uiState.value = _uiState.value.copy(sortFoldersOnTop = sortOnTop)
        viewModelScope.launch { setSortFoldersOnTopUseCase(sortOnTop) }
    }

    fun onShowSearchBarChanged(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSearchBar = show)
        viewModelScope.launch { setShowSearchBarUseCase(show) }
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
        _uiState.value =
            _uiState.value.copy(
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
        _uiState.value =
            _uiState.value.copy(
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

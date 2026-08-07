package com.esde.companion.ui.settings

import com.esde.companion.domain.model.DockSize
import com.esde.companion.domain.model.LogFolderValidation
import com.esde.companion.domain.model.MediaFolderValidation
import com.esde.companion.domain.model.MusicDuckingMode
import com.esde.companion.domain.model.ScreenBehavior
import com.esde.companion.domain.model.ThemePreference
import com.esde.companion.domain.repository.AppDrawerSettingsRepository
import com.esde.companion.domain.repository.DockSettingsRepository
import com.esde.companion.domain.repository.OnboardingRepository
import com.esde.companion.domain.usecase.ObserveCloseCompanionOnQuitEnabledUseCase
import com.esde.companion.domain.usecase.ObserveDockEnabledUseCase
import com.esde.companion.domain.usecase.ObserveDockMaxAppsUseCase
import com.esde.companion.domain.usecase.ObserveDockSizeUseCase
import com.esde.companion.domain.usecase.ObserveGamePlayingBehaviorUseCase
import com.esde.companion.domain.usecase.ObserveGridColumnsUseCase
import com.esde.companion.domain.usecase.ObserveMusicDuckingModeUseCase
import com.esde.companion.domain.usecase.ObserveMusicEnabledUseCase
import com.esde.companion.domain.usecase.ObserveMusicPlayDuringScreensaverUseCase
import com.esde.companion.domain.usecase.ObserveMusicPlayWhileBrowsingGamesUseCase
import com.esde.companion.domain.usecase.ObserveMusicPlayWhileBrowsingSystemsUseCase
import com.esde.companion.domain.usecase.ObserveOverlayOpacityUseCase
import com.esde.companion.domain.usecase.ObserveScreensaverBehaviorUseCase
import com.esde.companion.domain.usecase.ObserveSettingsFabVisibleUseCase
import com.esde.companion.domain.usecase.ObserveSortFoldersOnTopUseCase
import com.esde.companion.domain.usecase.ObserveThemePreferenceUseCase
import com.esde.companion.domain.usecase.ObserveVideoAudioEnabledUseCase
import com.esde.companion.domain.usecase.ObserveVideoDelaySecondsUseCase
import com.esde.companion.domain.usecase.ObserveVideoPlaybackEnabledUseCase
import com.esde.companion.domain.usecase.SetCloseCompanionOnQuitEnabledUseCase
import com.esde.companion.domain.usecase.SetDockEnabledUseCase
import com.esde.companion.domain.usecase.SetDockMaxAppsUseCase
import com.esde.companion.domain.usecase.SetDockSizeUseCase
import com.esde.companion.domain.usecase.SetGamePlayingBehaviorUseCase
import com.esde.companion.domain.usecase.SetGridColumnsUseCase
import com.esde.companion.domain.usecase.SetMusicDuckingModeUseCase
import com.esde.companion.domain.usecase.SetMusicEnabledUseCase
import com.esde.companion.domain.usecase.SetMusicPlayDuringScreensaverUseCase
import com.esde.companion.domain.usecase.SetMusicPlayWhileBrowsingGamesUseCase
import com.esde.companion.domain.usecase.SetMusicPlayWhileBrowsingSystemsUseCase
import com.esde.companion.domain.usecase.SetOverlayOpacityUseCase
import com.esde.companion.domain.usecase.SetScreensaverBehaviorUseCase
import com.esde.companion.domain.usecase.SetSettingsFabVisibleUseCase
import com.esde.companion.domain.usecase.SetSortFoldersOnTopUseCase
import com.esde.companion.domain.usecase.SetThemePreferenceUseCase
import com.esde.companion.domain.usecase.SetVideoAudioEnabledUseCase
import com.esde.companion.domain.usecase.SetVideoDelaySecondsUseCase
import com.esde.companion.domain.usecase.SetVideoPlaybackEnabledUseCase
import com.esde.companion.domain.usecase.ValidateEsdeLogFolderUseCase
import com.esde.companion.domain.usecase.ValidateEsdeMediaFolderUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SettingsViewModelTest {

    private class FakeOnboardingRepository : OnboardingRepository {
        var gamePlayingBehavior = ScreenBehavior.Nothing
        var videoPlaybackEnabled = false
        var videoDelaySeconds = 0
        var videoAudioEnabled = true
        var screensaverBehavior = ScreenBehavior.Nothing
        var themePreference = ThemePreference.Auto
        var musicEnabled = true
        var musicPlayWhileBrowsingSystems = true
        var musicPlayWhileBrowsingGames = true
        var musicPlayDuringScreensaver = true
        var musicDuckingMode = MusicDuckingMode.LowerVolume
        var overlayOpacityPercent = 80
        var closeCompanionOnQuitEnabled = false
        var settingsFabVisible = true

        override fun defaultLogFolderPath() = "/storage/emulated/0/ES-DE"
        override fun defaultMediaFolderPath() = "/storage/emulated/0/ES-DE/downloaded_media"
        override suspend fun validateLogFolder(path: String) = LogFolderValidation.FolderFound(true)
        override suspend fun validateMediaFolder(path: String) = MediaFolderValidation.FolderFound
        override suspend fun saveLogFolderPath(path: String) {}
        override suspend fun saveMediaFolderPath(path: String) {}
        override fun observeLogFolderPath(): Flow<String?> = flowOf(null)
        override fun observeMediaFolderPath(): Flow<String?> = flowOf(null)
        override suspend fun saveCustomSystemImagesFolderPath(path: String) {}
        override fun observeCustomSystemImagesFolderPath(): Flow<String?> = flowOf(null)
        override suspend fun clearCustomSystemImagesFolderPath() {}
        override suspend fun saveCustomLogosFolderPath(path: String) {}
        override fun observeCustomLogosFolderPath(): Flow<String?> = flowOf(null)
        override suspend fun clearCustomLogosFolderPath() {}
        override suspend fun markOnboardingComplete() {}
        override fun observeOnboardingComplete(): Flow<Boolean> = flowOf(false)
        override suspend fun setVideoPlaybackEnabled(enabled: Boolean) { videoPlaybackEnabled = enabled }
        override fun observeVideoPlaybackEnabled(): Flow<Boolean> = flowOf(videoPlaybackEnabled)
        override suspend fun setVideoDelaySeconds(seconds: Int) { videoDelaySeconds = seconds }
        override fun observeVideoDelaySeconds(): Flow<Int> = flowOf(videoDelaySeconds)
        override suspend fun setVideoAudioEnabled(enabled: Boolean) { videoAudioEnabled = enabled }
        override fun observeVideoAudioEnabled(): Flow<Boolean> = flowOf(videoAudioEnabled)
        override suspend fun setGamePlayingBehavior(behavior: ScreenBehavior) { gamePlayingBehavior = behavior }
        override fun observeGamePlayingBehavior(): Flow<ScreenBehavior> = flowOf(gamePlayingBehavior)
        override suspend fun setScreensaverBehavior(behavior: ScreenBehavior) { screensaverBehavior = behavior }
        override fun observeScreensaverBehavior(): Flow<ScreenBehavior> = flowOf(screensaverBehavior)
        override suspend fun setThemePreference(preference: ThemePreference) { themePreference = preference }
        override fun observeThemePreference(): Flow<ThemePreference> = flowOf(themePreference)
        override suspend fun setMusicEnabled(enabled: Boolean) { musicEnabled = enabled }
        override fun observeMusicEnabled(): Flow<Boolean> = flowOf(musicEnabled)
        override suspend fun setMusicPlayWhileBrowsingSystems(enabled: Boolean) { musicPlayWhileBrowsingSystems = enabled }
        override fun observeMusicPlayWhileBrowsingSystems(): Flow<Boolean> = flowOf(musicPlayWhileBrowsingSystems)
        override suspend fun setMusicPlayWhileBrowsingGames(enabled: Boolean) { musicPlayWhileBrowsingGames = enabled }
        override fun observeMusicPlayWhileBrowsingGames(): Flow<Boolean> = flowOf(musicPlayWhileBrowsingGames)
        override suspend fun setMusicPlayDuringScreensaver(enabled: Boolean) { musicPlayDuringScreensaver = enabled }
        override fun observeMusicPlayDuringScreensaver(): Flow<Boolean> = flowOf(musicPlayDuringScreensaver)
        override suspend fun setMusicDuckingMode(mode: MusicDuckingMode) { musicDuckingMode = mode }
        override fun observeMusicDuckingMode(): Flow<MusicDuckingMode> = flowOf(musicDuckingMode)
        override suspend fun setOverlayOpacityPercent(percent: Int) { overlayOpacityPercent = percent }
        override fun observeOverlayOpacityPercent(): Flow<Int> = flowOf(overlayOpacityPercent)
        override suspend fun saveCustomMusicFolderPath(path: String) {}
        override fun observeCustomMusicFolderPath(): Flow<String?> = flowOf(null)
        override suspend fun clearCustomMusicFolderPath() {}
        override suspend fun setCloseCompanionOnQuitEnabled(enabled: Boolean) { closeCompanionOnQuitEnabled = enabled }
        override fun observeCloseCompanionOnQuitEnabled(): Flow<Boolean> = flowOf(closeCompanionOnQuitEnabled)
        override suspend fun setSettingsFabVisible(visible: Boolean) { settingsFabVisible = visible }
        override fun observeSettingsFabVisible(): Flow<Boolean> = flowOf(settingsFabVisible)
    }

    private class FakeAppDrawerSettingsRepository(
        initialColumns: Int = 4,
        initialSortFoldersOnTop: Boolean = true,
    ) : AppDrawerSettingsRepository {
        val columns = MutableStateFlow(initialColumns)
        val sortFoldersOnTop = MutableStateFlow(initialSortFoldersOnTop)

        override suspend fun setHiddenApps(packageNames: Set<String>) {}
        override fun observeHiddenApps(): Flow<Set<String>> = flowOf(emptySet())
        override suspend fun setGridColumns(columns: Int) { this.columns.value = columns }
        override fun observeGridColumns(): Flow<Int> = columns
        override suspend fun setOtherScreenLaunchApps(packageNames: Set<String>) {}
        override fun observeOtherScreenLaunchApps(): Flow<Set<String>> = flowOf(emptySet())
        override suspend fun setSortFoldersOnTop(sortOnTop: Boolean) { sortFoldersOnTop.value = sortOnTop }
        override fun observeSortFoldersOnTop(): Flow<Boolean> = sortFoldersOnTop
    }

    private class FakeDockSettingsRepository(
        initialEnabled: Boolean = false,
        initialMaxApps: Int = 5,
        initialSize: DockSize = DockSize.Medium,
    ) : DockSettingsRepository {
        val enabled = MutableStateFlow(initialEnabled)
        val maxApps = MutableStateFlow(initialMaxApps)
        val size = MutableStateFlow(initialSize)

        override suspend fun setDockEnabled(enabled: Boolean) { this.enabled.value = enabled }
        override fun observeDockEnabled(): Flow<Boolean> = enabled
        override suspend fun setDockMaxApps(maxApps: Int) { this.maxApps.value = maxApps }
        override fun observeDockMaxApps(): Flow<Int> = maxApps
        override suspend fun setDockSize(size: DockSize) { this.size.value = size }
        override fun observeDockSize(): Flow<DockSize> = size
        override suspend fun setDockApps(packageNames: List<String>) {}
        override fun observeDockApps(): Flow<List<String>> = flowOf(emptyList())
    }

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(
        onboardingRepository: FakeOnboardingRepository = FakeOnboardingRepository(),
        appDrawerSettingsRepository: FakeAppDrawerSettingsRepository = FakeAppDrawerSettingsRepository(),
        dockSettingsRepository: FakeDockSettingsRepository = FakeDockSettingsRepository(),
    ): Pair<SettingsViewModel, FakeAppDrawerSettingsRepository> {
        val viewModel = SettingsViewModel(
            onboardingRepository = onboardingRepository,
            validateLogFolderUseCase = ValidateEsdeLogFolderUseCase(onboardingRepository),
            validateMediaFolderUseCase = ValidateEsdeMediaFolderUseCase(onboardingRepository),
            observeGamePlayingBehaviorUseCase = ObserveGamePlayingBehaviorUseCase(onboardingRepository),
            observeVideoPlaybackEnabledUseCase = ObserveVideoPlaybackEnabledUseCase(onboardingRepository),
            setVideoPlaybackEnabledUseCase = SetVideoPlaybackEnabledUseCase(onboardingRepository),
            observeVideoDelaySecondsUseCase = ObserveVideoDelaySecondsUseCase(onboardingRepository),
            setVideoDelaySecondsUseCase = SetVideoDelaySecondsUseCase(onboardingRepository),
            observeVideoAudioEnabledUseCase = ObserveVideoAudioEnabledUseCase(onboardingRepository),
            setVideoAudioEnabledUseCase = SetVideoAudioEnabledUseCase(onboardingRepository),
            setGamePlayingBehaviorUseCase = SetGamePlayingBehaviorUseCase(onboardingRepository),
            observeScreensaverBehaviorUseCase = ObserveScreensaverBehaviorUseCase(onboardingRepository),
            setScreensaverBehaviorUseCase = SetScreensaverBehaviorUseCase(onboardingRepository),
            observeThemePreferenceUseCase = ObserveThemePreferenceUseCase(onboardingRepository),
            setThemePreferenceUseCase = SetThemePreferenceUseCase(onboardingRepository),
            observeOverlayOpacityUseCase = ObserveOverlayOpacityUseCase(onboardingRepository),
            setOverlayOpacityUseCase = SetOverlayOpacityUseCase(onboardingRepository),
            observeGridColumnsUseCase = ObserveGridColumnsUseCase(appDrawerSettingsRepository),
            setGridColumnsUseCase = SetGridColumnsUseCase(appDrawerSettingsRepository),
            observeSortFoldersOnTopUseCase = ObserveSortFoldersOnTopUseCase(appDrawerSettingsRepository),
            setSortFoldersOnTopUseCase = SetSortFoldersOnTopUseCase(appDrawerSettingsRepository),
            observeDockEnabledUseCase = ObserveDockEnabledUseCase(dockSettingsRepository),
            setDockEnabledUseCase = SetDockEnabledUseCase(dockSettingsRepository),
            observeDockMaxAppsUseCase = ObserveDockMaxAppsUseCase(dockSettingsRepository),
            setDockMaxAppsUseCase = SetDockMaxAppsUseCase(dockSettingsRepository),
            observeDockSizeUseCase = ObserveDockSizeUseCase(dockSettingsRepository),
            setDockSizeUseCase = SetDockSizeUseCase(dockSettingsRepository),
            observeMusicEnabledUseCase = ObserveMusicEnabledUseCase(onboardingRepository),
            setMusicEnabledUseCase = SetMusicEnabledUseCase(onboardingRepository),
            observeMusicPlayWhileBrowsingSystemsUseCase = ObserveMusicPlayWhileBrowsingSystemsUseCase(onboardingRepository),
            setMusicPlayWhileBrowsingSystemsUseCase = SetMusicPlayWhileBrowsingSystemsUseCase(onboardingRepository),
            observeMusicPlayWhileBrowsingGamesUseCase = ObserveMusicPlayWhileBrowsingGamesUseCase(onboardingRepository),
            setMusicPlayWhileBrowsingGamesUseCase = SetMusicPlayWhileBrowsingGamesUseCase(onboardingRepository),
            observeMusicPlayDuringScreensaverUseCase = ObserveMusicPlayDuringScreensaverUseCase(onboardingRepository),
            setMusicPlayDuringScreensaverUseCase = SetMusicPlayDuringScreensaverUseCase(onboardingRepository),
            observeMusicDuckingModeUseCase = ObserveMusicDuckingModeUseCase(onboardingRepository),
            setMusicDuckingModeUseCase = SetMusicDuckingModeUseCase(onboardingRepository),
            observeCloseCompanionOnQuitEnabledUseCase = ObserveCloseCompanionOnQuitEnabledUseCase(onboardingRepository),
            setCloseCompanionOnQuitEnabledUseCase = SetCloseCompanionOnQuitEnabledUseCase(onboardingRepository),
            observeSettingsFabVisibleUseCase = ObserveSettingsFabVisibleUseCase(onboardingRepository),
            setSettingsFabVisibleUseCase = SetSettingsFabVisibleUseCase(onboardingRepository),
        )
        return viewModel to appDrawerSettingsRepository
    }

    @Test
    fun `initial state loads overlay opacity and grid columns from the repository`() = runTest(testDispatcher) {
        val (viewModel, _) = buildViewModel(
            onboardingRepository = FakeOnboardingRepository().apply { overlayOpacityPercent = 70 },
            appDrawerSettingsRepository = FakeAppDrawerSettingsRepository(initialColumns = 5),
        )

        advanceUntilIdle()

        assertEquals(70, viewModel.uiState.value.overlayOpacityPercent)
        assertEquals(5, viewModel.uiState.value.gridColumns)
    }

    @Test
    fun `onOverlayOpacityChanged updates ui state immediately and persists`() = runTest(testDispatcher) {
        val onboardingRepository = FakeOnboardingRepository()
        val (viewModel, _) = buildViewModel(onboardingRepository = onboardingRepository)
        advanceUntilIdle()

        viewModel.onOverlayOpacityChanged(85)

        // ui state updates synchronously, before the persistence coroutine runs.
        assertEquals(85, viewModel.uiState.value.overlayOpacityPercent)

        advanceUntilIdle()
        assertEquals(85, onboardingRepository.overlayOpacityPercent)
    }

    @Test
    fun `onGridColumnsChanged updates ui state immediately and persists`() = runTest(testDispatcher) {
        val (viewModel, appDrawerSettingsRepository) = buildViewModel()
        advanceUntilIdle()

        viewModel.onGridColumnsChanged(6)

        assertEquals(6, viewModel.uiState.value.gridColumns)

        advanceUntilIdle()
        assertEquals(6, appDrawerSettingsRepository.columns.value)
    }

    @Test
    fun `onSortFoldersOnTopChanged updates ui state immediately and persists`() = runTest(testDispatcher) {
        val (viewModel, appDrawerSettingsRepository) = buildViewModel(
            appDrawerSettingsRepository = FakeAppDrawerSettingsRepository(initialSortFoldersOnTop = true),
        )
        advanceUntilIdle()

        viewModel.onSortFoldersOnTopChanged(false)

        assertEquals(false, viewModel.uiState.value.sortFoldersOnTop)

        advanceUntilIdle()
        assertEquals(false, appDrawerSettingsRepository.sortFoldersOnTop.value)
    }
}
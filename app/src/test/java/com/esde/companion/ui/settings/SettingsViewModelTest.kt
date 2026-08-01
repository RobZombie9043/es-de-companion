package com.esde.companion.ui.settings

import com.esde.companion.domain.model.LogFolderValidation
import com.esde.companion.domain.model.MediaFolderValidation
import com.esde.companion.domain.model.MusicDuckingMode
import com.esde.companion.domain.model.PlacedWidget
import com.esde.companion.domain.model.ScreenBehavior
import com.esde.companion.domain.model.StateGroup
import com.esde.companion.domain.model.ThemePreference
import com.esde.companion.domain.model.VideoAspectRatioMode
import com.esde.companion.domain.repository.AppDrawerSettingsRepository
import com.esde.companion.domain.repository.OnboardingRepository
import com.esde.companion.domain.repository.WidgetLayoutRepository
import com.esde.companion.domain.usecase.ObserveDrawerOpacityUseCase
import com.esde.companion.domain.usecase.ObserveGamePlayingBehaviorUseCase
import com.esde.companion.domain.usecase.ObserveGridColumnsUseCase
import com.esde.companion.domain.usecase.ObserveMusicDuckingModeUseCase
import com.esde.companion.domain.usecase.ObserveMusicEnabledUseCase
import com.esde.companion.domain.usecase.ObserveMusicPlayDuringScreensaverUseCase
import com.esde.companion.domain.usecase.ObserveMusicPlayWhileBrowsingGamesUseCase
import com.esde.companion.domain.usecase.ObserveMusicPlayWhileBrowsingSystemsUseCase
import com.esde.companion.domain.usecase.ObserveOverlayEnabledUseCase
import com.esde.companion.domain.usecase.ObserveScreensaverBehaviorUseCase
import com.esde.companion.domain.usecase.ObserveThemePreferenceUseCase
import com.esde.companion.domain.usecase.ObserveVideoAspectRatioModeUseCase
import com.esde.companion.domain.usecase.ObserveVideoAudioEnabledUseCase
import com.esde.companion.domain.usecase.ObserveVideoDelaySecondsUseCase
import com.esde.companion.domain.usecase.ObserveVideoPlaybackEnabledUseCase
import com.esde.companion.domain.usecase.ObserveWidgetsLockedUseCase
import com.esde.companion.domain.usecase.SetDrawerOpacityUseCase
import com.esde.companion.domain.usecase.SetGamePlayingBehaviorUseCase
import com.esde.companion.domain.usecase.SetGridColumnsUseCase
import com.esde.companion.domain.usecase.SetMusicDuckingModeUseCase
import com.esde.companion.domain.usecase.SetMusicEnabledUseCase
import com.esde.companion.domain.usecase.SetMusicPlayDuringScreensaverUseCase
import com.esde.companion.domain.usecase.SetMusicPlayWhileBrowsingGamesUseCase
import com.esde.companion.domain.usecase.SetMusicPlayWhileBrowsingSystemsUseCase
import com.esde.companion.domain.usecase.SetOverlayEnabledUseCase
import com.esde.companion.domain.usecase.SetScreensaverBehaviorUseCase
import com.esde.companion.domain.usecase.SetThemePreferenceUseCase
import com.esde.companion.domain.usecase.SetVideoAspectRatioModeUseCase
import com.esde.companion.domain.usecase.SetVideoAudioEnabledUseCase
import com.esde.companion.domain.usecase.SetVideoDelaySecondsUseCase
import com.esde.companion.domain.usecase.SetVideoPlaybackEnabledUseCase
import com.esde.companion.domain.usecase.SetWidgetsLockedUseCase
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
        var overlayEnabled = true
        var gamePlayingBehavior = ScreenBehavior.Nothing
        var videoPlaybackEnabled = false
        var videoDelaySeconds = 0
        var videoAudioEnabled = true
        var videoAspectRatioMode = VideoAspectRatioMode.Cover
        var screensaverBehavior = ScreenBehavior.Nothing
        var themePreference = ThemePreference.Auto
        var musicEnabled = true
        var musicPlayWhileBrowsingSystems = true
        var musicPlayWhileBrowsingGames = true
        var musicPlayDuringScreensaver = true
        var musicDuckingMode = MusicDuckingMode.LowerVolume

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
        override suspend fun setOverlayEnabled(enabled: Boolean) { overlayEnabled = enabled }
        override fun observeOverlayEnabled(): Flow<Boolean> = flowOf(overlayEnabled)
        override suspend fun setVideoPlaybackEnabled(enabled: Boolean) { videoPlaybackEnabled = enabled }
        override fun observeVideoPlaybackEnabled(): Flow<Boolean> = flowOf(videoPlaybackEnabled)
        override suspend fun setVideoDelaySeconds(seconds: Int) { videoDelaySeconds = seconds }
        override fun observeVideoDelaySeconds(): Flow<Int> = flowOf(videoDelaySeconds)
        override suspend fun setVideoAudioEnabled(enabled: Boolean) { videoAudioEnabled = enabled }
        override fun observeVideoAudioEnabled(): Flow<Boolean> = flowOf(videoAudioEnabled)
        override suspend fun setVideoAspectRatioMode(mode: VideoAspectRatioMode) { videoAspectRatioMode = mode }
        override fun observeVideoAspectRatioMode(): Flow<VideoAspectRatioMode> = flowOf(videoAspectRatioMode)
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
        override suspend fun saveCustomMusicFolderPath(path: String) {}
        override fun observeCustomMusicFolderPath(): Flow<String?> = flowOf(null)
        override suspend fun clearCustomMusicFolderPath() {}
    }

    private class FakeAppDrawerSettingsRepository(
        initialOpacity: Int = 80,
        initialColumns: Int = 4,
    ) : AppDrawerSettingsRepository {
        val opacity = MutableStateFlow(initialOpacity)
        val columns = MutableStateFlow(initialColumns)

        override suspend fun setHiddenApps(packageNames: Set<String>) {}
        override fun observeHiddenApps(): Flow<Set<String>> = flowOf(emptySet())
        override suspend fun setDrawerOpacityPercent(percent: Int) { opacity.value = percent }
        override fun observeDrawerOpacityPercent(): Flow<Int> = opacity
        override suspend fun setGridColumns(columns: Int) { this.columns.value = columns }
        override fun observeGridColumns(): Flow<Int> = columns
        override suspend fun setOtherScreenLaunchApps(packageNames: Set<String>) {}
        override fun observeOtherScreenLaunchApps(): Flow<Set<String>> = flowOf(emptySet())
    }

    private class FakeWidgetLayoutRepository(
        initialLocked: Boolean = false,
    ) : WidgetLayoutRepository {
        val locked = MutableStateFlow(initialLocked)

        override fun observeCanvas(stateGroup: StateGroup): Flow<List<PlacedWidget>> = flowOf(emptyList())
        override suspend fun saveCanvas(stateGroup: StateGroup, widgets: List<PlacedWidget>) { /* not under test */ }
        override fun observeWidgetsLocked(): Flow<Boolean> = locked
        override suspend fun setWidgetsLocked(locked: Boolean) { this.locked.value = locked }
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
        widgetLayoutRepository: FakeWidgetLayoutRepository = FakeWidgetLayoutRepository(),
    ): Pair<SettingsViewModel, FakeAppDrawerSettingsRepository> {
        val viewModel = SettingsViewModel(
            onboardingRepository = onboardingRepository,
            validateLogFolderUseCase = ValidateEsdeLogFolderUseCase(onboardingRepository),
            validateMediaFolderUseCase = ValidateEsdeMediaFolderUseCase(onboardingRepository),
            observeOverlayEnabledUseCase = ObserveOverlayEnabledUseCase(onboardingRepository),
            setOverlayEnabledUseCase = SetOverlayEnabledUseCase(onboardingRepository),
            observeGamePlayingBehaviorUseCase = ObserveGamePlayingBehaviorUseCase(onboardingRepository),
            observeVideoPlaybackEnabledUseCase = ObserveVideoPlaybackEnabledUseCase(onboardingRepository),
            setVideoPlaybackEnabledUseCase = SetVideoPlaybackEnabledUseCase(onboardingRepository),
            observeVideoDelaySecondsUseCase = ObserveVideoDelaySecondsUseCase(onboardingRepository),
            setVideoDelaySecondsUseCase = SetVideoDelaySecondsUseCase(onboardingRepository),
            observeVideoAudioEnabledUseCase = ObserveVideoAudioEnabledUseCase(onboardingRepository),
            setVideoAudioEnabledUseCase = SetVideoAudioEnabledUseCase(onboardingRepository),
            observeVideoAspectRatioModeUseCase = ObserveVideoAspectRatioModeUseCase(onboardingRepository),
            setVideoAspectRatioModeUseCase = SetVideoAspectRatioModeUseCase(onboardingRepository),
            setGamePlayingBehaviorUseCase = SetGamePlayingBehaviorUseCase(onboardingRepository),
            observeScreensaverBehaviorUseCase = ObserveScreensaverBehaviorUseCase(onboardingRepository),
            setScreensaverBehaviorUseCase = SetScreensaverBehaviorUseCase(onboardingRepository),
            observeThemePreferenceUseCase = ObserveThemePreferenceUseCase(onboardingRepository),
            setThemePreferenceUseCase = SetThemePreferenceUseCase(onboardingRepository),
            observeDrawerOpacityUseCase = ObserveDrawerOpacityUseCase(appDrawerSettingsRepository),
            setDrawerOpacityUseCase = SetDrawerOpacityUseCase(appDrawerSettingsRepository),
            observeGridColumnsUseCase = ObserveGridColumnsUseCase(appDrawerSettingsRepository),
            setGridColumnsUseCase = SetGridColumnsUseCase(appDrawerSettingsRepository),
            observeWidgetsLockedUseCase = ObserveWidgetsLockedUseCase(widgetLayoutRepository),
            setWidgetsLockedUseCase = SetWidgetsLockedUseCase(widgetLayoutRepository),
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
        )
        return viewModel to appDrawerSettingsRepository
    }

    @Test
    fun `initial state loads drawer opacity and grid columns from the repository`() = runTest(testDispatcher) {
        val (viewModel, _) = buildViewModel(
            appDrawerSettingsRepository = FakeAppDrawerSettingsRepository(initialOpacity = 70, initialColumns = 5),
        )

        advanceUntilIdle()

        assertEquals(70, viewModel.uiState.value.drawerOpacityPercent)
        assertEquals(5, viewModel.uiState.value.gridColumns)
    }

    @Test
    fun `onDrawerOpacityChanged updates ui state immediately and persists`() = runTest(testDispatcher) {
        val (viewModel, appDrawerSettingsRepository) = buildViewModel()
        advanceUntilIdle()

        viewModel.onDrawerOpacityChanged(85)

        // ui state updates synchronously, before the persistence coroutine runs.
        assertEquals(85, viewModel.uiState.value.drawerOpacityPercent)

        advanceUntilIdle()
        assertEquals(85, appDrawerSettingsRepository.opacity.value)
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
    fun `onWidgetsLockedChanged updates ui state immediately and persists`() = runTest(testDispatcher) {
        val widgetLayoutRepository = FakeWidgetLayoutRepository()
        val (viewModel, _) = buildViewModel(widgetLayoutRepository = widgetLayoutRepository)
        advanceUntilIdle()

        viewModel.onWidgetsLockedChanged(true)

        assertEquals(true, viewModel.uiState.value.widgetsLocked)

        advanceUntilIdle()
        assertEquals(true, widgetLayoutRepository.locked.value)
    }
}
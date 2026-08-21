package com.esde.companion.ui.settings

import com.esde.companion.data.backup.JsonConfigBackupRepository
import com.esde.companion.domain.model.DockSize
import com.esde.companion.domain.model.FabAssignments
import com.esde.companion.domain.model.FabPosition
import com.esde.companion.domain.model.FabSlot
import com.esde.companion.domain.model.FabType
import com.esde.companion.domain.model.InstalledApp
import com.esde.companion.domain.model.LogFolderValidation
import com.esde.companion.domain.model.MediaFolderValidation
import com.esde.companion.domain.model.MusicDuckingMode
import com.esde.companion.domain.model.ScreenBehavior
import com.esde.companion.domain.model.ThemePreference
import com.esde.companion.domain.repository.AppDrawerSettingsRepository
import com.esde.companion.domain.repository.BackupRepositories
import com.esde.companion.domain.repository.DockSettingsRepository
import com.esde.companion.domain.repository.InstalledAppsRepository
import com.esde.companion.domain.repository.OnboardingRepository
import com.esde.companion.domain.usecase.ExportConfigBackupUseCase
import com.esde.companion.domain.usecase.FakeAppFolderRepository
import com.esde.companion.domain.usecase.FakeThorSettingsRepository
import com.esde.companion.domain.usecase.FakeWidgetLayoutRepository
import com.esde.companion.domain.usecase.ObserveAutoFpsEnabledUseCase
import com.esde.companion.domain.usecase.ObserveCloseCompanionOnQuitEnabledUseCase
import com.esde.companion.domain.usecase.ObserveDebugLoggingEnabledUseCase
import com.esde.companion.domain.usecase.ObserveDockEnabledUseCase
import com.esde.companion.domain.usecase.ObserveDockMaxAppsUseCase
import com.esde.companion.domain.usecase.ObserveDockSizeUseCase
import com.esde.companion.domain.usecase.ObserveFabAssignmentsUseCase
import com.esde.companion.domain.usecase.ObserveGamePlayingBehaviorUseCase
import com.esde.companion.domain.usecase.ObserveGamePlayingDimPercentUseCase
import com.esde.companion.domain.usecase.ObserveGridColumnsUseCase
import com.esde.companion.domain.usecase.ObserveHallSensorCalibrationUseCase
import com.esde.companion.domain.usecase.ObserveInstalledAppsUseCase
import com.esde.companion.domain.usecase.ObserveLaunchEsdeOnStartEnabledUseCase
import com.esde.companion.domain.usecase.ObserveLidWakeGuardEnabledUseCase
import com.esde.companion.domain.usecase.ObserveMusicDuckingModeUseCase
import com.esde.companion.domain.usecase.ObserveMusicEnabledUseCase
import com.esde.companion.domain.usecase.ObserveMusicPlayDuringScreensaverUseCase
import com.esde.companion.domain.usecase.ObserveMusicPlayWhileBrowsingGamesUseCase
import com.esde.companion.domain.usecase.ObserveMusicPlayWhileBrowsingSystemsUseCase
import com.esde.companion.domain.usecase.ObserveOverlayOpacityUseCase
import com.esde.companion.domain.usecase.ObserveScreensaverBehaviorUseCase
import com.esde.companion.domain.usecase.ObserveScreensaverDimPercentUseCase
import com.esde.companion.domain.usecase.ObserveShowSearchBarUseCase
import com.esde.companion.domain.usecase.ObserveSortFoldersOnTopUseCase
import com.esde.companion.domain.usecase.ObserveTaskKillerEnabledUseCase
import com.esde.companion.domain.usecase.ObserveThemePreferenceUseCase
import com.esde.companion.domain.usecase.ObserveVideoAudioEnabledUseCase
import com.esde.companion.domain.usecase.ObserveVideoDelaySecondsUseCase
import com.esde.companion.domain.usecase.ObserveVideoPlaybackEnabledUseCase
import com.esde.companion.domain.usecase.ObserveVolumeSyncEnabledUseCase
import com.esde.companion.domain.usecase.ObserveVolumeSyncModeUseCase
import com.esde.companion.domain.usecase.RestoreConfigBackupUseCase
import com.esde.companion.domain.usecase.SetAutoFpsEnabledUseCase
import com.esde.companion.domain.usecase.SetCloseCompanionOnQuitEnabledUseCase
import com.esde.companion.domain.usecase.SetDebugLoggingEnabledUseCase
import com.esde.companion.domain.usecase.SetDockEnabledUseCase
import com.esde.companion.domain.usecase.SetDockMaxAppsUseCase
import com.esde.companion.domain.usecase.SetDockSizeUseCase
import com.esde.companion.domain.usecase.SetFabAssignmentUseCase
import com.esde.companion.domain.usecase.SetGamePlayingBehaviorUseCase
import com.esde.companion.domain.usecase.SetGamePlayingDimPercentUseCase
import com.esde.companion.domain.usecase.SetGridColumnsUseCase
import com.esde.companion.domain.usecase.SetHallSensorCalibrationUseCase
import com.esde.companion.domain.usecase.SetLaunchEsdeOnStartEnabledUseCase
import com.esde.companion.domain.usecase.SetLidWakeGuardEnabledUseCase
import com.esde.companion.domain.usecase.SetMusicDuckingModeUseCase
import com.esde.companion.domain.usecase.SetMusicEnabledUseCase
import com.esde.companion.domain.usecase.SetMusicPlayDuringScreensaverUseCase
import com.esde.companion.domain.usecase.SetMusicPlayWhileBrowsingGamesUseCase
import com.esde.companion.domain.usecase.SetMusicPlayWhileBrowsingSystemsUseCase
import com.esde.companion.domain.usecase.SetOverlayOpacityUseCase
import com.esde.companion.domain.usecase.SetScreensaverBehaviorUseCase
import com.esde.companion.domain.usecase.SetScreensaverDimPercentUseCase
import com.esde.companion.domain.usecase.SetShowSearchBarUseCase
import com.esde.companion.domain.usecase.SetSortFoldersOnTopUseCase
import com.esde.companion.domain.usecase.SetTaskKillerEnabledUseCase
import com.esde.companion.domain.usecase.SetThemePreferenceUseCase
import com.esde.companion.domain.usecase.SetVideoAudioEnabledUseCase
import com.esde.companion.domain.usecase.SetVideoDelaySecondsUseCase
import com.esde.companion.domain.usecase.SetVideoPlaybackEnabledUseCase
import com.esde.companion.domain.usecase.SetVolumeSyncEnabledUseCase
import com.esde.companion.domain.usecase.SetVolumeSyncModeUseCase
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
        var gamePlayingDimPercent = 50
        var videoPlaybackEnabled = false
        var videoDelaySeconds = 0
        var videoAudioEnabled = true
        var screensaverBehavior = ScreenBehavior.Nothing
        var screensaverDimPercent = 50
        var themePreference = ThemePreference.Auto
        var musicEnabled = true
        var musicPlayWhileBrowsingSystems = true
        var musicPlayWhileBrowsingGames = true
        var musicPlayDuringScreensaver = true
        var musicDuckingMode = MusicDuckingMode.LowerVolume
        var overlayOpacityPercent = 80
        var closeCompanionOnQuitEnabled = false
        var fabAssignments = FabAssignments.Default
        var launchEsdeOnStartEnabled = false
        var debugLoggingEnabled = false

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

        override suspend fun setVideoPlaybackEnabled(enabled: Boolean) {
            videoPlaybackEnabled = enabled
        }

        override fun observeVideoPlaybackEnabled(): Flow<Boolean> = flowOf(videoPlaybackEnabled)

        override suspend fun setVideoDelaySeconds(seconds: Int) {
            videoDelaySeconds = seconds
        }

        override fun observeVideoDelaySeconds(): Flow<Int> = flowOf(videoDelaySeconds)

        override suspend fun setVideoAudioEnabled(enabled: Boolean) {
            videoAudioEnabled = enabled
        }

        override fun observeVideoAudioEnabled(): Flow<Boolean> = flowOf(videoAudioEnabled)

        override suspend fun setGamePlayingBehavior(behavior: ScreenBehavior) {
            gamePlayingBehavior = behavior
        }

        override fun observeGamePlayingBehavior(): Flow<ScreenBehavior> = flowOf(gamePlayingBehavior)

        override suspend fun setGamePlayingDimPercent(percent: Int) {
            gamePlayingDimPercent = percent
        }

        override fun observeGamePlayingDimPercent(): Flow<Int> = flowOf(gamePlayingDimPercent)

        override suspend fun setScreensaverBehavior(behavior: ScreenBehavior) {
            screensaverBehavior = behavior
        }

        override fun observeScreensaverBehavior(): Flow<ScreenBehavior> = flowOf(screensaverBehavior)

        override suspend fun setScreensaverDimPercent(percent: Int) {
            screensaverDimPercent = percent
        }

        override fun observeScreensaverDimPercent(): Flow<Int> = flowOf(screensaverDimPercent)

        override suspend fun setThemePreference(preference: ThemePreference) {
            themePreference = preference
        }

        override fun observeThemePreference(): Flow<ThemePreference> = flowOf(themePreference)

        override suspend fun setMusicEnabled(enabled: Boolean) {
            musicEnabled = enabled
        }

        override fun observeMusicEnabled(): Flow<Boolean> = flowOf(musicEnabled)

        override suspend fun setMusicPlayWhileBrowsingSystems(enabled: Boolean) {
            musicPlayWhileBrowsingSystems = enabled
        }

        override fun observeMusicPlayWhileBrowsingSystems(): Flow<Boolean> = flowOf(musicPlayWhileBrowsingSystems)

        override suspend fun setMusicPlayWhileBrowsingGames(enabled: Boolean) {
            musicPlayWhileBrowsingGames = enabled
        }

        override fun observeMusicPlayWhileBrowsingGames(): Flow<Boolean> = flowOf(musicPlayWhileBrowsingGames)

        override suspend fun setMusicPlayDuringScreensaver(enabled: Boolean) {
            musicPlayDuringScreensaver = enabled
        }

        override fun observeMusicPlayDuringScreensaver(): Flow<Boolean> = flowOf(musicPlayDuringScreensaver)

        override suspend fun setMusicDuckingMode(mode: MusicDuckingMode) {
            musicDuckingMode = mode
        }

        override fun observeMusicDuckingMode(): Flow<MusicDuckingMode> = flowOf(musicDuckingMode)

        override suspend fun setOverlayOpacityPercent(percent: Int) {
            overlayOpacityPercent = percent
        }

        override fun observeOverlayOpacityPercent(): Flow<Int> = flowOf(overlayOpacityPercent)

        override suspend fun saveCustomMusicFolderPath(path: String) {}

        override fun observeCustomMusicFolderPath(): Flow<String?> = flowOf(null)

        override suspend fun clearCustomMusicFolderPath() {}

        override suspend fun setCloseCompanionOnQuitEnabled(enabled: Boolean) {
            closeCompanionOnQuitEnabled = enabled
        }

        override fun observeCloseCompanionOnQuitEnabled(): Flow<Boolean> = flowOf(closeCompanionOnQuitEnabled)

        override suspend fun setFabAssignments(assignments: FabAssignments) {
            fabAssignments = assignments
        }

        override fun observeFabAssignments(): Flow<FabAssignments> = flowOf(fabAssignments)

        override suspend fun setLaunchEsdeOnStartEnabled(enabled: Boolean) {
            launchEsdeOnStartEnabled = enabled
        }

        override fun observeLaunchEsdeOnStartEnabled(): Flow<Boolean> = flowOf(launchEsdeOnStartEnabled)

        override suspend fun setDebugLoggingEnabled(enabled: Boolean) {
            debugLoggingEnabled = enabled
        }

        override fun observeDebugLoggingEnabled(): Flow<Boolean> = flowOf(debugLoggingEnabled)
    }

    private class FakeAppDrawerSettingsRepository(
        initialColumns: Int = 4,
        initialSortFoldersOnTop: Boolean = true,
        initialShowSearchBar: Boolean = true,
    ) : AppDrawerSettingsRepository {
        val columns = MutableStateFlow(initialColumns)
        val sortFoldersOnTop = MutableStateFlow(initialSortFoldersOnTop)
        val showSearchBar = MutableStateFlow(initialShowSearchBar)

        override suspend fun setHiddenApps(packageNames: Set<String>) {}

        override fun observeHiddenApps(): Flow<Set<String>> = flowOf(emptySet())

        override suspend fun setGridColumns(columns: Int) {
            this.columns.value = columns
        }

        override fun observeGridColumns(): Flow<Int> = columns

        override suspend fun setOtherScreenLaunchApps(packageNames: Set<String>) {}

        override fun observeOtherScreenLaunchApps(): Flow<Set<String>> = flowOf(emptySet())

        override suspend fun setSortFoldersOnTop(sortOnTop: Boolean) {
            sortFoldersOnTop.value = sortOnTop
        }

        override fun observeSortFoldersOnTop(): Flow<Boolean> = sortFoldersOnTop

        override suspend fun setShowSearchBar(show: Boolean) {
            showSearchBar.value = show
        }

        override fun observeShowSearchBar(): Flow<Boolean> = showSearchBar
    }

    private class FakeDockSettingsRepository(
        initialEnabled: Boolean = false,
        initialMaxApps: Int = 5,
        initialSize: DockSize = DockSize.Medium,
    ) : DockSettingsRepository {
        val enabled = MutableStateFlow(initialEnabled)
        val maxApps = MutableStateFlow(initialMaxApps)
        val size = MutableStateFlow(initialSize)

        override suspend fun setDockEnabled(enabled: Boolean) {
            this.enabled.value = enabled
        }

        override fun observeDockEnabled(): Flow<Boolean> = enabled

        override suspend fun setDockMaxApps(maxApps: Int) {
            this.maxApps.value = maxApps
        }

        override fun observeDockMaxApps(): Flow<Int> = maxApps

        override suspend fun setDockSize(size: DockSize) {
            this.size.value = size
        }

        override fun observeDockSize(): Flow<DockSize> = size

        override suspend fun setDockApps(packageNames: List<String>) {}

        override fun observeDockApps(): Flow<List<String>> = flowOf(emptyList())
    }

    private class FakeInstalledAppsRepository(
        private val apps: List<InstalledApp> = emptyList(),
    ) : InstalledAppsRepository {
        override fun observeInstalledApps(): Flow<List<InstalledApp>> = flowOf(apps)
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

    /** [buildViewModel]'s two config-backup use cases, split into their own function purely
     * to stay under detekt's LongMethod threshold once every other use case it wires joins
     * them in one place. */
    private fun configBackupUseCases(
        onboardingRepository: FakeOnboardingRepository,
        appDrawerSettingsRepository: FakeAppDrawerSettingsRepository,
        dockSettingsRepository: FakeDockSettingsRepository,
        thorSettingsRepository: FakeThorSettingsRepository,
    ): Pair<ExportConfigBackupUseCase, RestoreConfigBackupUseCase> {
        val configBackupRepository = JsonConfigBackupRepository()
        val repositories =
            BackupRepositories(
                onboardingRepository,
                appDrawerSettingsRepository,
                FakeAppFolderRepository(),
                dockSettingsRepository,
                FakeWidgetLayoutRepository(),
                thorSettingsRepository,
            )
        val export = ExportConfigBackupUseCase(repositories, configBackupRepository)
        val restore = RestoreConfigBackupUseCase(repositories, configBackupRepository)
        return export to restore
    }

    private fun buildViewModel(
        onboardingRepository: FakeOnboardingRepository = FakeOnboardingRepository(),
        appDrawerSettingsRepository: FakeAppDrawerSettingsRepository = FakeAppDrawerSettingsRepository(),
        dockSettingsRepository: FakeDockSettingsRepository = FakeDockSettingsRepository(),
        installedAppsRepository: FakeInstalledAppsRepository = FakeInstalledAppsRepository(),
        thorSettingsRepository: FakeThorSettingsRepository = FakeThorSettingsRepository(),
    ): Pair<SettingsViewModel, FakeAppDrawerSettingsRepository> {
        val (exportConfigBackupUseCase, restoreConfigBackupUseCase) =
            configBackupUseCases(
                onboardingRepository,
                appDrawerSettingsRepository,
                dockSettingsRepository,
                thorSettingsRepository,
            )
        val viewModel =
            SettingsViewModel(
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
                observeGamePlayingDimPercentUseCase = ObserveGamePlayingDimPercentUseCase(onboardingRepository),
                setGamePlayingDimPercentUseCase = SetGamePlayingDimPercentUseCase(onboardingRepository),
                observeScreensaverBehaviorUseCase = ObserveScreensaverBehaviorUseCase(onboardingRepository),
                setScreensaverBehaviorUseCase = SetScreensaverBehaviorUseCase(onboardingRepository),
                observeScreensaverDimPercentUseCase = ObserveScreensaverDimPercentUseCase(onboardingRepository),
                setScreensaverDimPercentUseCase = SetScreensaverDimPercentUseCase(onboardingRepository),
                observeThemePreferenceUseCase = ObserveThemePreferenceUseCase(onboardingRepository),
                setThemePreferenceUseCase = SetThemePreferenceUseCase(onboardingRepository),
                observeOverlayOpacityUseCase = ObserveOverlayOpacityUseCase(onboardingRepository),
                setOverlayOpacityUseCase = SetOverlayOpacityUseCase(onboardingRepository),
                observeGridColumnsUseCase = ObserveGridColumnsUseCase(appDrawerSettingsRepository),
                setGridColumnsUseCase = SetGridColumnsUseCase(appDrawerSettingsRepository),
                observeSortFoldersOnTopUseCase = ObserveSortFoldersOnTopUseCase(appDrawerSettingsRepository),
                setSortFoldersOnTopUseCase = SetSortFoldersOnTopUseCase(appDrawerSettingsRepository),
                observeShowSearchBarUseCase = ObserveShowSearchBarUseCase(appDrawerSettingsRepository),
                setShowSearchBarUseCase = SetShowSearchBarUseCase(appDrawerSettingsRepository),
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
                observeFabAssignmentsUseCase = ObserveFabAssignmentsUseCase(onboardingRepository),
                setFabAssignmentUseCase = SetFabAssignmentUseCase(onboardingRepository),
                observeInstalledAppsUseCase = ObserveInstalledAppsUseCase(installedAppsRepository),
                observeLaunchEsdeOnStartEnabledUseCase = ObserveLaunchEsdeOnStartEnabledUseCase(onboardingRepository),
                setLaunchEsdeOnStartEnabledUseCase = SetLaunchEsdeOnStartEnabledUseCase(onboardingRepository),
                observeDebugLoggingEnabledUseCase = ObserveDebugLoggingEnabledUseCase(onboardingRepository),
                setDebugLoggingEnabledUseCase = SetDebugLoggingEnabledUseCase(onboardingRepository),
                exportConfigBackupUseCase = exportConfigBackupUseCase,
                restoreConfigBackupUseCase = restoreConfigBackupUseCase,
                observeLidWakeGuardEnabledUseCase = ObserveLidWakeGuardEnabledUseCase(thorSettingsRepository),
                setLidWakeGuardEnabledUseCase = SetLidWakeGuardEnabledUseCase(thorSettingsRepository),
                observeHallSensorCalibrationUseCase = ObserveHallSensorCalibrationUseCase(thorSettingsRepository),
                setHallSensorCalibrationUseCase = SetHallSensorCalibrationUseCase(thorSettingsRepository),
                observeAutoFpsEnabledUseCase = ObserveAutoFpsEnabledUseCase(thorSettingsRepository),
                setAutoFpsEnabledUseCase = SetAutoFpsEnabledUseCase(thorSettingsRepository),
                observeTaskKillerEnabledUseCase = ObserveTaskKillerEnabledUseCase(thorSettingsRepository),
                setTaskKillerEnabledUseCase = SetTaskKillerEnabledUseCase(thorSettingsRepository),
                observeVolumeSyncEnabledUseCase = ObserveVolumeSyncEnabledUseCase(thorSettingsRepository),
                setVolumeSyncEnabledUseCase = SetVolumeSyncEnabledUseCase(thorSettingsRepository),
                observeVolumeSyncModeUseCase = ObserveVolumeSyncModeUseCase(thorSettingsRepository),
                setVolumeSyncModeUseCase = SetVolumeSyncModeUseCase(thorSettingsRepository),
                volumeSyncSecondarySettingPresent = false,
            )
        return viewModel to appDrawerSettingsRepository
    }

    @Test
    fun `initial state loads overlay opacity and grid columns from the repository`() =
        runTest(testDispatcher) {
            val (viewModel, _) =
                buildViewModel(
                    onboardingRepository = FakeOnboardingRepository().apply { overlayOpacityPercent = 70 },
                    appDrawerSettingsRepository = FakeAppDrawerSettingsRepository(initialColumns = 5),
                )

            advanceUntilIdle()

            assertEquals(70, viewModel.uiState.value.overlayOpacityPercent)
            assertEquals(5, viewModel.uiState.value.gridColumns)
        }

    @Test
    fun `onOverlayOpacityChanged updates ui state immediately and persists`() =
        runTest(testDispatcher) {
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
    fun `onGamePlayingDimPercentChanged and onScreensaverDimPercentChanged update ui state immediately and persist`() =
        runTest(testDispatcher) {
            val onboardingRepository = FakeOnboardingRepository()
            val (viewModel, _) = buildViewModel(onboardingRepository = onboardingRepository)
            advanceUntilIdle()

            viewModel.onGamePlayingDimPercentChanged(65)
            viewModel.onScreensaverDimPercentChanged(30)

            assertEquals(65, viewModel.uiState.value.gamePlayingDimPercent)
            assertEquals(30, viewModel.uiState.value.screensaverDimPercent)

            advanceUntilIdle()
            assertEquals(65, onboardingRepository.gamePlayingDimPercent)
            assertEquals(30, onboardingRepository.screensaverDimPercent)
        }

    @Test
    fun `onLaunchEsdeOnStartEnabledChanged updates ui state immediately and persists`() =
        runTest(testDispatcher) {
            val onboardingRepository = FakeOnboardingRepository()
            val (viewModel, _) = buildViewModel(onboardingRepository = onboardingRepository)
            advanceUntilIdle()

            viewModel.onLaunchEsdeOnStartEnabledChanged(true)

            // ui state updates synchronously, before the persistence coroutine runs.
            assertEquals(true, viewModel.uiState.value.launchEsdeOnStartEnabled)

            advanceUntilIdle()
            assertEquals(true, onboardingRepository.launchEsdeOnStartEnabled)
        }

    @Test
    fun `onFabTypeChanged persists the swapped result and reflects it in ui state`() =
        runTest(testDispatcher) {
            // Defaults: topStart = Music, topEnd = Settings - assigning Music to topEnd
            // should swap topStart to Settings rather than just overwriting topEnd.
            val onboardingRepository = FakeOnboardingRepository()
            val (viewModel, _) = buildViewModel(onboardingRepository = onboardingRepository)
            advanceUntilIdle()

            viewModel.onFabTypeChanged(FabPosition.TopEnd, FabType.Music)
            advanceUntilIdle()

            val expected =
                FabAssignments.Default.copy(topStart = FabSlot(FabType.Settings), topEnd = FabSlot(FabType.Music))
            assertEquals(expected, viewModel.uiState.value.fabAssignments)
            assertEquals(expected, onboardingRepository.fabAssignments)
        }

    @Test
    fun `onFabCustomAppChanged sets CustomApp type and package for that corner`() =
        runTest(testDispatcher) {
            val onboardingRepository = FakeOnboardingRepository()
            val (viewModel, _) = buildViewModel(onboardingRepository = onboardingRepository)
            advanceUntilIdle()

            viewModel.onFabCustomAppChanged(FabPosition.BottomStart, "com.example.app")
            advanceUntilIdle()

            val expected = FabAssignments.Default.copy(bottomStart = FabSlot(FabType.CustomApp, "com.example.app"))
            assertEquals(expected, viewModel.uiState.value.fabAssignments)
            assertEquals(expected, onboardingRepository.fabAssignments)
        }

    @Test
    fun `onGridColumnsChanged updates ui state immediately and persists`() =
        runTest(testDispatcher) {
            val (viewModel, appDrawerSettingsRepository) = buildViewModel()
            advanceUntilIdle()

            viewModel.onGridColumnsChanged(6)

            assertEquals(6, viewModel.uiState.value.gridColumns)

            advanceUntilIdle()
            assertEquals(6, appDrawerSettingsRepository.columns.value)
        }

    @Test
    fun `onSortFoldersOnTopChanged updates ui state immediately and persists`() =
        runTest(testDispatcher) {
            val (viewModel, appDrawerSettingsRepository) =
                buildViewModel(
                    appDrawerSettingsRepository = FakeAppDrawerSettingsRepository(initialSortFoldersOnTop = true),
                )
            advanceUntilIdle()

            viewModel.onSortFoldersOnTopChanged(false)

            assertEquals(false, viewModel.uiState.value.sortFoldersOnTop)

            advanceUntilIdle()
            assertEquals(false, appDrawerSettingsRepository.sortFoldersOnTop.value)
        }

    @Test
    fun `onShowSearchBarChanged updates ui state immediately and persists`() =
        runTest(testDispatcher) {
            val (viewModel, appDrawerSettingsRepository) =
                buildViewModel(
                    appDrawerSettingsRepository = FakeAppDrawerSettingsRepository(initialShowSearchBar = true),
                )
            advanceUntilIdle()

            viewModel.onShowSearchBarChanged(false)

            assertEquals(false, viewModel.uiState.value.showSearchBar)

            advanceUntilIdle()
            assertEquals(false, appDrawerSettingsRepository.showSearchBar.value)
        }

    @Test
    fun `showSearchBar loads the persisted value into ui state`() =
        runTest(testDispatcher) {
            val (viewModel, _) =
                buildViewModel(
                    appDrawerSettingsRepository = FakeAppDrawerSettingsRepository(initialShowSearchBar = false),
                )
            advanceUntilIdle()

            assertEquals(false, viewModel.uiState.value.showSearchBar)
        }
}

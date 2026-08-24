package com.esde.companion.ui.dock

import com.esde.companion.domain.model.AppDrawerShortcut
import com.esde.companion.domain.model.DockSize
import com.esde.companion.domain.model.FabAssignments
import com.esde.companion.domain.model.InstalledApp
import com.esde.companion.domain.model.LaunchLocation
import com.esde.companion.domain.model.LogFolderValidation
import com.esde.companion.domain.model.MediaFolderValidation
import com.esde.companion.domain.model.MusicDuckingMode
import com.esde.companion.domain.model.ScreenBehavior
import com.esde.companion.domain.model.ThemePreference
import com.esde.companion.domain.repository.AppDrawerSettingsRepository
import com.esde.companion.domain.repository.DockSettingsRepository
import com.esde.companion.domain.repository.InstalledAppsRepository
import com.esde.companion.domain.repository.OnboardingRepository
import com.esde.companion.domain.usecase.ObserveDockAppsUseCase
import com.esde.companion.domain.usecase.ObserveDockEnabledUseCase
import com.esde.companion.domain.usecase.ObserveDockMaxAppsUseCase
import com.esde.companion.domain.usecase.ObserveDockSizeUseCase
import com.esde.companion.domain.usecase.ObserveInstalledAppsUseCase
import com.esde.companion.domain.usecase.ObserveOtherScreenLaunchAppsUseCase
import com.esde.companion.domain.usecase.ObserveOverlayOpacityUseCase
import com.esde.companion.domain.usecase.SetDockAppsUseCase
import com.esde.companion.domain.usecase.SetOtherScreenLaunchAppsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AppDockViewModelTest {
    private class FakeInstalledAppsRepository(private val apps: Flow<List<InstalledApp>>) : InstalledAppsRepository {
        override fun observeInstalledApps(): Flow<List<InstalledApp>> = apps
    }

    private class FakeDockSettingsRepository(
        initialEnabled: Boolean = true,
        initialMaxApps: Int = 5,
        initialDockApps: List<String> = emptyList(),
    ) : DockSettingsRepository {
        val enabled = MutableStateFlow(initialEnabled)
        val maxApps = MutableStateFlow(initialMaxApps)
        val size = MutableStateFlow(DockSize.Medium)
        val dockApps = MutableStateFlow(initialDockApps)

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

        override suspend fun setDockApps(packageNames: List<String>) {
            dockApps.value = packageNames
        }

        override fun observeDockApps(): Flow<List<String>> = dockApps
    }

    private class FakeAppDrawerSettingsRepository(
        initialOtherScreenLaunchApps: Set<String> = emptySet(),
    ) : AppDrawerSettingsRepository {
        val otherScreenLaunchApps = MutableStateFlow(initialOtherScreenLaunchApps)

        override suspend fun setHiddenApps(packageNames: Set<String>) {}

        override fun observeHiddenApps(): Flow<Set<String>> = flowOf(emptySet())

        override suspend fun setGridColumns(columns: Int) {}

        override fun observeGridColumns(): Flow<Int> = flowOf(4)

        override suspend fun setOtherScreenLaunchApps(packageNames: Set<String>) {
            otherScreenLaunchApps.value = packageNames
        }

        override fun observeOtherScreenLaunchApps(): Flow<Set<String>> = otherScreenLaunchApps

        override suspend fun setSortFoldersOnTop(sortOnTop: Boolean) {}

        override fun observeSortFoldersOnTop(): Flow<Boolean> = flowOf(true)

        override suspend fun setShowSearchBar(show: Boolean) {}

        override fun observeShowSearchBar(): Flow<Boolean> = flowOf(true)
    }

    private class FakeOnboardingRepository : OnboardingRepository {
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

        override fun observeOnboardingComplete(): Flow<Boolean> = flowOf(true)

        override suspend fun setVideoPlaybackEnabled(enabled: Boolean) {}

        override fun observeVideoPlaybackEnabled(): Flow<Boolean> = flowOf(false)

        override suspend fun setVideoDelaySeconds(seconds: Int) {}

        override fun observeVideoDelaySeconds(): Flow<Int> = flowOf(0)

        override suspend fun setVideoAudioEnabled(enabled: Boolean) {}

        override fun observeVideoAudioEnabled(): Flow<Boolean> = flowOf(true)

        override suspend fun setGamePlayingBehavior(behavior: ScreenBehavior) {}

        override fun observeGamePlayingBehavior(): Flow<ScreenBehavior> = flowOf(ScreenBehavior.Nothing)

        override suspend fun setGamePlayingDimPercent(percent: Int) {}

        override fun observeGamePlayingDimPercent(): Flow<Int> = flowOf(50)

        override suspend fun setScreensaverBehavior(behavior: ScreenBehavior) {}

        override fun observeScreensaverBehavior(): Flow<ScreenBehavior> = flowOf(ScreenBehavior.Nothing)

        override suspend fun setScreensaverDimPercent(percent: Int) {}

        override fun observeScreensaverDimPercent(): Flow<Int> = flowOf(50)

        override suspend fun setThemePreference(preference: ThemePreference) {}

        override fun observeThemePreference(): Flow<ThemePreference> = flowOf(ThemePreference.Auto)

        override suspend fun setMusicEnabled(enabled: Boolean) {}

        override fun observeMusicEnabled(): Flow<Boolean> = flowOf(true)

        override suspend fun setMusicPlayWhileBrowsingSystems(enabled: Boolean) {}

        override fun observeMusicPlayWhileBrowsingSystems(): Flow<Boolean> = flowOf(true)

        override suspend fun setMusicPlayWhileBrowsingGames(enabled: Boolean) {}

        override fun observeMusicPlayWhileBrowsingGames(): Flow<Boolean> = flowOf(true)

        override suspend fun setMusicPlayDuringScreensaver(enabled: Boolean) {}

        override fun observeMusicPlayDuringScreensaver(): Flow<Boolean> = flowOf(true)

        override suspend fun setMusicDuckingMode(mode: MusicDuckingMode) {}

        override fun observeMusicDuckingMode(): Flow<MusicDuckingMode> = flowOf(MusicDuckingMode.LowerVolume)

        override suspend fun setOverlayOpacityPercent(percent: Int) {}

        override fun observeOverlayOpacityPercent(): Flow<Int> = flowOf(80)

        override suspend fun saveCustomMusicFolderPath(path: String) {}

        override fun observeCustomMusicFolderPath(): Flow<String?> = flowOf(null)

        override suspend fun clearCustomMusicFolderPath() {}

        override suspend fun setCloseCompanionOnQuitEnabled(enabled: Boolean) {}

        override fun observeCloseCompanionOnQuitEnabled(): Flow<Boolean> = flowOf(false)

        override suspend fun setLaunchEsdeOnStartEnabled(enabled: Boolean) {}

        override fun observeLaunchEsdeOnStartEnabled(): Flow<Boolean> = flowOf(false)

        override suspend fun setDebugLoggingEnabled(enabled: Boolean) {}

        override fun observeDebugLoggingEnabled(): Flow<Boolean> = flowOf(false)

        override suspend fun setUpdateAchievementsOnScreensaverEnabled(enabled: Boolean) {}

        override fun observeUpdateAchievementsOnScreensaverEnabled(): Flow<Boolean> = flowOf(true)

        override suspend fun setPlaytimeStatsHardcoreModeEnabled(enabled: Boolean) {}

        override fun observePlaytimeStatsHardcoreModeEnabled(): Flow<Boolean> = flowOf(false)

        override suspend fun setBluetoothPermissionRequested(requested: Boolean) {}

        override fun observeBluetoothPermissionRequested(): Flow<Boolean> = flowOf(false)

        override suspend fun setFabAssignments(assignments: FabAssignments) {}

        override fun observeFabAssignments(): Flow<FabAssignments> = flowOf(FabAssignments.Default)
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

    private val allApps =
        listOf(
            InstalledApp(packageName = "com.example.a", label = "App A"),
            InstalledApp(packageName = "com.example.b", label = "App B"),
            InstalledApp(packageName = "com.example.c", label = "App C"),
        )

    private fun buildViewModel(
        apps: List<InstalledApp> = allApps,
        dockSettingsRepository: FakeDockSettingsRepository = FakeDockSettingsRepository(),
        appDrawerSettingsRepository: FakeAppDrawerSettingsRepository = FakeAppDrawerSettingsRepository(),
        onboardingRepository: FakeOnboardingRepository = FakeOnboardingRepository(),
    ): AppDockViewModel {
        val installedAppsRepository = FakeInstalledAppsRepository(flowOf(apps))
        return AppDockViewModel(
            observeDockEnabled = ObserveDockEnabledUseCase(dockSettingsRepository),
            observeDockApps = ObserveDockAppsUseCase(dockSettingsRepository),
            setDockApps = SetDockAppsUseCase(dockSettingsRepository),
            observeDockMaxApps = ObserveDockMaxAppsUseCase(dockSettingsRepository),
            observeDockSize = ObserveDockSizeUseCase(dockSettingsRepository),
            observeOverlayOpacity = ObserveOverlayOpacityUseCase(onboardingRepository),
            observeInstalledApps = ObserveInstalledAppsUseCase(installedAppsRepository),
            observeOtherScreenLaunchApps = ObserveOtherScreenLaunchAppsUseCase(appDrawerSettingsRepository),
            setOtherScreenLaunchApps = SetOtherScreenLaunchAppsUseCase(appDrawerSettingsRepository),
        )
    }

    @Test
    fun `dockItems resolves pinned package names to installed apps in order`() =
        runTest(testDispatcher) {
            val repository = FakeDockSettingsRepository(initialDockApps = listOf("com.example.c", "com.example.a"))
            val viewModel = buildViewModel(dockSettingsRepository = repository)

            val collectJob = launch { viewModel.dockItems.collect {} }
            advanceUntilIdle()

            assertEquals(listOf(allApps[2], allApps[0]), viewModel.dockItems.value)
            collectJob.cancel()
        }

    @Test
    fun `dockItems caps at maxApps without deleting from storage`() =
        runTest(testDispatcher) {
            val repository =
                FakeDockSettingsRepository(
                    initialMaxApps = 5,
                    initialDockApps = listOf("com.example.a", "com.example.b", "com.example.c"),
                )
            val viewModel = buildViewModel(dockSettingsRepository = repository)

            val collectJob = launch { viewModel.dockItems.collect {} }
            advanceUntilIdle()
            assertEquals(3, viewModel.dockItems.value.size)

            repository.maxApps.value = 1
            advanceUntilIdle()

            assertEquals(listOf(allApps[0]), viewModel.dockItems.value)
            assertEquals(listOf("com.example.a", "com.example.b", "com.example.c"), repository.dockApps.value)
            collectJob.cancel()
        }

    @Test
    fun `dockItems silently skips an uninstalled app`() =
        runTest(testDispatcher) {
            val repository = FakeDockSettingsRepository(initialDockApps = listOf("com.example.a", "com.example.gone"))
            val viewModel = buildViewModel(dockSettingsRepository = repository)

            val collectJob = launch { viewModel.dockItems.collect {} }
            advanceUntilIdle()

            assertEquals(listOf(allApps[0]), viewModel.dockItems.value)
            collectJob.cancel()
        }

    @Test
    fun `addToDock appends and no-ops once at maxApps`() =
        runTest(testDispatcher) {
            val repository = FakeDockSettingsRepository(initialMaxApps = 2, initialDockApps = listOf("com.example.a"))
            val viewModel = buildViewModel(dockSettingsRepository = repository)
            val collectJob = launch { viewModel.maxApps.collect {} }
            advanceUntilIdle()

            viewModel.addToDock("com.example.b")
            advanceUntilIdle()
            assertEquals(listOf("com.example.a", "com.example.b"), repository.dockApps.value)

            viewModel.addToDock("com.example.c")
            advanceUntilIdle()
            assertEquals(listOf("com.example.a", "com.example.b"), repository.dockApps.value)
            collectJob.cancel()
        }

    @Test
    fun `removeFromDock filters the package and closes the gap`() =
        runTest(testDispatcher) {
            val repository =
                FakeDockSettingsRepository(initialDockApps = listOf("com.example.a", "com.example.b", "com.example.c"))
            val viewModel = buildViewModel(dockSettingsRepository = repository)

            viewModel.removeFromDock("com.example.b")
            advanceUntilIdle()

            assertEquals(listOf("com.example.a", "com.example.c"), repository.dockApps.value)
        }

    @Test
    fun `moveLeft swaps with the previous entry and no-ops at the start`() =
        runTest(testDispatcher) {
            val repository =
                FakeDockSettingsRepository(initialDockApps = listOf("com.example.a", "com.example.b", "com.example.c"))
            val viewModel = buildViewModel(dockSettingsRepository = repository)

            viewModel.moveLeft("com.example.b")
            advanceUntilIdle()
            assertEquals(listOf("com.example.b", "com.example.a", "com.example.c"), repository.dockApps.value)

            viewModel.moveLeft("com.example.b")
            advanceUntilIdle()
            assertEquals(listOf("com.example.b", "com.example.a", "com.example.c"), repository.dockApps.value)
        }

    @Test
    fun `moveRight swaps with the next entry and no-ops at the end`() =
        runTest(testDispatcher) {
            val repository =
                FakeDockSettingsRepository(initialDockApps = listOf("com.example.a", "com.example.b", "com.example.c"))
            val viewModel = buildViewModel(dockSettingsRepository = repository)

            viewModel.moveRight("com.example.b")
            advanceUntilIdle()
            assertEquals(listOf("com.example.a", "com.example.c", "com.example.b"), repository.dockApps.value)

            viewModel.moveRight("com.example.b")
            advanceUntilIdle()
            assertEquals(listOf("com.example.a", "com.example.c", "com.example.b"), repository.dockApps.value)
        }

    @Test
    fun `recordLaunchLocation adds and removes the package from the shared preference set`() =
        runTest(testDispatcher) {
            val appDrawerSettingsRepository = FakeAppDrawerSettingsRepository()
            val viewModel = buildViewModel(appDrawerSettingsRepository = appDrawerSettingsRepository)

            viewModel.recordLaunchLocation("com.example.a", LaunchLocation.OtherScreen)
            advanceUntilIdle()
            assertEquals(setOf("com.example.a"), appDrawerSettingsRepository.otherScreenLaunchApps.value)

            viewModel.recordLaunchLocation("com.example.a", LaunchLocation.ThisScreen)
            advanceUntilIdle()
            assertEquals(emptySet<String>(), appDrawerSettingsRepository.otherScreenLaunchApps.value)
        }

    @Test
    fun `availableApps excludes already-pinned packages`() =
        runTest(testDispatcher) {
            val repository = FakeDockSettingsRepository(initialDockApps = listOf("com.example.a"))
            val viewModel = buildViewModel(dockSettingsRepository = repository)

            val collectJob = launch { viewModel.availableApps.collect {} }
            advanceUntilIdle()

            // AppDrawerShortcut is prepended whenever it isn't itself already pinned - see
            // AppDockViewModel.availableApps's kdoc.
            assertEquals(listOf(AppDrawerShortcut, allApps[1], allApps[2]), viewModel.availableApps.value)
            collectJob.cancel()
        }
}

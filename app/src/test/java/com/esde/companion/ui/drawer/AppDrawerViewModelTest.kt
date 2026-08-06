package com.esde.companion.ui.drawer

import com.esde.companion.domain.model.InstalledApp
import com.esde.companion.domain.model.LaunchLocation
import com.esde.companion.domain.model.LogFolderValidation
import com.esde.companion.domain.model.MediaFolderValidation
import com.esde.companion.domain.model.MusicDuckingMode
import com.esde.companion.domain.model.ScreenBehavior
import com.esde.companion.domain.model.ThemePreference
import com.esde.companion.domain.repository.AppDrawerSettingsRepository
import com.esde.companion.domain.repository.InstalledAppsRepository
import com.esde.companion.domain.repository.OnboardingRepository
import com.esde.companion.domain.usecase.ObserveGridColumnsUseCase
import com.esde.companion.domain.usecase.ObserveHiddenAppsUseCase
import com.esde.companion.domain.usecase.ObserveInstalledAppsUseCase
import com.esde.companion.domain.usecase.ObserveOtherScreenLaunchAppsUseCase
import com.esde.companion.domain.usecase.ObserveOverlayOpacityUseCase
import com.esde.companion.domain.usecase.SetHiddenAppsUseCase
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

class AppDrawerViewModelTest {

    private class FakeInstalledAppsRepository(private val apps: Flow<List<InstalledApp>>) : InstalledAppsRepository {
        override fun observeInstalledApps(): Flow<List<InstalledApp>> = apps
    }

    private class FakeAppDrawerSettingsRepository(
        initialHiddenApps: Set<String> = emptySet(),
        initialColumns: Int = 4,
        initialOtherScreenLaunchApps: Set<String> = emptySet(),
    ) : AppDrawerSettingsRepository {
        val hiddenApps = MutableStateFlow(initialHiddenApps)
        val columns = MutableStateFlow(initialColumns)
        val otherScreenLaunchApps = MutableStateFlow(initialOtherScreenLaunchApps)

        override suspend fun setHiddenApps(packageNames: Set<String>) { hiddenApps.value = packageNames }
        override fun observeHiddenApps(): Flow<Set<String>> = hiddenApps
        override suspend fun setGridColumns(columns: Int) { this.columns.value = columns }
        override fun observeGridColumns(): Flow<Int> = columns
        override suspend fun setOtherScreenLaunchApps(packageNames: Set<String>) { otherScreenLaunchApps.value = packageNames }
        override fun observeOtherScreenLaunchApps(): Flow<Set<String>> = otherScreenLaunchApps
    }

    private class FakeOnboardingRepository(initialOverlayOpacity: Int = 80) : OnboardingRepository {
        val overlayOpacity = MutableStateFlow(initialOverlayOpacity)

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
        override suspend fun setScreensaverBehavior(behavior: ScreenBehavior) {}
        override fun observeScreensaverBehavior(): Flow<ScreenBehavior> = flowOf(ScreenBehavior.Nothing)
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
        override suspend fun setOverlayOpacityPercent(percent: Int) { overlayOpacity.value = percent }
        override fun observeOverlayOpacityPercent(): Flow<Int> = overlayOpacity
        override suspend fun saveCustomMusicFolderPath(path: String) {}
        override fun observeCustomMusicFolderPath(): Flow<String?> = flowOf(null)
        override suspend fun clearCustomMusicFolderPath() {}
        override suspend fun setCloseCompanionOnQuitEnabled(enabled: Boolean) {}
        override fun observeCloseCompanionOnQuitEnabled(): Flow<Boolean> = flowOf(false)
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

    private val allApps = listOf(
        InstalledApp(packageName = "com.example.a", label = "App A"),
        InstalledApp(packageName = "com.example.b", label = "App B"),
        InstalledApp(packageName = "com.example.c", label = "App C"),
    )

    private fun buildViewModel(
        apps: List<InstalledApp> = allApps,
        settingsRepository: FakeAppDrawerSettingsRepository = FakeAppDrawerSettingsRepository(),
        onboardingRepository: FakeOnboardingRepository = FakeOnboardingRepository(),
    ): AppDrawerViewModel {
        val installedAppsRepository = FakeInstalledAppsRepository(flowOf(apps))
        return AppDrawerViewModel(
            observeInstalledApps = ObserveInstalledAppsUseCase(installedAppsRepository),
            observeHiddenApps = ObserveHiddenAppsUseCase(settingsRepository),
            setHiddenApps = SetHiddenAppsUseCase(settingsRepository),
            observeOtherScreenLaunchApps = ObserveOtherScreenLaunchAppsUseCase(settingsRepository),
            setOtherScreenLaunchApps = SetOtherScreenLaunchAppsUseCase(settingsRepository),
            observeOverlayOpacity = ObserveOverlayOpacityUseCase(onboardingRepository),
            observeGridColumns = ObserveGridColumnsUseCase(settingsRepository),
        )
    }

    @Test
    fun `installedApps excludes hidden packages`() = runTest(testDispatcher) {
        val settingsRepository = FakeAppDrawerSettingsRepository(initialHiddenApps = setOf("com.example.b"))
        val viewModel = buildViewModel(settingsRepository = settingsRepository)

        val collectJob = launch { viewModel.installedApps.collect {} }
        advanceUntilIdle()

        assertEquals(listOf(allApps[0], allApps[2]), viewModel.installedApps.value)
        collectJob.cancel()
    }

    @Test
    fun `installedApps includes everything when nothing is hidden`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()

        val collectJob = launch { viewModel.installedApps.collect {} }
        advanceUntilIdle()

        assertEquals(allApps, viewModel.installedApps.value)
        collectJob.cancel()
    }

    @Test
    fun `installedApps updates when hidden apps change`() = runTest(testDispatcher) {
        val settingsRepository = FakeAppDrawerSettingsRepository()
        val viewModel = buildViewModel(settingsRepository = settingsRepository)

        val collectJob = launch { viewModel.installedApps.collect {} }
        advanceUntilIdle()
        assertEquals(allApps, viewModel.installedApps.value)

        settingsRepository.hiddenApps.value = setOf("com.example.a")
        advanceUntilIdle()

        assertEquals(listOf(allApps[1], allApps[2]), viewModel.installedApps.value)
        collectJob.cancel()
    }

    @Test
    fun `drawerOpacityPercent reflects the repository value`() = runTest(testDispatcher) {
        val onboardingRepository = FakeOnboardingRepository(initialOverlayOpacity = 65)
        val viewModel = buildViewModel(onboardingRepository = onboardingRepository)

        val collectJob = launch { viewModel.drawerOpacityPercent.collect {} }
        advanceUntilIdle()

        assertEquals(65, viewModel.drawerOpacityPercent.value)
        collectJob.cancel()
    }

    @Test
    fun `gridColumns reflects the repository value`() = runTest(testDispatcher) {
        val settingsRepository = FakeAppDrawerSettingsRepository(initialColumns = 6)
        val viewModel = buildViewModel(settingsRepository = settingsRepository)

        val collectJob = launch { viewModel.gridColumns.collect {} }
        advanceUntilIdle()

        assertEquals(6, viewModel.gridColumns.value)
        collectJob.cancel()
    }

    @Test
    fun `otherScreenLaunchApps reflects the repository value`() = runTest(testDispatcher) {
        val settingsRepository = FakeAppDrawerSettingsRepository(initialOtherScreenLaunchApps = setOf("com.example.a"))
        val viewModel = buildViewModel(settingsRepository = settingsRepository)

        val collectJob = launch { viewModel.otherScreenLaunchApps.collect {} }
        advanceUntilIdle()

        assertEquals(setOf("com.example.a"), viewModel.otherScreenLaunchApps.value)
        collectJob.cancel()
    }

    @Test
    fun `recordLaunchLocation adds the package when set to other screen`() = runTest(testDispatcher) {
        val settingsRepository = FakeAppDrawerSettingsRepository()
        val viewModel = buildViewModel(settingsRepository = settingsRepository)

        viewModel.recordLaunchLocation("com.example.a", LaunchLocation.OtherScreen)
        advanceUntilIdle()

        assertEquals(setOf("com.example.a"), settingsRepository.otherScreenLaunchApps.value)
    }

    @Test
    fun `recordLaunchLocation removes the package when set back to this screen`() = runTest(testDispatcher) {
        val settingsRepository = FakeAppDrawerSettingsRepository(
            initialOtherScreenLaunchApps = setOf("com.example.a", "com.example.b"),
        )
        val viewModel = buildViewModel(settingsRepository = settingsRepository)

        viewModel.recordLaunchLocation("com.example.a", LaunchLocation.ThisScreen)
        advanceUntilIdle()

        assertEquals(setOf("com.example.b"), settingsRepository.otherScreenLaunchApps.value)
    }

    @Test
    fun `hideApp adds the package to the hidden set without disturbing others`() = runTest(testDispatcher) {
        val settingsRepository = FakeAppDrawerSettingsRepository(initialHiddenApps = setOf("com.example.b"))
        val viewModel = buildViewModel(settingsRepository = settingsRepository)

        viewModel.hideApp("com.example.a")
        advanceUntilIdle()

        assertEquals(setOf("com.example.a", "com.example.b"), settingsRepository.hiddenApps.value)
    }
}
package com.esde.companion.ui.drawer

import com.esde.companion.domain.model.AppFolder
import com.esde.companion.domain.model.DrawerItem
import com.esde.companion.domain.model.FabAssignments
import com.esde.companion.domain.model.InstalledApp
import com.esde.companion.domain.model.LaunchLocation
import com.esde.companion.domain.model.LogFolderValidation
import com.esde.companion.domain.model.MediaFolderValidation
import com.esde.companion.domain.model.MusicDuckingMode
import com.esde.companion.domain.model.ScreenBehavior
import com.esde.companion.domain.model.ThemePreference
import com.esde.companion.domain.repository.AppDrawerSettingsRepository
import com.esde.companion.domain.repository.AppFolderRepository
import com.esde.companion.domain.repository.InstalledAppsRepository
import com.esde.companion.domain.repository.OnboardingRepository
import com.esde.companion.domain.usecase.ObserveAppFoldersUseCase
import com.esde.companion.domain.usecase.ObserveGridColumnsUseCase
import com.esde.companion.domain.usecase.ObserveHiddenAppsUseCase
import com.esde.companion.domain.usecase.ObserveInstalledAppsUseCase
import com.esde.companion.domain.usecase.ObserveOtherScreenLaunchAppsUseCase
import com.esde.companion.domain.usecase.ObserveOverlayOpacityUseCase
import com.esde.companion.domain.usecase.ObserveShowSearchBarUseCase
import com.esde.companion.domain.usecase.ObserveSortFoldersOnTopUseCase
import com.esde.companion.domain.usecase.SetAppFoldersUseCase
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
        initialSortFoldersOnTop: Boolean = true,
        initialShowSearchBar: Boolean = true,
    ) : AppDrawerSettingsRepository {
        val hiddenApps = MutableStateFlow(initialHiddenApps)
        val columns = MutableStateFlow(initialColumns)
        val otherScreenLaunchApps = MutableStateFlow(initialOtherScreenLaunchApps)
        val sortFoldersOnTop = MutableStateFlow(initialSortFoldersOnTop)
        val showSearchBar = MutableStateFlow(initialShowSearchBar)

        override suspend fun setHiddenApps(packageNames: Set<String>) {
            hiddenApps.value = packageNames
        }

        override fun observeHiddenApps(): Flow<Set<String>> = hiddenApps

        override suspend fun setGridColumns(columns: Int) {
            this.columns.value = columns
        }

        override fun observeGridColumns(): Flow<Int> = columns

        override suspend fun setOtherScreenLaunchApps(packageNames: Set<String>) {
            otherScreenLaunchApps.value = packageNames
        }

        override fun observeOtherScreenLaunchApps(): Flow<Set<String>> = otherScreenLaunchApps

        override suspend fun setSortFoldersOnTop(sortOnTop: Boolean) {
            sortFoldersOnTop.value = sortOnTop
        }

        override fun observeSortFoldersOnTop(): Flow<Boolean> = sortFoldersOnTop

        override suspend fun setShowSearchBar(show: Boolean) {
            showSearchBar.value = show
        }

        override fun observeShowSearchBar(): Flow<Boolean> = showSearchBar
    }

    private class FakeAppFolderRepository(initialFolders: List<AppFolder> = emptyList()) : AppFolderRepository {
        val folders = MutableStateFlow(initialFolders)

        override suspend fun setFolders(folders: List<AppFolder>) {
            this.folders.value = folders
        }

        override fun observeFolders(): Flow<List<AppFolder>> = folders
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

        override suspend fun setOverlayOpacityPercent(percent: Int) {
            overlayOpacity.value = percent
        }

        override fun observeOverlayOpacityPercent(): Flow<Int> = overlayOpacity

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
        settingsRepository: FakeAppDrawerSettingsRepository = FakeAppDrawerSettingsRepository(),
        onboardingRepository: FakeOnboardingRepository = FakeOnboardingRepository(),
        folderRepository: FakeAppFolderRepository = FakeAppFolderRepository(),
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
            observeAppFolders = ObserveAppFoldersUseCase(folderRepository),
            setAppFolders = SetAppFoldersUseCase(folderRepository),
            observeSortFoldersOnTop = ObserveSortFoldersOnTopUseCase(settingsRepository),
            observeShowSearchBar = ObserveShowSearchBarUseCase(settingsRepository),
        )
    }

    @Test
    fun `setSearchQuery filters drawerItems to matching apps including hidden ones`() =
        runTest(testDispatcher) {
            val settingsRepository = FakeAppDrawerSettingsRepository(initialHiddenApps = setOf("com.example.b"))
            val viewModel = buildViewModel(settingsRepository = settingsRepository)

            val collectJob = launch { viewModel.drawerItems.collect {} }
            advanceUntilIdle()

            viewModel.setSearchQuery("App B")
            advanceUntilIdle()

            assertEquals(listOf(DrawerItem.App(allApps[1])), viewModel.drawerItems.value)
            collectJob.cancel()
        }

    @Test
    fun `clearSearchQuery restores the unfiltered drawer list`() =
        runTest(testDispatcher) {
            val viewModel = buildViewModel()

            val collectJob = launch { viewModel.drawerItems.collect {} }
            advanceUntilIdle()

            viewModel.setSearchQuery("App C")
            advanceUntilIdle()
            assertEquals(listOf(DrawerItem.App(allApps[2])), viewModel.drawerItems.value)

            viewModel.clearSearchQuery()
            advanceUntilIdle()

            assertEquals(allApps.map(DrawerItem::App), viewModel.drawerItems.value)
            assertEquals("", viewModel.searchQuery.value)
            collectJob.cancel()
        }

    @Test
    fun `showSearchBar reflects the persisted setting`() =
        runTest(testDispatcher) {
            val settingsRepository = FakeAppDrawerSettingsRepository(initialShowSearchBar = false)
            val viewModel = buildViewModel(settingsRepository = settingsRepository)

            val collectJob = launch { viewModel.showSearchBar.collect {} }
            advanceUntilIdle()
            assertEquals(false, viewModel.showSearchBar.value)

            settingsRepository.setShowSearchBar(true)
            advanceUntilIdle()

            assertEquals(true, viewModel.showSearchBar.value)
            collectJob.cancel()
        }

    @Test
    fun `drawerItems excludes hidden packages`() =
        runTest(testDispatcher) {
            val settingsRepository = FakeAppDrawerSettingsRepository(initialHiddenApps = setOf("com.example.b"))
            val viewModel = buildViewModel(settingsRepository = settingsRepository)

            val collectJob = launch { viewModel.drawerItems.collect {} }
            advanceUntilIdle()

            assertEquals(listOf(DrawerItem.App(allApps[0]), DrawerItem.App(allApps[2])), viewModel.drawerItems.value)
            collectJob.cancel()
        }

    @Test
    fun `drawerItems includes everything when nothing is hidden`() =
        runTest(testDispatcher) {
            val viewModel = buildViewModel()

            val collectJob = launch { viewModel.drawerItems.collect {} }
            advanceUntilIdle()

            assertEquals(allApps.map(DrawerItem::App), viewModel.drawerItems.value)
            collectJob.cancel()
        }

    @Test
    fun `drawerItems updates when hidden apps change`() =
        runTest(testDispatcher) {
            val settingsRepository = FakeAppDrawerSettingsRepository()
            val viewModel = buildViewModel(settingsRepository = settingsRepository)

            val collectJob = launch { viewModel.drawerItems.collect {} }
            advanceUntilIdle()
            assertEquals(allApps.map(DrawerItem::App), viewModel.drawerItems.value)

            settingsRepository.hiddenApps.value = setOf("com.example.a")
            advanceUntilIdle()

            assertEquals(listOf(DrawerItem.App(allApps[1]), DrawerItem.App(allApps[2])), viewModel.drawerItems.value)
            collectJob.cancel()
        }

    @Test
    fun `drawerItems groups an app into its folder instead of showing it ungrouped`() =
        runTest(testDispatcher) {
            val folder = AppFolder(id = "folder-1", name = "Group", memberPackageNames = setOf("com.example.a"))
            val folderRepository = FakeAppFolderRepository(initialFolders = listOf(folder))
            val viewModel = buildViewModel(folderRepository = folderRepository)

            val collectJob = launch { viewModel.drawerItems.collect {} }
            advanceUntilIdle()

            // sortFoldersOnTop defaults to true, so the folder comes first regardless of
            // where "Group" would otherwise fall alphabetically among "App B"/"App C".
            assertEquals(
                listOf(DrawerItem.Folder(folder, listOf(allApps[0])), DrawerItem.App(allApps[1]), DrawerItem.App(allApps[2])),
                viewModel.drawerItems.value,
            )
            collectJob.cancel()
        }

    @Test
    fun `drawerItems fully interleaves folders and apps when sortFoldersOnTop is false`() =
        runTest(testDispatcher) {
            val folder = AppFolder(id = "folder-1", name = "Group", memberPackageNames = setOf("com.example.a"))
            val folderRepository = FakeAppFolderRepository(initialFolders = listOf(folder))
            val settingsRepository = FakeAppDrawerSettingsRepository(initialSortFoldersOnTop = false)
            val viewModel = buildViewModel(settingsRepository = settingsRepository, folderRepository = folderRepository)

            val collectJob = launch { viewModel.drawerItems.collect {} }
            advanceUntilIdle()

            // "App B"/"App C" sort before "Group" (the folder's name) alphabetically.
            assertEquals(
                listOf(DrawerItem.App(allApps[1]), DrawerItem.App(allApps[2]), DrawerItem.Folder(folder, listOf(allApps[0]))),
                viewModel.drawerItems.value,
            )
            collectJob.cancel()
        }

    @Test
    fun `createFolderAndAddApp persists a new folder with a single member`() =
        runTest(testDispatcher) {
            val folderRepository = FakeAppFolderRepository()
            val viewModel = buildViewModel(folderRepository = folderRepository)

            viewModel.createFolderAndAddApp("Games", "com.example.a")
            advanceUntilIdle()

            val created = folderRepository.folders.value.single()
            assertEquals("Games", created.name)
            assertEquals(setOf("com.example.a"), created.memberPackageNames)
        }

    @Test
    fun `addAppToFolder adds to the target folder and strips membership from every other folder`() =
        runTest(testDispatcher) {
            val target = AppFolder(id = "target", name = "Target", memberPackageNames = emptySet())
            val other = AppFolder(id = "other", name = "Other", memberPackageNames = setOf("com.example.a"))
            val folderRepository = FakeAppFolderRepository(initialFolders = listOf(target, other))
            val viewModel = buildViewModel(folderRepository = folderRepository)

            viewModel.addAppToFolder("target", "com.example.a")
            advanceUntilIdle()

            val updated = folderRepository.folders.value.associateBy { it.id }
            assertEquals(setOf("com.example.a"), updated.getValue("target").memberPackageNames)
            assertEquals(emptySet<String>(), updated.getValue("other").memberPackageNames)
        }

    @Test
    fun `removeAppFromFolder shrinks a multi-member folder without deleting it`() =
        runTest(testDispatcher) {
            val folder = AppFolder(id = "folder-1", name = "Group", memberPackageNames = setOf("com.example.a", "com.example.b"))
            val folderRepository = FakeAppFolderRepository(initialFolders = listOf(folder))
            val viewModel = buildViewModel(folderRepository = folderRepository)

            viewModel.removeAppFromFolder("folder-1", "com.example.a")
            advanceUntilIdle()

            val remaining = folderRepository.folders.value.single()
            assertEquals(setOf("com.example.b"), remaining.memberPackageNames)
        }

    @Test
    fun `removeAppFromFolder deletes the folder once it hits zero members`() =
        runTest(testDispatcher) {
            val folder = AppFolder(id = "folder-1", name = "Group", memberPackageNames = setOf("com.example.a"))
            val otherFolder = AppFolder(id = "folder-2", name = "Other", memberPackageNames = setOf("com.example.b"))
            val folderRepository = FakeAppFolderRepository(initialFolders = listOf(folder, otherFolder))
            val viewModel = buildViewModel(folderRepository = folderRepository)

            viewModel.removeAppFromFolder("folder-1", "com.example.a")
            advanceUntilIdle()

            assertEquals(listOf(otherFolder), folderRepository.folders.value)
        }

    @Test
    fun `renameFolder updates only the target folder's name`() =
        runTest(testDispatcher) {
            val target = AppFolder(id = "target", name = "Old Name", memberPackageNames = setOf("com.example.a"))
            val other = AppFolder(id = "other", name = "Other", memberPackageNames = setOf("com.example.b"))
            val folderRepository = FakeAppFolderRepository(initialFolders = listOf(target, other))
            val viewModel = buildViewModel(folderRepository = folderRepository)

            viewModel.renameFolder("target", "New Name")
            advanceUntilIdle()

            val updated = folderRepository.folders.value.associateBy { it.id }
            assertEquals("New Name", updated.getValue("target").name)
            assertEquals(setOf("com.example.a"), updated.getValue("target").memberPackageNames)
            assertEquals(other, updated.getValue("other"))
        }

    @Test
    fun `drawerOpacityPercent reflects the repository value`() =
        runTest(testDispatcher) {
            val onboardingRepository = FakeOnboardingRepository(initialOverlayOpacity = 65)
            val viewModel = buildViewModel(onboardingRepository = onboardingRepository)

            val collectJob = launch { viewModel.drawerOpacityPercent.collect {} }
            advanceUntilIdle()

            assertEquals(65, viewModel.drawerOpacityPercent.value)
            collectJob.cancel()
        }

    @Test
    fun `gridColumns reflects the repository value`() =
        runTest(testDispatcher) {
            val settingsRepository = FakeAppDrawerSettingsRepository(initialColumns = 6)
            val viewModel = buildViewModel(settingsRepository = settingsRepository)

            val collectJob = launch { viewModel.gridColumns.collect {} }
            advanceUntilIdle()

            assertEquals(6, viewModel.gridColumns.value)
            collectJob.cancel()
        }

    @Test
    fun `otherScreenLaunchApps reflects the repository value`() =
        runTest(testDispatcher) {
            val settingsRepository = FakeAppDrawerSettingsRepository(initialOtherScreenLaunchApps = setOf("com.example.a"))
            val viewModel = buildViewModel(settingsRepository = settingsRepository)

            val collectJob = launch { viewModel.otherScreenLaunchApps.collect {} }
            advanceUntilIdle()

            assertEquals(setOf("com.example.a"), viewModel.otherScreenLaunchApps.value)
            collectJob.cancel()
        }

    @Test
    fun `recordLaunchLocation adds the package when set to other screen`() =
        runTest(testDispatcher) {
            val settingsRepository = FakeAppDrawerSettingsRepository()
            val viewModel = buildViewModel(settingsRepository = settingsRepository)

            viewModel.recordLaunchLocation("com.example.a", LaunchLocation.OtherScreen)
            advanceUntilIdle()

            assertEquals(setOf("com.example.a"), settingsRepository.otherScreenLaunchApps.value)
        }

    @Test
    fun `recordLaunchLocation removes the package when set back to this screen`() =
        runTest(testDispatcher) {
            val settingsRepository =
                FakeAppDrawerSettingsRepository(
                    initialOtherScreenLaunchApps = setOf("com.example.a", "com.example.b"),
                )
            val viewModel = buildViewModel(settingsRepository = settingsRepository)

            viewModel.recordLaunchLocation("com.example.a", LaunchLocation.ThisScreen)
            advanceUntilIdle()

            assertEquals(setOf("com.example.b"), settingsRepository.otherScreenLaunchApps.value)
        }

    @Test
    fun `setAppHidden true adds the package to the hidden set without disturbing others`() =
        runTest(testDispatcher) {
            val settingsRepository = FakeAppDrawerSettingsRepository(initialHiddenApps = setOf("com.example.b"))
            val viewModel = buildViewModel(settingsRepository = settingsRepository)

            viewModel.setAppHidden("com.example.a", hidden = true)
            advanceUntilIdle()

            assertEquals(setOf("com.example.a", "com.example.b"), settingsRepository.hiddenApps.value)
        }

    @Test
    fun `setAppHidden false removes the package from the hidden set`() =
        runTest(testDispatcher) {
            val settingsRepository =
                FakeAppDrawerSettingsRepository(initialHiddenApps = setOf("com.example.a", "com.example.b"))
            val viewModel = buildViewModel(settingsRepository = settingsRepository)

            viewModel.setAppHidden("com.example.a", hidden = false)
            advanceUntilIdle()

            assertEquals(setOf("com.example.b"), settingsRepository.hiddenApps.value)
        }

    @Test
    fun `setAppHidden true removes the app from its folder`() =
        runTest(testDispatcher) {
            val folder =
                AppFolder(
                    id = "folder-1",
                    name = "Games",
                    memberPackageNames = setOf("com.example.a", "com.example.b"),
                )
            val folderRepository = FakeAppFolderRepository(initialFolders = listOf(folder))
            val viewModel = buildViewModel(folderRepository = folderRepository)

            viewModel.setAppHidden("com.example.a", hidden = true)
            advanceUntilIdle()

            assertEquals(
                listOf(folder.copy(memberPackageNames = setOf("com.example.b"))),
                folderRepository.folders.value,
            )
        }

    @Test
    fun `setAppHidden true deletes the folder entirely if it was the only member`() =
        runTest(testDispatcher) {
            val folder = AppFolder(id = "folder-1", name = "Games", memberPackageNames = setOf("com.example.a"))
            val folderRepository = FakeAppFolderRepository(initialFolders = listOf(folder))
            val viewModel = buildViewModel(folderRepository = folderRepository)

            viewModel.setAppHidden("com.example.a", hidden = true)
            advanceUntilIdle()

            assertEquals(emptyList<AppFolder>(), folderRepository.folders.value)
        }

    @Test
    fun `setAppHidden true leaves folders untouched when the app is not a member of any`() =
        runTest(testDispatcher) {
            val folder = AppFolder(id = "folder-1", name = "Games", memberPackageNames = setOf("com.example.b"))
            val folderRepository = FakeAppFolderRepository(initialFolders = listOf(folder))
            val viewModel = buildViewModel(folderRepository = folderRepository)

            viewModel.setAppHidden("com.example.a", hidden = true)
            advanceUntilIdle()

            assertEquals(listOf(folder), folderRepository.folders.value)
        }
}

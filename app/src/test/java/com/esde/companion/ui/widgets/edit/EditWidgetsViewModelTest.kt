package com.esde.companion.ui.widgets.edit

import com.esde.companion.domain.model.DockSize
import com.esde.companion.domain.model.GameDescription
import com.esde.companion.domain.model.GameMedia
import com.esde.companion.domain.model.GameReference
import com.esde.companion.domain.model.GridDimensions
import com.esde.companion.domain.model.ImageTransitionMode
import com.esde.companion.domain.model.InstalledApp
import com.esde.companion.domain.model.LogFolderValidation
import com.esde.companion.domain.model.LogoTransitionMode
import com.esde.companion.domain.model.MediaFolderValidation
import com.esde.companion.domain.model.MediaType
import com.esde.companion.domain.model.MusicDuckingMode
import com.esde.companion.domain.model.PlacedWidget
import com.esde.companion.domain.model.SavedWidgetCanvas
import com.esde.companion.domain.model.ScaleMode
import com.esde.companion.domain.model.ScreenBehavior
import com.esde.companion.domain.model.StateGroup
import com.esde.companion.domain.model.ThemePreference
import com.esde.companion.domain.model.WidgetContent
import com.esde.companion.domain.model.WidgetType
import com.esde.companion.domain.repository.BundledSystemLogoRepository
import com.esde.companion.domain.repository.CustomSystemImageRepository
import com.esde.companion.domain.repository.CustomSystemLogoRepository
import com.esde.companion.domain.repository.DockSettingsRepository
import com.esde.companion.domain.repository.GameDescriptionRepository
import com.esde.companion.domain.repository.GameMediaRepository
import com.esde.companion.domain.repository.InstalledAppsRepository
import com.esde.companion.domain.repository.LastKnownContextRepository
import com.esde.companion.domain.repository.OnboardingRepository
import com.esde.companion.domain.repository.SystemMediaRepository
import com.esde.companion.domain.repository.WidgetLayoutRepository
import com.esde.companion.domain.usecase.ObserveDockAppsUseCase
import com.esde.companion.domain.usecase.ObserveDockEnabledUseCase
import com.esde.companion.domain.usecase.ObserveDockMaxAppsUseCase
import com.esde.companion.domain.usecase.ObserveDockSizeUseCase
import com.esde.companion.domain.usecase.ObserveImageTransitionModeUseCase
import com.esde.companion.domain.usecase.ObserveInstalledAppsUseCase
import com.esde.companion.domain.usecase.ObserveLastGameReferenceUseCase
import com.esde.companion.domain.usecase.ObserveLastSystemShortNameUseCase
import com.esde.companion.domain.usecase.ObserveLogoTransitionModeUseCase
import com.esde.companion.domain.usecase.ObserveOverlayOpacityUseCase
import com.esde.companion.domain.usecase.ObserveWidgetCanvasUseCase
import com.esde.companion.domain.usecase.ResolveBundledSystemLogoUseCase
import com.esde.companion.domain.usecase.ResolveCustomSystemImageUseCase
import com.esde.companion.domain.usecase.ResolveCustomSystemLogoUseCase
import com.esde.companion.domain.usecase.ResolveGameDescriptionUseCase
import com.esde.companion.domain.usecase.ResolveGameMediaUseCase
import com.esde.companion.domain.usecase.ResolveRandomSystemMediaUseCase
import com.esde.companion.domain.usecase.SaveWidgetCanvasUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EditWidgetsViewModelTest {

    // --- Fakes -------------------------------------------------------------------------

    /**
     * Per-canvas flows, same shape as FileWidgetLayoutRepository's real persistence.
     * Deliberately does NOT special-case "empty" - that's ObserveWidgetCanvasUseCase's
     * job (falling back to defaultCanvas), same as production wiring, so tests see
     * exactly what the real use case would hand the ViewModel.
     */
    private class FakeWidgetLayoutRepository : WidgetLayoutRepository {
        private val canvases = mutableMapOf<StateGroup, MutableStateFlow<SavedWidgetCanvas>>()
        val locked = MutableStateFlow(false)
        var lastSaved: Pair<StateGroup, List<PlacedWidget>>? = null

        /** [grid] defaults to null - "matches whatever grid it's loaded under", same as
         * production data saved before grid-tagging existed - so existing tests that don't
         * care about rescaling keep seeing their seeded widgets completely unchanged. */
        fun seed(stateGroup: StateGroup, widgets: List<PlacedWidget>, grid: GridDimensions? = null) {
            flowFor(stateGroup).value = SavedWidgetCanvas(grid, widgets)
        }

        private fun flowFor(stateGroup: StateGroup) =
            canvases.getOrPut(stateGroup) { MutableStateFlow(SavedWidgetCanvas(grid = null, widgets = emptyList())) }

        override fun observeCanvas(stateGroup: StateGroup): Flow<SavedWidgetCanvas> = flowFor(stateGroup)

        override suspend fun saveCanvas(stateGroup: StateGroup, widgets: List<PlacedWidget>, grid: GridDimensions) {
            lastSaved = stateGroup to widgets
            flowFor(stateGroup).value = SavedWidgetCanvas(grid, widgets)
        }

        override fun observeWidgetsLocked(): Flow<Boolean> = locked
        override suspend fun setWidgetsLocked(locked: Boolean) { this.locked.value = locked }
    }

    private class FakeGameMediaRepository(
        private val media: GameMedia = GameMedia(baseRelativePath = null, filesByType = emptyMap()),
    ) : GameMediaRepository {
        override suspend fun resolveMedia(systemShortName: String, romPath: String): GameMedia = media
    }

    private class FakeGameDescriptionRepository(
        private val description: GameDescription = GameDescription(text = null),
    ) : GameDescriptionRepository {
        override suspend fun resolveDescription(systemShortName: String, romPath: String): GameDescription = description
    }

    private class FakeSystemMediaRepository(
        private val filesByType: Map<MediaType, String> = emptyMap(),
    ) : SystemMediaRepository {
        override suspend fun randomMedia(systemShortName: String, mediaType: MediaType): String? = filesByType[mediaType]
    }

    /** Returns a distinct value every call, unlike [FakeSystemMediaRepository]'s fixed
     * mapping - used to prove EditWidgetsViewModel's own caching (not the repository's)
     * is what keeps repeated previewContent resolutions stable within one edit session. */
    private class CountingSystemMediaRepository : SystemMediaRepository {
        var callCount = 0
            private set

        override suspend fun randomMedia(systemShortName: String, mediaType: MediaType): String? {
            callCount++
            return "/media/$systemShortName/$mediaType/pick-$callCount.png"
        }
    }

    private class FakeCustomSystemImageRepository(
        private val imagesByShortName: Map<String, String> = emptyMap(),
    ) : CustomSystemImageRepository {
        override suspend fun findImage(systemShortName: String): String? = imagesByShortName[systemShortName]
    }

    private class FakeCustomSystemLogoRepository(
        private val logosByShortName: Map<String, String> = emptyMap(),
    ) : CustomSystemLogoRepository {
        override suspend fun findLogo(systemShortName: String): String? = logosByShortName[systemShortName]
    }

    private class FakeBundledSystemLogoRepository(
        private val assetPathsByName: Map<String, String> = emptyMap(),
    ) : BundledSystemLogoRepository {
        override suspend fun findLogoAssetPath(assetName: String): String? = assetPathsByName[assetName]
    }

    private class FakeLastKnownContextRepository(
        initialSystemShortName: String? = null,
        initialGameReference: GameReference? = null,
    ) : LastKnownContextRepository {
        val systemShortName = MutableStateFlow(initialSystemShortName)
        val gameReference = MutableStateFlow(initialGameReference)

        override fun observeLastSystemShortName(): Flow<String?> = systemShortName
        override suspend fun setLastSystemShortName(systemShortName: String) { this.systemShortName.value = systemShortName }
        override fun observeLastGameReference(): Flow<GameReference?> = gameReference
        override suspend fun setLastGameReference(gameReference: GameReference) { this.gameReference.value = gameReference }
    }

    /** Only imageTransitionMode/logoTransitionMode are exercised by this ViewModel -
     * everything else here is a fixed default, not under test. */
    private class FakeOnboardingRepository : OnboardingRepository {
        override fun defaultLogFolderPath() = ""
        override fun defaultMediaFolderPath() = ""
        override suspend fun validateLogFolder(path: String) = LogFolderValidation.FolderNotFound
        override suspend fun validateMediaFolder(path: String) = MediaFolderValidation.FolderNotFound
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
        override suspend fun setThemePreference(preference: ThemePreference) {}
        override fun observeThemePreference(): Flow<ThemePreference> = flowOf(ThemePreference.Auto)
        override suspend fun setGamePlayingBehavior(behavior: ScreenBehavior) {}
        override fun observeGamePlayingBehavior(): Flow<ScreenBehavior> = flowOf(ScreenBehavior.Nothing)
        override suspend fun setScreensaverBehavior(behavior: ScreenBehavior) {}
        override fun observeScreensaverBehavior(): Flow<ScreenBehavior> = flowOf(ScreenBehavior.Nothing)
        override suspend fun setVideoPlaybackEnabled(enabled: Boolean) {}
        override fun observeVideoPlaybackEnabled(): Flow<Boolean> = flowOf(false)
        override suspend fun setVideoDelaySeconds(seconds: Int) {}
        override fun observeVideoDelaySeconds(): Flow<Int> = flowOf(0)
        override suspend fun setVideoAudioEnabled(enabled: Boolean) {}
        override fun observeVideoAudioEnabled(): Flow<Boolean> = flowOf(true)
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
        override suspend fun setImageTransitionMode(mode: ImageTransitionMode) {}
        override fun observeImageTransitionMode(): Flow<ImageTransitionMode> = flowOf(ImageTransitionMode.None)
        override suspend fun setLogoTransitionMode(mode: LogoTransitionMode) {}
        override fun observeLogoTransitionMode(): Flow<LogoTransitionMode> = flowOf(LogoTransitionMode.None)
    }

    private class FakeDockSettingsRepository(
        initialEnabled: Boolean = false,
        initialMaxApps: Int = 5,
        initialSize: DockSize = DockSize.Medium,
        initialApps: List<String> = emptyList(),
    ) : DockSettingsRepository {
        val enabled = MutableStateFlow(initialEnabled)
        val maxApps = MutableStateFlow(initialMaxApps)
        val size = MutableStateFlow(initialSize)
        val apps = MutableStateFlow(initialApps)

        override suspend fun setDockEnabled(enabled: Boolean) { this.enabled.value = enabled }
        override fun observeDockEnabled(): Flow<Boolean> = enabled
        override suspend fun setDockMaxApps(maxApps: Int) { this.maxApps.value = maxApps }
        override fun observeDockMaxApps(): Flow<Int> = maxApps
        override suspend fun setDockSize(size: DockSize) { this.size.value = size }
        override fun observeDockSize(): Flow<DockSize> = size
        override suspend fun setDockApps(packageNames: List<String>) { apps.value = packageNames }
        override fun observeDockApps(): Flow<List<String>> = apps
    }

    private class FakeInstalledAppsRepository(
        private val apps: List<InstalledApp> = emptyList(),
    ) : InstalledAppsRepository {
        override fun observeInstalledApps(): Flow<List<InstalledApp>> = flowOf(apps)
    }

    // --- Setup ---------------------------------------------------------------------------

    private val testDispatcher = StandardTestDispatcher()
    private val grid = GridDimensions(columns = 10, rows = 10)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun placedWidget(
        id: String,
        zIndex: Int,
        gridColumn: Int = 0,
        gridRow: Int = 0,
        columnSpan: Int = 2,
        rowSpan: Int = 2,
        widgetType: WidgetType = WidgetType.ColorBackground(colorArgb = 0xFF000000, alpha = 1f),
    ) = PlacedWidget(
        id = id,
        widgetType = widgetType,
        gridColumn = gridColumn,
        gridRow = gridRow,
        columnSpan = columnSpan,
        rowSpan = rowSpan,
        zIndex = zIndex,
    )

    private fun buildViewModel(
        widgetLayoutRepository: FakeWidgetLayoutRepository = FakeWidgetLayoutRepository(),
        gameMediaRepository: FakeGameMediaRepository = FakeGameMediaRepository(),
        systemMediaRepository: SystemMediaRepository = FakeSystemMediaRepository(),
        customSystemImageRepository: FakeCustomSystemImageRepository = FakeCustomSystemImageRepository(),
        customSystemLogoRepository: FakeCustomSystemLogoRepository = FakeCustomSystemLogoRepository(),
        bundledSystemLogoRepository: FakeBundledSystemLogoRepository = FakeBundledSystemLogoRepository(),
        lastKnownContextRepository: FakeLastKnownContextRepository = FakeLastKnownContextRepository(),
        onboardingRepository: FakeOnboardingRepository = FakeOnboardingRepository(),
        dockSettingsRepository: FakeDockSettingsRepository = FakeDockSettingsRepository(),
        installedAppsRepository: FakeInstalledAppsRepository = FakeInstalledAppsRepository(),
    ) = EditWidgetsViewModel(
        observeWidgetCanvas = ObserveWidgetCanvasUseCase(widgetLayoutRepository),
        saveWidgetCanvas = SaveWidgetCanvasUseCase(widgetLayoutRepository),
        resolveGameMedia = ResolveGameMediaUseCase(gameMediaRepository),
        resolveGameDescription = ResolveGameDescriptionUseCase(FakeGameDescriptionRepository()),
        resolveRandomSystemMedia = ResolveRandomSystemMediaUseCase(systemMediaRepository),
        resolveCustomSystemImage = ResolveCustomSystemImageUseCase(customSystemImageRepository),
        resolveCustomSystemLogo = ResolveCustomSystemLogoUseCase(customSystemLogoRepository),
        resolveBundledSystemLogo = ResolveBundledSystemLogoUseCase(bundledSystemLogoRepository),
        observeLastSystemShortName = ObserveLastSystemShortNameUseCase(lastKnownContextRepository),
        observeLastGameReference = ObserveLastGameReferenceUseCase(lastKnownContextRepository),
        observeImageTransitionMode = ObserveImageTransitionModeUseCase(onboardingRepository),
        observeLogoTransitionMode = ObserveLogoTransitionModeUseCase(onboardingRepository),
        observeDockEnabled = ObserveDockEnabledUseCase(dockSettingsRepository),
        observeDockSize = ObserveDockSizeUseCase(dockSettingsRepository),
        observeOverlayOpacity = ObserveOverlayOpacityUseCase(onboardingRepository),
        observeDockMaxApps = ObserveDockMaxAppsUseCase(dockSettingsRepository),
        observeDockApps = ObserveDockAppsUseCase(dockSettingsRepository),
        observeInstalledApps = ObserveInstalledAppsUseCase(installedAppsRepository),
    )

    // --- addWidget -------------------------------------------------------------------------

    @Test
    fun `addWidget is a no-op until grid dimensions are set`() = runTest(testDispatcher) {
        val viewModel = buildViewModel()
        advanceUntilIdle()

        viewModel.addWidget(WidgetType.ColorBackground(colorArgb = 0xFFFFFFFF, alpha = 1f))
        advanceUntilIdle()

        assertTrue(viewModel.widgets.value.isEmpty())
    }

    @Test
    fun `addWidget centers a new widget and assigns the next zIndex above existing widgets`() = runTest(testDispatcher) {
        val repository = FakeWidgetLayoutRepository().apply {
            seed(
                StateGroup.System,
                listOf(placedWidget(id = "existing-a", zIndex = 0), placedWidget(id = "existing-b", zIndex = 3)),
            )
        }
        val viewModel = buildViewModel(widgetLayoutRepository = repository)
        viewModel.setGridDimensions(grid)
        advanceUntilIdle()

        viewModel.addWidget(WidgetType.ColorBackground(colorArgb = 0xFF112233, alpha = 1f))
        advanceUntilIdle()

        val added = viewModel.widgets.value.first { it.id !in setOf("existing-a", "existing-b") }
        // DEFAULT_NEW_WIDGET_SPAN is 4x4, centered on a 10x10 grid -> column/row 3.
        assertEquals(4, added.columnSpan)
        assertEquals(4, added.rowSpan)
        assertEquals(3, added.gridColumn)
        assertEquals(3, added.gridRow)
        assertEquals(4, added.zIndex) // one above the existing max of 3
    }

    @Test
    fun `addWidget clamps span to a grid smaller than the default span`() = runTest(testDispatcher) {
        // Seeded explicitly (rather than left unsaved) so ObserveWidgetCanvasUseCase's
        // "empty saved canvas falls back to defaultCanvas" behavior doesn't kick in here -
        // that fallback is exactly what an unseeded repository would trigger, which would
        // add its own widgets and make the single-widget assertion below wrong.
        val repository = FakeWidgetLayoutRepository().apply {
            seed(StateGroup.System, listOf(placedWidget(id = "existing", zIndex = 0)))
        }
        val viewModel = buildViewModel(widgetLayoutRepository = repository)
        viewModel.setGridDimensions(GridDimensions(columns = 2, rows = 2))
        advanceUntilIdle()

        viewModel.addWidget(WidgetType.ColorBackground(colorArgb = 0xFF112233, alpha = 1f))
        advanceUntilIdle()

        val added = viewModel.widgets.value.first { it.id != "existing" }
        assertEquals(2, added.columnSpan)
        assertEquals(2, added.rowSpan)
        assertEquals(0, added.gridColumn)
        assertEquals(0, added.gridRow)
    }

    @Test
    fun `addWidget selects the new widget and persists it`() = runTest(testDispatcher) {
        // Same reasoning as the clamping test above - seed explicitly to avoid the
        // defaultCanvas fallback so the added widget is the only new thing on the canvas.
        val repository = FakeWidgetLayoutRepository().apply {
            seed(StateGroup.System, listOf(placedWidget(id = "existing", zIndex = 0)))
        }
        val viewModel = buildViewModel(widgetLayoutRepository = repository)
        viewModel.setGridDimensions(grid)
        advanceUntilIdle()

        viewModel.addWidget(WidgetType.ColorBackground(colorArgb = 0xFF112233, alpha = 1f))
        advanceUntilIdle()

        val added = viewModel.widgets.value.first { it.id != "existing" }
        assertEquals(added.id, viewModel.selectedWidgetId.value)
        assertEquals(StateGroup.System, repository.lastSaved?.first)
        assertEquals(setOf("existing", added.id), repository.lastSaved?.second?.map { it.id }?.toSet())
    }

    // --- moveUp / moveDown ------------------------------------------------------------------

    @Test
    fun `moveUp swaps zIndex with the next-highest widget by sorted position, not by incrementing`() = runTest(testDispatcher) {
        // Non-contiguous zIndex values (0, 5, 10) - a naive "zIndex + 1" swap would land
        // widget-a on 1, not on widget-b's actual value of 5.
        val repository = FakeWidgetLayoutRepository().apply {
            seed(
                StateGroup.System,
                listOf(
                    placedWidget(id = "widget-a", zIndex = 0),
                    placedWidget(id = "widget-b", zIndex = 5),
                    placedWidget(id = "widget-c", zIndex = 10),
                ),
            )
        }
        val viewModel = buildViewModel(widgetLayoutRepository = repository)
        viewModel.setGridDimensions(grid)
        advanceUntilIdle()

        viewModel.moveUp("widget-a")
        advanceUntilIdle()

        val byId = viewModel.widgets.value.associateBy { it.id }
        assertEquals(5, byId.getValue("widget-a").zIndex)
        assertEquals(0, byId.getValue("widget-b").zIndex)
        assertEquals(10, byId.getValue("widget-c").zIndex) // untouched
    }

    @Test
    fun `moveUp is a no-op for the topmost widget`() = runTest(testDispatcher) {
        val repository = FakeWidgetLayoutRepository().apply {
            seed(StateGroup.System, listOf(placedWidget(id = "widget-a", zIndex = 0), placedWidget(id = "widget-b", zIndex = 1)))
        }
        val viewModel = buildViewModel(widgetLayoutRepository = repository)
        viewModel.setGridDimensions(grid)
        advanceUntilIdle()

        viewModel.moveUp("widget-b")
        advanceUntilIdle()

        val byId = viewModel.widgets.value.associateBy { it.id }
        assertEquals(0, byId.getValue("widget-a").zIndex)
        assertEquals(1, byId.getValue("widget-b").zIndex)
    }

    @Test
    fun `moveDown is a no-op for the bottommost widget`() = runTest(testDispatcher) {
        val repository = FakeWidgetLayoutRepository().apply {
            seed(StateGroup.System, listOf(placedWidget(id = "widget-a", zIndex = 0), placedWidget(id = "widget-b", zIndex = 1)))
        }
        val viewModel = buildViewModel(widgetLayoutRepository = repository)
        viewModel.setGridDimensions(grid)
        advanceUntilIdle()

        viewModel.moveDown("widget-a")
        advanceUntilIdle()

        val byId = viewModel.widgets.value.associateBy { it.id }
        assertEquals(0, byId.getValue("widget-a").zIndex)
        assertEquals(1, byId.getValue("widget-b").zIndex)
    }

    // --- removeWidget -----------------------------------------------------------------------

    @Test
    fun `removeWidget removes the widget and clears selection if it was selected`() = runTest(testDispatcher) {
        val repository = FakeWidgetLayoutRepository().apply {
            seed(StateGroup.System, listOf(placedWidget(id = "widget-a", zIndex = 0)))
        }
        val viewModel = buildViewModel(widgetLayoutRepository = repository)
        viewModel.setGridDimensions(grid)
        advanceUntilIdle()
        viewModel.selectWidget("widget-a")

        viewModel.removeWidget("widget-a")
        advanceUntilIdle()

        assertTrue(viewModel.widgets.value.isEmpty())
        assertNull(viewModel.selectedWidgetId.value)
    }

    @Test
    fun `removeWidget leaves selection untouched when a different widget was selected`() = runTest(testDispatcher) {
        val repository = FakeWidgetLayoutRepository().apply {
            seed(
                StateGroup.System,
                listOf(placedWidget(id = "widget-a", zIndex = 0), placedWidget(id = "widget-b", zIndex = 1)),
            )
        }
        val viewModel = buildViewModel(widgetLayoutRepository = repository)
        viewModel.setGridDimensions(grid)
        advanceUntilIdle()
        viewModel.selectWidget("widget-b")

        viewModel.removeWidget("widget-a")
        advanceUntilIdle()

        assertEquals("widget-b", viewModel.selectedWidgetId.value)
    }

    // --- grid rescaling on load ---------------------------------------------------------------

    @Test
    fun `widgets saved under a different grid size are rescaled to fit the current grid`() = runTest(testDispatcher) {
        val repository = FakeWidgetLayoutRepository().apply {
            seed(
                StateGroup.System,
                listOf(placedWidget(id = "widget-a", gridColumn = 5, gridRow = 0, columnSpan = 5, rowSpan = 10, zIndex = 0)),
                grid = GridDimensions(columns = 10, rows = 10),
            )
        }
        val viewModel = buildViewModel(widgetLayoutRepository = repository)

        // Loaded on a grid twice as wide as the one it was saved under (e.g. moving the
        // app to a wider secondary display) - the widget should keep occupying the right
        // half of the canvas, not stay pinned at its original absolute columns 5-9.
        viewModel.setGridDimensions(GridDimensions(columns = 20, rows = 10))
        advanceUntilIdle()

        val widget = viewModel.widgets.value.single()
        assertEquals(10, widget.gridColumn)
        assertEquals(10, widget.columnSpan)
        assertEquals(0, widget.gridRow)
        assertEquals(10, widget.rowSpan)
    }

    @Test
    fun `widgets saved without a tagged grid (pre-rescaling data) load unchanged`() = runTest(testDispatcher) {
        val repository = FakeWidgetLayoutRepository().apply {
            seed(
                StateGroup.System,
                listOf(placedWidget(id = "widget-a", gridColumn = 5, gridRow = 0, columnSpan = 5, rowSpan = 10, zIndex = 0)),
                // grid omitted - defaults to null, same as data persisted before this feature existed.
            )
        }
        val viewModel = buildViewModel(widgetLayoutRepository = repository)

        viewModel.setGridDimensions(GridDimensions(columns = 20, rows = 10))
        advanceUntilIdle()

        val widget = viewModel.widgets.value.single()
        assertEquals(5, widget.gridColumn)
        assertEquals(5, widget.columnSpan)
    }

    @Test
    fun `persistWidgets saves the grid dimensions alongside the widgets`() = runTest(testDispatcher) {
        val repository = FakeWidgetLayoutRepository()
        val viewModel = buildViewModel(widgetLayoutRepository = repository)
        viewModel.setGridDimensions(grid)
        advanceUntilIdle()

        viewModel.addWidget(WidgetType.ColorBackground(colorArgb = 0xFF112233, alpha = 1f))
        advanceUntilIdle()

        assertEquals(grid, repository.observeCanvas(StateGroup.System).first().grid)
    }

    // --- selectCanvas -----------------------------------------------------------------------

    @Test
    fun `selectCanvas switches canvases and clears the current selection`() = runTest(testDispatcher) {
        val repository = FakeWidgetLayoutRepository().apply {
            seed(StateGroup.System, listOf(placedWidget(id = "system-widget", zIndex = 0)))
            seed(StateGroup.Playing, listOf(placedWidget(id = "playing-widget", zIndex = 0)))
        }
        val viewModel = buildViewModel(widgetLayoutRepository = repository)
        viewModel.setGridDimensions(grid)
        advanceUntilIdle()
        viewModel.selectWidget("system-widget")

        viewModel.selectCanvas(StateGroup.Playing)
        advanceUntilIdle()

        assertEquals(StateGroup.Playing, viewModel.selectedCanvas.value)
        assertNull(viewModel.selectedWidgetId.value)
        assertEquals(listOf("playing-widget"), viewModel.widgets.value.map { it.id })
    }

    // --- live drag/resize preview -----------------------------------------------------------

    @Test
    fun `updateWidgetPosition and updateWidgetBounds update in place without touching previewContent`() = runTest(testDispatcher) {
        val repository = FakeWidgetLayoutRepository().apply {
            seed(StateGroup.System, listOf(placedWidget(id = "widget-a", zIndex = 0, gridColumn = 1, gridRow = 1)))
        }
        val viewModel = buildViewModel(widgetLayoutRepository = repository)
        viewModel.setGridDimensions(grid)
        advanceUntilIdle()
        val previewBefore = viewModel.previewContent.value

        viewModel.updateWidgetPosition("widget-a", gridColumn = 4, gridRow = 5)
        viewModel.updateWidgetBounds("widget-a", gridColumn = 4, gridRow = 5, columnSpan = 3, rowSpan = 3)
        advanceUntilIdle()

        val updated = viewModel.widgets.value.single()
        assertEquals(4, updated.gridColumn)
        assertEquals(5, updated.gridRow)
        assertEquals(3, updated.columnSpan)
        assertEquals(3, updated.rowSpan)
        assertEquals(previewBefore, viewModel.previewContent.value) // unchanged - no recompute on drag/resize
        assertNull(repository.lastSaved) // neither function persists - see persistWidgets() kdoc
    }

    // --- previewContent resolution ----------------------------------------------------------

    @Test
    fun `previewContent resolves to Empty when there is no last-known context`() = runTest(testDispatcher) {
        val repository = FakeWidgetLayoutRepository().apply {
            seed(
                StateGroup.System,
                listOf(placedWidget(id = "widget-a", zIndex = 0, widgetType = WidgetType.SystemLogo(ScaleMode.Fit))),
            )
        }
        val viewModel = buildViewModel(widgetLayoutRepository = repository)
        viewModel.setGridDimensions(grid)
        advanceUntilIdle()

        assertEquals(WidgetContent.Empty, viewModel.previewContent.value["widget-a"])
    }

    @Test
    fun `previewContent resolves real game media from the last-known game reference`() = runTest(testDispatcher) {
        val gameReference = GameReference(systemShortName = "snes", romPath = "/roms/snes/game.sfc")
        val gameMedia = GameMedia(
            baseRelativePath = "game",
            filesByType = mapOf(MediaType.FanArt to "/media/snes/fanart/game.png"),
        )
        val repository = FakeWidgetLayoutRepository().apply {
            seed(
                StateGroup.Playing,
                listOf(
                    placedWidget(
                        id = "widget-a",
                        zIndex = 0,
                        widgetType = WidgetType.GameMedia(MediaType.FanArt, ScaleMode.Fill),
                    ),
                ),
            )
        }
        val viewModel = buildViewModel(
            widgetLayoutRepository = repository,
            gameMediaRepository = FakeGameMediaRepository(gameMedia),
            lastKnownContextRepository = FakeLastKnownContextRepository(initialGameReference = gameReference),
        )
        viewModel.selectCanvas(StateGroup.Playing)
        viewModel.setGridDimensions(grid)
        advanceUntilIdle()

        val content = viewModel.previewContent.value["widget-a"]
        assertTrue(content is WidgetContent.Image)
        assertEquals("/media/snes/fanart/game.png", (content as WidgetContent.Image).path)
    }

    @Test
    fun `previewContent reuses the same random system media pick across repeated resolutions in one edit session`() = runTest(testDispatcher) {
        val repository = FakeWidgetLayoutRepository().apply {
            seed(
                StateGroup.System,
                listOf(placedWidget(id = "widget-a", zIndex = 0, widgetType = WidgetType.SystemMedia(MediaType.FanArt, ScaleMode.Fill))),
            )
        }
        val systemMediaRepository = CountingSystemMediaRepository()
        val viewModel = buildViewModel(
            widgetLayoutRepository = repository,
            systemMediaRepository = systemMediaRepository,
            lastKnownContextRepository = FakeLastKnownContextRepository(initialSystemShortName = "snes"),
        )
        viewModel.setGridDimensions(grid)
        advanceUntilIdle()
        val firstPick = (viewModel.previewContent.value["widget-a"] as WidgetContent.Image).path

        // A Configure Widget tweak recomputes previewContent (see updateWidgetConfig's
        // kdoc) but must not reroll which random image was already chosen this session.
        viewModel.updateWidgetConfig("widget-a", WidgetType.SystemMedia(MediaType.FanArt, ScaleMode.Fit))
        advanceUntilIdle()
        val secondPick = (viewModel.previewContent.value["widget-a"] as WidgetContent.Image).path

        assertEquals(firstPick, secondPick)
        assertEquals(1, systemMediaRepository.callCount)
    }
}
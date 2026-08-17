package com.esde.companion.ui.widgets

import com.esde.companion.domain.model.EsdeEvent
import com.esde.companion.domain.model.GameDescription
import com.esde.companion.domain.model.GameMedia
import com.esde.companion.domain.model.GridDimensions
import com.esde.companion.domain.model.ImageEffects
import com.esde.companion.domain.model.MediaType
import com.esde.companion.domain.model.PlacedWidget
import com.esde.companion.domain.model.SavedWidgetCanvas
import com.esde.companion.domain.model.ScaleMode
import com.esde.companion.domain.model.StateGroup
import com.esde.companion.domain.model.WidgetContent
import com.esde.companion.domain.model.WidgetType
import com.esde.companion.domain.repository.BundledSystemLogoRepository
import com.esde.companion.domain.repository.CustomSystemImageRepository
import com.esde.companion.domain.repository.CustomSystemLogoRepository
import com.esde.companion.domain.repository.EsdeLogRepository
import com.esde.companion.domain.repository.GameDescriptionRepository
import com.esde.companion.domain.repository.GameMediaRepository
import com.esde.companion.domain.repository.SystemMediaRepository
import com.esde.companion.domain.repository.WidgetLayoutRepository
import com.esde.companion.domain.usecase.ObserveAppStateUseCase
import com.esde.companion.domain.usecase.ObserveConnectionStateUseCase
import com.esde.companion.domain.usecase.ObserveWidgetCanvasUseCase
import com.esde.companion.domain.usecase.ResolveBundledSystemLogoUseCase
import com.esde.companion.domain.usecase.ResolveCustomSystemImageUseCase
import com.esde.companion.domain.usecase.ResolveCustomSystemLogoUseCase
import com.esde.companion.domain.usecase.ResolveGameDescriptionUseCase
import com.esde.companion.domain.usecase.ResolveGameMediaUseCase
import com.esde.companion.domain.usecase.ResolveRandomSystemMediaUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WidgetsViewModelTest {
    // --- Fakes -------------------------------------------------------------------------

    private class FakeEsdeLogRepository(
        private val fileExists: Flow<Boolean> = flowOf(true),
    ) : EsdeLogRepository {
        val events = MutableSharedFlow<EsdeEvent>()

        override fun observeEvents(): Flow<EsdeEvent> = events

        override fun observeLogFileExists(): Flow<Boolean> = fileExists
    }

    private class FakeWidgetLayoutRepository : WidgetLayoutRepository {
        private val canvases = mutableMapOf<StateGroup, MutableStateFlow<SavedWidgetCanvas>>()

        fun seed(
            stateGroup: StateGroup,
            widgets: List<PlacedWidget>,
            grid: GridDimensions? = null,
        ) {
            flowFor(stateGroup).value = SavedWidgetCanvas(grid, widgets)
        }

        private fun flowFor(stateGroup: StateGroup) =
            canvases.getOrPut(stateGroup) { MutableStateFlow(SavedWidgetCanvas(grid = null, widgets = emptyList())) }

        override fun observeCanvas(stateGroup: StateGroup): Flow<SavedWidgetCanvas> = flowFor(stateGroup)

        override suspend fun saveCanvas(
            stateGroup: StateGroup,
            widgets: List<PlacedWidget>,
            grid: GridDimensions,
        ) {
            flowFor(stateGroup).value = SavedWidgetCanvas(grid, widgets)
        }
    }

    private class RecordingGameMediaRepository(
        private val media: GameMedia = GameMedia(baseRelativePath = null, filesByType = emptyMap()),
    ) : GameMediaRepository {
        var callCount = 0
            private set
        var lastRequestedTypes: Set<MediaType>? = null
            private set

        override suspend fun resolveMedia(
            systemShortName: String,
            systemPath: String?,
            romPath: String,
            mediaTypes: Set<MediaType>,
        ): GameMedia {
            callCount++
            lastRequestedTypes = mediaTypes
            return media
        }
    }

    private class RecordingGameDescriptionRepository(
        private val description: GameDescription = GameDescription(text = null),
    ) : GameDescriptionRepository {
        var callCount = 0
            private set

        override suspend fun resolveDescription(
            systemShortName: String,
            romPath: String,
        ): GameDescription {
            callCount++
            return description
        }
    }

    /** Returns a distinct value every call - used to prove the ViewModel's own
     * per-system cache (not the repository's) is what keeps repeated resolutions for the
     * same system stable, per WidgetsViewModel's documented "A -> B -> A" rule. */
    private class CountingSystemMediaRepository : SystemMediaRepository {
        var callCount = 0
            private set
        var lastRequestedTypes = mutableListOf<MediaType>()
            private set

        override suspend fun randomMedia(
            systemShortName: String,
            mediaType: MediaType,
        ): String? {
            callCount++
            lastRequestedTypes += mediaType
            return "/media/$systemShortName/$mediaType/pick-$callCount.png"
        }
    }

    private class RecordingCustomSystemImageRepository : CustomSystemImageRepository {
        var callCount = 0
            private set

        override suspend fun findImage(systemShortName: String): String? {
            callCount++
            return null
        }
    }

    private class RecordingCustomSystemLogoRepository : CustomSystemLogoRepository {
        var callCount = 0
            private set

        override suspend fun findLogo(systemShortName: String): String? {
            callCount++
            return null
        }
    }

    private class FakeBundledSystemLogoRepository : BundledSystemLogoRepository {
        override suspend fun findLogoAssetPath(assetName: String): String? = null
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
        widgetType: WidgetType = WidgetType.ColorBackground(colorArgb = 0xFF000000, alpha = 1f),
    ) = PlacedWidget(
        id = id,
        widgetType = widgetType,
        gridColumn = 0,
        gridRow = 0,
        columnSpan = 2,
        rowSpan = 2,
        zIndex = 0,
    )

    @Suppress("LongParameterList")
    private fun TestScope.buildViewModel(
        esdeLogRepository: FakeEsdeLogRepository = FakeEsdeLogRepository(),
        widgetLayoutRepository: FakeWidgetLayoutRepository = FakeWidgetLayoutRepository(),
        gameMediaRepository: GameMediaRepository = RecordingGameMediaRepository(),
        gameDescriptionRepository: GameDescriptionRepository = RecordingGameDescriptionRepository(),
        systemMediaRepository: SystemMediaRepository = CountingSystemMediaRepository(),
        customSystemImageRepository: CustomSystemImageRepository = RecordingCustomSystemImageRepository(),
        customSystemLogoRepository: CustomSystemLogoRepository = RecordingCustomSystemLogoRepository(),
        bundledSystemLogoRepository: BundledSystemLogoRepository = FakeBundledSystemLogoRepository(),
    ): WidgetsViewModel {
        val observeAppState = ObserveAppStateUseCase(esdeLogRepository, backgroundScope)
        val observeConnectionState = ObserveConnectionStateUseCase(esdeLogRepository, observeAppState)
        val viewModel =
            WidgetsViewModel(
                observeConnectionState = observeConnectionState,
                observeWidgetCanvas = ObserveWidgetCanvasUseCase(widgetLayoutRepository),
                resolveGameMedia = ResolveGameMediaUseCase(gameMediaRepository),
                resolveRandomSystemMedia = ResolveRandomSystemMediaUseCase(systemMediaRepository),
                resolveGameDescription = ResolveGameDescriptionUseCase(gameDescriptionRepository),
                resolveCustomSystemImage = ResolveCustomSystemImageUseCase(customSystemImageRepository),
                resolveCustomSystemLogo = ResolveCustomSystemLogoUseCase(customSystemLogoRepository),
                resolveBundledSystemLogo = ResolveBundledSystemLogoUseCase(bundledSystemLogoRepository),
            )
        // canvasState is WhileSubscribed - simulates the always-on UI collector
        // (MainScreen's collectAsState()) so reading .value in tests reflects live updates,
        // the same way EditWidgetsViewModelTest's manually-pushed StateFlows already do.
        backgroundScope.launch { viewModel.canvasState.collect {} }
        // Lets ObserveAppStateUseCase's Eagerly fold (and the collector above) actually
        // start collecting before any test emits an event - otherwise, with
        // StandardTestDispatcher, those background launches are merely queued rather than
        // running, and an emit() to a replay=0 MutableSharedFlow with no active collector
        // yet is silently dropped rather than buffered.
        advanceUntilIdle()
        return viewModel
    }

    private fun systemSelect(systemShortName: String) =
        EsdeEvent.SystemSelect(systemShortName, "$systemShortName full name", "/roms/$systemShortName")

    // --- canvasState basic states -----------------------------------------------------------

    @Test
    fun `canvasState starts Unmeasured before setGridDimensions is called`() =
        runTest(testDispatcher) {
            val viewModel = buildViewModel()

            assertEquals(WidgetCanvasState.Unmeasured, viewModel.canvasState.value)
        }

    @Test
    fun `canvasState is Disconnected once measured while AppState is Idle`() =
        runTest(testDispatcher) {
            val viewModel = buildViewModel()

            viewModel.setGridDimensions(grid)
            advanceUntilIdle()

            assertEquals(WidgetCanvasState.Disconnected, viewModel.canvasState.value)
        }

    @Test
    fun `BrowsingSystem resolves Showing with the seeded widgets and resolved content`() =
        runTest(testDispatcher) {
            val esdeLogRepository = FakeEsdeLogRepository()
            val widgetLayoutRepository = FakeWidgetLayoutRepository()
            val widget = placedWidget("bg", WidgetType.ColorBackground(colorArgb = 0xFF112233, alpha = 0.5f))
            widgetLayoutRepository.seed(StateGroup.System, listOf(widget))
            val viewModel = buildViewModel(esdeLogRepository, widgetLayoutRepository)

            viewModel.setGridDimensions(grid)
            esdeLogRepository.events.emit(systemSelect("snes"))
            advanceUntilIdle()

            val state = viewModel.canvasState.value
            check(state is WidgetCanvasState.Showing)
            assertEquals(listOf(widget), state.widgets)
            assertEquals(WidgetContent.Color(0xFF112233, 0.5f), state.contentByWidgetId["bg"])
        }

    // --- contentIdentity distinctUntilChanged -------------------------------------------

    @Test
    fun `a repeated identical event does not re-trigger content resolution`() =
        runTest(testDispatcher) {
            val esdeLogRepository = FakeEsdeLogRepository()
            val widgetLayoutRepository = FakeWidgetLayoutRepository()
            widgetLayoutRepository.seed(
                StateGroup.Playing,
                listOf(placedWidget("desc", WidgetType.GameDescription())),
            )
            val gameDescriptionRepository = RecordingGameDescriptionRepository()
            val viewModel =
                buildViewModel(
                    esdeLogRepository,
                    widgetLayoutRepository,
                    gameDescriptionRepository = gameDescriptionRepository,
                )
            viewModel.setGridDimensions(grid)

            val gameSelect = EsdeEvent.GameSelect("/roms/snes/game.sfc", "Game", "snes", "SNES")
            esdeLogRepository.events.emit(gameSelect)
            advanceUntilIdle()
            esdeLogRepository.events.emit(gameSelect)
            advanceUntilIdle()

            assertEquals(1, gameDescriptionRepository.callCount)
        }

    // --- random system-media cache ------------------------------------------------------

    @Test
    fun `resolving the same system twice reuses the cached random media pick`() =
        runTest(testDispatcher) {
            val esdeLogRepository = FakeEsdeLogRepository()
            val widgetLayoutRepository = FakeWidgetLayoutRepository()
            val widget = placedWidget("fanart", WidgetType.SystemMedia(MediaType.FanArt, ScaleMode.Fill))
            widgetLayoutRepository.seed(StateGroup.System, listOf(widget))
            val systemMediaRepository = CountingSystemMediaRepository()
            val viewModel =
                buildViewModel(
                    esdeLogRepository,
                    widgetLayoutRepository,
                    systemMediaRepository = systemMediaRepository,
                )
            viewModel.setGridDimensions(grid)

            esdeLogRepository.events.emit(systemSelect("snes"))
            advanceUntilIdle()
            assertEquals(1, systemMediaRepository.callCount)

            // Re-emitting the same canvas (no AppState/system change) still re-runs
            // resolveContent - the ViewModel's own cache, not the upstream distinctUntilChanged,
            // is what must keep this from re-rolling.
            widgetLayoutRepository.seed(StateGroup.System, listOf(widget))
            advanceUntilIdle()

            assertEquals(1, systemMediaRepository.callCount)
        }

    @Test
    fun `switching to a different system produces a fresh pick, and returning to the first system later also does`() =
        runTest(testDispatcher) {
            val esdeLogRepository = FakeEsdeLogRepository()
            val widgetLayoutRepository = FakeWidgetLayoutRepository()
            val widget = placedWidget("fanart", WidgetType.SystemMedia(MediaType.FanArt, ScaleMode.Fill))
            widgetLayoutRepository.seed(StateGroup.System, listOf(widget))
            val systemMediaRepository = CountingSystemMediaRepository()
            val viewModel =
                buildViewModel(
                    esdeLogRepository,
                    widgetLayoutRepository,
                    systemMediaRepository = systemMediaRepository,
                )
            viewModel.setGridDimensions(grid)

            esdeLogRepository.events.emit(systemSelect("snes"))
            advanceUntilIdle()
            assertEquals(1, systemMediaRepository.callCount)

            esdeLogRepository.events.emit(systemSelect("genesis"))
            advanceUntilIdle()
            assertEquals(2, systemMediaRepository.callCount)

            esdeLogRepository.events.emit(systemSelect("snes"))
            advanceUntilIdle()
            assertEquals(3, systemMediaRepository.callCount)
        }

    // --- selective media fetching in resolveContent ---------------------------------------

    @Test
    fun `a ColorBackground-only canvas requests no specific game or system media types`() =
        runTest(testDispatcher) {
            // A gameRef being current still triggers a resolveGameMedia call (it always does
            // once a game is current, regardless of what widgets are on the canvas) - what
            // this pins down is that it's asked for zero media types, and that system media
            // (gated on the canvas's actual widget types) isn't touched at all.
            val esdeLogRepository = FakeEsdeLogRepository()
            val widgetLayoutRepository = FakeWidgetLayoutRepository()
            widgetLayoutRepository.seed(StateGroup.Playing, listOf(placedWidget("bg")))
            val gameMediaRepository = RecordingGameMediaRepository()
            val systemMediaRepository = CountingSystemMediaRepository()
            val viewModel =
                buildViewModel(
                    esdeLogRepository,
                    widgetLayoutRepository,
                    gameMediaRepository = gameMediaRepository,
                    systemMediaRepository = systemMediaRepository,
                )
            viewModel.setGridDimensions(grid)

            esdeLogRepository.events.emit(EsdeEvent.GameSelect("/roms/snes/game.sfc", "Game", "snes", "SNES"))
            advanceUntilIdle()

            assertEquals(1, gameMediaRepository.callCount)
            assertEquals(emptySet<MediaType>(), gameMediaRepository.lastRequestedTypes)
            assertEquals(0, systemMediaRepository.callCount)
        }

    @Test
    fun `a SystemImage widget requests FanArt and Screenshots system media even with no SystemMedia widget`() =
        runTest(testDispatcher) {
            val esdeLogRepository = FakeEsdeLogRepository()
            val widgetLayoutRepository = FakeWidgetLayoutRepository()
            val widget =
                placedWidget(
                    "image",
                    WidgetType.SystemImage(ScaleMode.Fill, ImageEffects(), panZoomEnabled = false),
                )
            widgetLayoutRepository.seed(StateGroup.System, listOf(widget))
            val systemMediaRepository = CountingSystemMediaRepository()
            val viewModel =
                buildViewModel(
                    esdeLogRepository,
                    widgetLayoutRepository,
                    systemMediaRepository = systemMediaRepository,
                )
            viewModel.setGridDimensions(grid)

            esdeLogRepository.events.emit(systemSelect("snes"))
            advanceUntilIdle()

            val expectedTypes = setOf(MediaType.FanArt, MediaType.Screenshots)
            assertEquals(expectedTypes, systemMediaRepository.lastRequestedTypes.toSet())
        }

    // --- game media/description gated on gameRef presence ---------------------------------

    @Test
    fun `a BrowsingSystem canvas with no current game never resolves game media or description`() =
        runTest(testDispatcher) {
            val esdeLogRepository = FakeEsdeLogRepository()
            val widgetLayoutRepository = FakeWidgetLayoutRepository()
            widgetLayoutRepository.seed(
                StateGroup.System,
                listOf(placedWidget("desc", WidgetType.GameDescription())),
            )
            val gameMediaRepository = RecordingGameMediaRepository()
            val gameDescriptionRepository = RecordingGameDescriptionRepository()
            val viewModel =
                buildViewModel(
                    esdeLogRepository,
                    widgetLayoutRepository,
                    gameMediaRepository = gameMediaRepository,
                    gameDescriptionRepository = gameDescriptionRepository,
                )
            viewModel.setGridDimensions(grid)

            esdeLogRepository.events.emit(systemSelect("snes"))
            advanceUntilIdle()

            assertEquals(0, gameMediaRepository.callCount)
            assertEquals(0, gameDescriptionRepository.callCount)
        }

    // --- custom logo/image gated on matching widget presence -------------------------------

    @Test
    fun `custom system logo is only resolved when a SystemLogo widget is on the canvas`() =
        runTest(testDispatcher) {
            val esdeLogRepository = FakeEsdeLogRepository()
            val widgetLayoutRepository = FakeWidgetLayoutRepository()
            widgetLayoutRepository.seed(StateGroup.System, listOf(placedWidget("bg")))
            val customSystemLogoRepository = RecordingCustomSystemLogoRepository()
            val viewModel =
                buildViewModel(
                    esdeLogRepository,
                    widgetLayoutRepository,
                    customSystemLogoRepository = customSystemLogoRepository,
                )
            viewModel.setGridDimensions(grid)

            esdeLogRepository.events.emit(systemSelect("snes"))
            advanceUntilIdle()

            assertEquals(0, customSystemLogoRepository.callCount)
        }

    @Test
    fun `custom system image is resolved when a SystemImage widget is on the canvas`() =
        runTest(testDispatcher) {
            val esdeLogRepository = FakeEsdeLogRepository()
            val widgetLayoutRepository = FakeWidgetLayoutRepository()
            val widget =
                placedWidget("image", WidgetType.SystemImage(ScaleMode.Fill, ImageEffects(), panZoomEnabled = false))
            widgetLayoutRepository.seed(StateGroup.System, listOf(widget))
            val customSystemImageRepository = RecordingCustomSystemImageRepository()
            val viewModel =
                buildViewModel(
                    esdeLogRepository,
                    widgetLayoutRepository,
                    customSystemImageRepository = customSystemImageRepository,
                )
            viewModel.setGridDimensions(grid)

            esdeLogRepository.events.emit(systemSelect("snes"))
            advanceUntilIdle()

            assertTrue(customSystemImageRepository.callCount > 0)
        }

    // --- setGridDimensions re-threading ------------------------------------------------

    @Test
    fun `changing the grid re-resolves the canvas against the new grid`() =
        runTest(testDispatcher) {
            val esdeLogRepository = FakeEsdeLogRepository()
            val widgetLayoutRepository = FakeWidgetLayoutRepository()
            val savedGrid = GridDimensions(columns = 10, rows = 10)
            val widget =
                placedWidget("bg", WidgetType.ColorBackground(colorArgb = 0xFF000000, alpha = 1f))
                    .copy(gridColumn = 5, gridRow = 0, columnSpan = 5, rowSpan = 10)
            widgetLayoutRepository.seed(StateGroup.System, listOf(widget), grid = savedGrid)
            val viewModel = buildViewModel(esdeLogRepository, widgetLayoutRepository)

            viewModel.setGridDimensions(savedGrid)
            esdeLogRepository.events.emit(systemSelect("snes"))
            advanceUntilIdle()
            val unscaled = viewModel.canvasState.value
            check(unscaled is WidgetCanvasState.Showing)
            assertEquals(5, unscaled.widgets.single().gridColumn)

            viewModel.setGridDimensions(GridDimensions(columns = 20, rows = 10))
            advanceUntilIdle()

            val rescaled = viewModel.canvasState.value
            check(rescaled is WidgetCanvasState.Showing)
            val rescaledWidget = rescaled.widgets.single()
            assertEquals(10, rescaledWidget.gridColumn)
            assertEquals(10, rescaledWidget.columnSpan)
        }
}

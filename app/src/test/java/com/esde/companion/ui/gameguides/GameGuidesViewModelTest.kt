package com.esde.companion.ui.gameguides

import com.esde.companion.domain.model.DownloadedGameGuide
import com.esde.companion.domain.model.EsdeEvent
import com.esde.companion.domain.model.GameGuideDisplayPreferences
import com.esde.companion.domain.model.GameGuideFormat
import com.esde.companion.domain.model.GameGuideReadingProgress
import com.esde.companion.domain.model.GameMedia
import com.esde.companion.domain.model.GameReference
import com.esde.companion.domain.model.MediaType
import com.esde.companion.domain.repository.EsdeLogRepository
import com.esde.companion.domain.repository.GameGuideLibraryRepository
import com.esde.companion.domain.repository.GameGuideSettingsRepository
import com.esde.companion.domain.repository.GameMediaRepository
import com.esde.companion.domain.usecase.DeleteGameGuideUseCase
import com.esde.companion.domain.usecase.ImportGameGuideUseCase
import com.esde.companion.domain.usecase.LoadGameGuideBinaryPathUseCase
import com.esde.companion.domain.usecase.LoadGameGuideContentUseCase
import com.esde.companion.domain.usecase.LoadGameGuidePageUseCase
import com.esde.companion.domain.usecase.ObserveAppStateUseCase
import com.esde.companion.domain.usecase.ObserveConnectionStateUseCase
import com.esde.companion.domain.usecase.ObserveGameGuideDisplayPreferencesUseCase
import com.esde.companion.domain.usecase.ObserveGameGuideReadingProgressUseCase
import com.esde.companion.domain.usecase.ObserveGameGuidesUseCase
import com.esde.companion.domain.usecase.ResolveGameMediaUseCase
import com.esde.companion.domain.usecase.SaveGameGuideUseCase
import com.esde.companion.domain.usecase.SetGameGuideDisplayPreferencesUseCase
import com.esde.companion.domain.usecase.SetGameGuideReadingProgressUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GameGuidesViewModelTest {
    // --- Fakes -------------------------------------------------------------------------

    private class FakeEsdeLogRepository : EsdeLogRepository {
        val events = MutableSharedFlow<EsdeEvent>()

        override fun observeEvents(): Flow<EsdeEvent> = events

        override fun observeLogFileExists(): Flow<Boolean> = flowOf(true)
    }

    private class FakeGameGuideLibraryRepository : GameGuideLibraryRepository {
        private val guidesByGame = mutableMapOf<GameReference, MutableStateFlow<List<DownloadedGameGuide>>>()
        private val contentByGuideId = mutableMapOf<String, List<String>>()
        private val binaryPathByGuideId = mutableMapOf<String, String>()
        val deletedGuideIds = mutableListOf<String>()
        val loadPageCalls = mutableListOf<Pair<String, Int>>()

        fun seedGuide(
            guide: DownloadedGameGuide,
            content: List<String>,
        ) {
            contentByGuideId[guide.id] = content
            flowFor(guide.gameReference).value += guide
        }

        override suspend fun saveGuide(
            guide: DownloadedGameGuide,
            content: List<String>,
        ): Result<Unit> {
            contentByGuideId[guide.id] = content
            flowFor(guide.gameReference).value += guide
            return Result.success(Unit)
        }

        override suspend fun saveImportedGuide(
            guide: DownloadedGameGuide,
            contentBytes: ByteArray,
            fileExtension: String,
        ): Result<Unit> {
            binaryPathByGuideId[guide.id] = "/fake/guides/${guide.id}/content.$fileExtension"
            flowFor(guide.gameReference).value += guide
            return Result.success(Unit)
        }

        override fun observeGuidesFor(gameReference: GameReference): Flow<List<DownloadedGameGuide>> {
            return flowFor(gameReference)
        }

        override fun observeAllGuides(): Flow<List<DownloadedGameGuide>> {
            return flowOf(guidesByGame.values.flatMap { it.value })
        }

        override suspend fun loadContent(guideId: String): List<String>? = contentByGuideId[guideId]

        override suspend fun loadPage(
            guideId: String,
            pageIndex: Int,
        ): String? {
            loadPageCalls += guideId to pageIndex
            return contentByGuideId[guideId]?.getOrNull(pageIndex)
        }

        override suspend fun binaryContentPath(guideId: String): String? = binaryPathByGuideId[guideId]

        override suspend fun deleteGuide(guideId: String) {
            deletedGuideIds += guideId
            guidesByGame.values.forEach { flow -> flow.value = flow.value.filterNot { it.id == guideId } }
        }

        override suspend fun deleteAllGuides() {
            guidesByGame.values.forEach { it.value = emptyList() }
        }

        private fun flowFor(gameReference: GameReference): MutableStateFlow<List<DownloadedGameGuide>> {
            return guidesByGame.getOrPut(gameReference) { MutableStateFlow(emptyList()) }
        }
    }

    private class FakeGameGuideSettingsRepository : GameGuideSettingsRepository {
        private val displayPreferencesFlow = MutableStateFlow(GameGuideDisplayPreferences())
        private val progressByGuideId = mutableMapOf<String, GameGuideReadingProgress>()
        private val manualFallbackFlow = MutableStateFlow(false)

        override suspend fun setDisplayPreferences(preferences: GameGuideDisplayPreferences) {
            displayPreferencesFlow.value = preferences
        }

        override fun observeDisplayPreferences(): Flow<GameGuideDisplayPreferences> = displayPreferencesFlow

        override suspend fun setReadingProgress(progress: GameGuideReadingProgress) {
            progressByGuideId[progress.guideId] = progress
        }

        override fun observeReadingProgress(guideId: String): Flow<GameGuideReadingProgress?> {
            return flowOf(progressByGuideId[guideId])
        }

        override suspend fun setManualFallbackOnNoGuideEnabled(enabled: Boolean) {
            manualFallbackFlow.value = enabled
        }

        override fun observeManualFallbackOnNoGuideEnabled(): Flow<Boolean> = manualFallbackFlow
    }

    /** Only ever resolves [MediaType.Manuals] for [manualPathByReference]'s seeded game, if
     * any - null (no manual) for anything else, same "expected, routine outcome" shape the
     * real repository documents. */
    private class FakeGameMediaRepository(
        private val manualPathByReference: Map<GameReference, String> = emptyMap(),
    ) : GameMediaRepository {
        override suspend fun resolveMedia(
            systemShortName: String,
            systemPath: String?,
            romPath: String,
            mediaTypes: Set<MediaType>,
        ): GameMedia {
            val reference = GameReference(systemShortName, romPath, systemPath)
            val manualPath = manualPathByReference[reference]
            val filesByType = if (manualPath != null) mapOf(MediaType.Manuals to manualPath) else emptyMap()
            return GameMedia(baseRelativePath = null, filesByType = filesByType)
        }
    }

    // --- Setup ---------------------------------------------------------------------------

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.buildViewModel(
        esdeLogRepository: FakeEsdeLogRepository = FakeEsdeLogRepository(),
        libraryRepository: FakeGameGuideLibraryRepository = FakeGameGuideLibraryRepository(),
        settingsRepository: FakeGameGuideSettingsRepository = FakeGameGuideSettingsRepository(),
        gameMediaRepository: FakeGameMediaRepository = FakeGameMediaRepository(),
    ): GameGuidesViewModel {
        val observeAppState = ObserveAppStateUseCase(esdeLogRepository, backgroundScope)
        val observeConnectionState = ObserveConnectionStateUseCase(esdeLogRepository, observeAppState)
        val useCases =
            GameGuidesUseCases(
                observeGameGuides = ObserveGameGuidesUseCase(libraryRepository),
                saveGameGuide = SaveGameGuideUseCase(libraryRepository),
                importGameGuide = ImportGameGuideUseCase(libraryRepository),
                loadGameGuideContent = LoadGameGuideContentUseCase(libraryRepository),
                loadGameGuidePage = LoadGameGuidePageUseCase(libraryRepository),
                loadGameGuideBinaryPath = LoadGameGuideBinaryPathUseCase(libraryRepository),
                deleteGameGuide = DeleteGameGuideUseCase(libraryRepository),
                observeDisplayPreferences = ObserveGameGuideDisplayPreferencesUseCase(settingsRepository),
                setDisplayPreferences = SetGameGuideDisplayPreferencesUseCase(settingsRepository),
                observeReadingProgress = ObserveGameGuideReadingProgressUseCase(settingsRepository),
                setReadingProgress = SetGameGuideReadingProgressUseCase(settingsRepository),
                resolveGameMedia = ResolveGameMediaUseCase(gameMediaRepository),
            )
        val viewModel = GameGuidesViewModel(observeConnectionState, useCases)
        advanceUntilIdle()
        return viewModel
    }

    private fun downloadedGuide(
        reference: GameReference,
        id: String,
        pageCount: Int = 1,
    ) = DownloadedGameGuide(
        id = id,
        gameReference = reference,
        title = "Guide $id",
        sourceUrl = "https://gamefaqs.gamespot.com/guide/$id",
        format = GameGuideFormat.PlainText,
        pageCount = pageCount,
        sizeBytes = 0L,
        downloadedAtMillis = 0L,
    )

    // --- hasCurrentGame --------------------------------------------------------------------

    @Test
    fun `hasCurrentGame follows the same AppState cases as currentGameReference`() =
        runTest(testDispatcher) {
            val esdeLogRepository = FakeEsdeLogRepository()
            val viewModel = buildViewModel(esdeLogRepository)

            assertFalse(viewModel.hasCurrentGame.value)

            esdeLogRepository.events.emit(EsdeEvent.GameStart("/roms/snes/a.sfc", "A", "snes", "SNES"))
            advanceUntilIdle()
            assertTrue(viewModel.hasCurrentGame.value)

            esdeLogRepository.events.emit(EsdeEvent.GameEnd("/roms/snes/a.sfc", "A", "snes", "SNES"))
            esdeLogRepository.events.emit(EsdeEvent.SystemSelect("snes", "SNES", "/roms/snes"))
            advanceUntilIdle()
            assertFalse(viewModel.hasCurrentGame.value)
        }

    // --- open() ------------------------------------------------------------------------

    @Test
    fun `open shows NoGame when there is no current game`() =
        runTest(testDispatcher) {
            val viewModel = buildViewModel()

            viewModel.open()
            advanceUntilIdle()

            assertEquals(GameGuidesUiState.NoGame, viewModel.uiState.value)
        }

    @Test
    fun `open shows Library, not Browsing, when no guides exist yet for the current game`() =
        runTest(testDispatcher) {
            val esdeLogRepository = FakeEsdeLogRepository()
            val viewModel = buildViewModel(esdeLogRepository)
            esdeLogRepository.events.emit(EsdeEvent.GameStart("/roms/snes/a.sfc", "Super Game", "snes", "SNES"))
            advanceUntilIdle()

            viewModel.open()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            check(state is GameGuidesUiState.Library)
            assertTrue(state.guides.isEmpty())
        }

    @Test
    fun `open shows Library when guides already exist for the current game`() =
        runTest(testDispatcher) {
            val esdeLogRepository = FakeEsdeLogRepository()
            val libraryRepository = FakeGameGuideLibraryRepository()
            val reference = GameReference("snes", "/roms/snes/a.sfc", null)
            val guide = downloadedGuide(reference, id = "g1")
            libraryRepository.seedGuide(guide, listOf("page one"))
            val viewModel = buildViewModel(esdeLogRepository, libraryRepository)
            esdeLogRepository.events.emit(EsdeEvent.GameStart("/roms/snes/a.sfc", "A", "snes", "SNES"))
            advanceUntilIdle()

            viewModel.open()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            check(state is GameGuidesUiState.Library)
            assertEquals(listOf(guide), state.guides)
        }

    @Test
    fun `open resolves the current game's manual path into Library`() =
        runTest(testDispatcher) {
            val esdeLogRepository = FakeEsdeLogRepository()
            val reference = GameReference("snes", "/roms/snes/a.sfc", null)
            val gameMediaRepository =
                FakeGameMediaRepository(manualPathByReference = mapOf(reference to "/media/snes/manuals/a.pdf"))
            val viewModel = buildViewModel(esdeLogRepository, gameMediaRepository = gameMediaRepository)
            esdeLogRepository.events.emit(EsdeEvent.GameStart("/roms/snes/a.sfc", "A", "snes", "SNES"))
            advanceUntilIdle()

            viewModel.open()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            check(state is GameGuidesUiState.Library)
            assertEquals("/media/snes/manuals/a.pdf", state.manualPdfPath)
        }

    @Test
    fun `open resolves a null manual path when the current game has no manual`() =
        runTest(testDispatcher) {
            val esdeLogRepository = FakeEsdeLogRepository()
            val viewModel = buildViewModel(esdeLogRepository)
            esdeLogRepository.events.emit(EsdeEvent.GameStart("/roms/snes/a.sfc", "A", "snes", "SNES"))
            advanceUntilIdle()

            viewModel.open()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            check(state is GameGuidesUiState.Library)
            assertNull(state.manualPdfPath)
        }

    @Test
    fun `openBrowser switches from Library back to Browsing for the current game`() =
        runTest(testDispatcher) {
            val esdeLogRepository = FakeEsdeLogRepository()
            val libraryRepository = FakeGameGuideLibraryRepository()
            val reference = GameReference("snes", "/roms/snes/a.sfc", null)
            libraryRepository.seedGuide(downloadedGuide(reference, id = "g1"), listOf("page one"))
            val viewModel = buildViewModel(esdeLogRepository, libraryRepository)
            esdeLogRepository.events.emit(EsdeEvent.GameStart("/roms/snes/a.sfc", "A", "snes", "SNES"))
            advanceUntilIdle()
            viewModel.open()
            advanceUntilIdle()
            check(viewModel.uiState.value is GameGuidesUiState.Library)

            viewModel.openBrowser()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value is GameGuidesUiState.Browsing)
        }

    // --- openGuide() / onFontScaleChanged() ---------------------------------------------

    @Test
    fun `openGuide loads saved content and remembered progress into Viewing`() =
        runTest(testDispatcher) {
            val reference = GameReference("snes", "/roms/snes/a.sfc", null)
            val libraryRepository = FakeGameGuideLibraryRepository()
            val guide = downloadedGuide(reference, id = "g1", pageCount = 2)
            libraryRepository.seedGuide(guide, listOf("page one", "page two"))
            val settingsRepository = FakeGameGuideSettingsRepository()
            settingsRepository.setReadingProgress(GameGuideReadingProgress("g1", 0.5f, 123L))
            val viewModel =
                buildViewModel(libraryRepository = libraryRepository, settingsRepository = settingsRepository)

            viewModel.openGuide(guide)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            check(state is GameGuidesUiState.Viewing)
            assertEquals("page one", state.currentPageContent)
            assertEquals(0.5f, state.initialScrollFraction)
        }

    @Test
    fun `openGuide loads only the resumed page's content, not every page`() =
        runTest(testDispatcher) {
            val reference = GameReference("snes", "/roms/snes/a.sfc", null)
            val libraryRepository = FakeGameGuideLibraryRepository()
            val guide = downloadedGuide(reference, id = "g1", pageCount = 3)
            libraryRepository.seedGuide(guide, listOf("page one", "page two", "page three"))
            val settingsRepository = FakeGameGuideSettingsRepository()
            settingsRepository.setReadingProgress(GameGuideReadingProgress("g1", 0f, 123L, pageIndex = 1))
            val viewModel =
                buildViewModel(libraryRepository = libraryRepository, settingsRepository = settingsRepository)

            viewModel.openGuide(guide)
            advanceUntilIdle()

            assertEquals(listOf("g1" to 1), libraryRepository.loadPageCalls)
            val state = viewModel.uiState.value
            check(state is GameGuidesUiState.Viewing)
            assertEquals("page two", state.currentPageContent)
        }

    @Test
    fun `loadPage loads a different page's content into an already-Viewing guide`() =
        runTest(testDispatcher) {
            val reference = GameReference("snes", "/roms/snes/a.sfc", null)
            val libraryRepository = FakeGameGuideLibraryRepository()
            val guide = downloadedGuide(reference, id = "g1", pageCount = 2)
            libraryRepository.seedGuide(guide, listOf("page one", "page two"))
            val viewModel = buildViewModel(libraryRepository = libraryRepository)
            viewModel.openGuide(guide)
            advanceUntilIdle()
            libraryRepository.loadPageCalls.clear()

            viewModel.loadPage(1)
            advanceUntilIdle()

            assertEquals(listOf("g1" to 1), libraryRepository.loadPageCalls)
            val state = viewModel.uiState.value
            check(state is GameGuidesUiState.Viewing)
            assertEquals("page two", state.currentPageContent)
            assertFalse(state.isLoadingContent)
        }

    @Test
    fun `onDisplayPreferencesChanged persists the preference and updates a currently Viewing state`() =
        runTest(testDispatcher) {
            val reference = GameReference("snes", "/roms/snes/a.sfc", null)
            val libraryRepository = FakeGameGuideLibraryRepository()
            val guide = downloadedGuide(reference, id = "g1")
            libraryRepository.seedGuide(guide, listOf("page one"))
            val settingsRepository = FakeGameGuideSettingsRepository()
            val viewModel =
                buildViewModel(libraryRepository = libraryRepository, settingsRepository = settingsRepository)
            viewModel.openGuide(guide)
            advanceUntilIdle()

            viewModel.onDisplayPreferencesChanged(GameGuideDisplayPreferences(fontScale = 1.5f))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            check(state is GameGuidesUiState.Viewing)
            assertEquals(1.5f, state.displayPreferences.fontScale)
            assertEquals(1.5f, settingsRepository.observeDisplayPreferences().first().fontScale)
        }

    // --- deleteGuide() -------------------------------------------------------------------

    @Test
    fun `deleteGuide removes it and stays in an empty Library, not the browser`() =
        runTest(testDispatcher) {
            val esdeLogRepository = FakeEsdeLogRepository()
            val reference = GameReference("snes", "/roms/snes/a.sfc", null)
            val libraryRepository = FakeGameGuideLibraryRepository()
            libraryRepository.seedGuide(downloadedGuide(reference, id = "g1"), listOf("page one"))
            val viewModel = buildViewModel(esdeLogRepository, libraryRepository)
            esdeLogRepository.events.emit(EsdeEvent.GameStart("/roms/snes/a.sfc", "A", "snes", "SNES"))
            advanceUntilIdle()
            viewModel.open()
            advanceUntilIdle()
            check(viewModel.uiState.value is GameGuidesUiState.Library)

            viewModel.deleteGuide("g1")
            advanceUntilIdle()

            assertEquals(listOf("g1"), libraryRepository.deletedGuideIds)
            val state = viewModel.uiState.value
            check(state is GameGuidesUiState.Library)
            assertTrue(state.guides.isEmpty())
        }

    // --- importGuideFor() ----------------------------------------------------------------

    @Test
    fun `importGuideFor saves a PlainText import through the text page pipeline`() =
        runTest(testDispatcher) {
            val reference = GameReference("snes", "/roms/snes/a.sfc", null)
            val libraryRepository = FakeGameGuideLibraryRepository()
            val viewModel = buildViewModel(libraryRepository = libraryRepository)

            viewModel.importGuideFor(
                reference,
                "Super Game",
                "hello guide".toByteArray(),
                "notes.txt",
                GameGuideFormat.PlainText,
            )
            advanceUntilIdle()

            val guides = libraryRepository.observeGuidesFor(reference).first()
            assertEquals(1, guides.size)
            val guide = guides.single()
            assertEquals(GameGuideFormat.PlainText, guide.format)
            assertEquals(listOf("hello guide"), libraryRepository.loadContent(guide.id))
        }

    @Test
    fun `importGuideFor saves a Pdf import through the binary pipeline`() =
        runTest(testDispatcher) {
            val reference = GameReference("snes", "/roms/snes/a.sfc", null)
            val libraryRepository = FakeGameGuideLibraryRepository()
            val viewModel = buildViewModel(libraryRepository = libraryRepository)

            viewModel.importGuideFor(
                reference,
                "Super Game",
                byteArrayOf(1, 2, 3),
                "manual-scan.pdf",
                GameGuideFormat.Pdf,
            )
            advanceUntilIdle()

            val guides = libraryRepository.observeGuidesFor(reference).first()
            assertEquals(1, guides.size)
            val guide = guides.single()
            assertEquals(GameGuideFormat.Pdf, guide.format)
            assertEquals(null, libraryRepository.loadContent(guide.id))
            assertTrue(libraryRepository.binaryContentPath(guide.id)?.endsWith(".pdf") == true)
        }

    @Test
    fun `importGuideFor refreshes the Library to include the newly imported guide`() =
        runTest(testDispatcher) {
            val esdeLogRepository = FakeEsdeLogRepository()
            val reference = GameReference("snes", "/roms/snes/a.sfc", null)
            val libraryRepository = FakeGameGuideLibraryRepository()
            val viewModel = buildViewModel(esdeLogRepository, libraryRepository)
            esdeLogRepository.events.emit(EsdeEvent.GameStart("/roms/snes/a.sfc", "Super Game", "snes", "SNES"))
            advanceUntilIdle()
            viewModel.open()
            advanceUntilIdle()

            viewModel.importGuideFor(
                reference,
                "Super Game",
                "screenshot".toByteArray(),
                "cover.png",
                GameGuideFormat.Image,
            )
            advanceUntilIdle()

            val state = viewModel.uiState.value
            check(state is GameGuidesUiState.Library)
            assertEquals(1, state.guides.size)
            assertEquals(GameGuideFormat.Image, state.guides.single().format)
        }

    // --- autoOpenLastViewedGuideForCurrentGame() -------------------------------------------

    @Test
    fun `autoOpenLastViewedGuideForCurrentGame returns false when the current game has no guides`() =
        runTest(testDispatcher) {
            val esdeLogRepository = FakeEsdeLogRepository()
            val viewModel = buildViewModel(esdeLogRepository)
            esdeLogRepository.events.emit(EsdeEvent.GameStart("/roms/snes/a.sfc", "A", "snes", "SNES"))
            advanceUntilIdle()

            val opened = viewModel.autoOpenLastViewedGuideForCurrentGame()

            assertFalse(opened)
            assertEquals(GameGuidesUiState.NoGame, viewModel.uiState.value)
        }
}

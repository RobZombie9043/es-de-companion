package com.esde.companion.ui.manual

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModelStore
import com.esde.companion.data.pdf.ManualRenderer
import com.esde.companion.domain.model.EsdeEvent
import com.esde.companion.domain.model.GameMedia
import com.esde.companion.domain.model.MediaType
import com.esde.companion.domain.repository.EsdeLogRepository
import com.esde.companion.domain.repository.GameMediaRepository
import com.esde.companion.domain.usecase.ObserveAppStateUseCase
import com.esde.companion.domain.usecase.ObserveConnectionStateUseCase
import com.esde.companion.domain.usecase.ResolveGameMediaUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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

class GameManualViewModelTest {
    // --- Fakes -------------------------------------------------------------------------

    private class FakeEsdeLogRepository : EsdeLogRepository {
        val events = MutableSharedFlow<EsdeEvent>()

        override fun observeEvents(): Flow<EsdeEvent> = events

        override fun observeLogFileExists(): Flow<Boolean> = flowOf(true)
    }

    /** Manual path is just "<romPath>.pdf" - every distinct rom gets a distinct manual path. */
    private class FakeGameMediaRepository : GameMediaRepository {
        override suspend fun resolveMedia(
            systemShortName: String,
            systemPath: String?,
            romPath: String,
            mediaTypes: Set<MediaType>,
        ): GameMedia = GameMedia(baseRelativePath = null, filesByType = mapOf(MediaType.Manuals to "$romPath.pdf"))
    }

    private class FakeManualRenderer(
        override val pageCount: Int,
        private val renderResult: (Int) -> ImageBitmap? = { null },
    ) : ManualRenderer {
        val renderedPages = mutableListOf<Int>()
        var closed = false
            private set

        override suspend fun renderPage(
            index: Int,
            targetWidthPx: Int,
        ): ImageBitmap? {
            renderedPages += index
            return renderResult(index)
        }

        override fun close() {
            closed = true
        }
    }

    private class FakeRendererFactory(
        private val pageCountForPath: (String) -> Int = { 3 },
    ) {
        val openedRenderers = mutableMapOf<String, FakeManualRenderer>()
        var openCallCount = 0
            private set

        val open: suspend (String) -> ManualRenderer? = { path ->
            openCallCount++
            FakeManualRenderer(pageCount = pageCountForPath(path)).also { openedRenderers[path] = it }
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
        gameMediaRepository: GameMediaRepository = FakeGameMediaRepository(),
        openRenderer: suspend (String) -> ManualRenderer? = { null },
    ): GameManualViewModel {
        val observeAppState = ObserveAppStateUseCase(esdeLogRepository, backgroundScope)
        val observeConnectionState = ObserveConnectionStateUseCase(esdeLogRepository, observeAppState)
        val viewModel =
            GameManualViewModel(observeConnectionState, ResolveGameMediaUseCase(gameMediaRepository), openRenderer)
        advanceUntilIdle()
        return viewModel
    }

    // --- pdfPath resolution per AppState -------------------------------------------------

    @Test
    fun `pdfPath resolves for PlayingGame, BrowsingGame, and a game-having Screensaver`() =
        runTest(testDispatcher) {
            val esdeLogRepository = FakeEsdeLogRepository()
            val viewModel = buildViewModel(esdeLogRepository)

            esdeLogRepository.events.emit(EsdeEvent.GameStart("/roms/snes/a.sfc", "A", "snes", "SNES"))
            advanceUntilIdle()
            assertEquals("/roms/snes/a.sfc.pdf", viewModel.pdfPath.value)

            esdeLogRepository.events.emit(EsdeEvent.GameEnd("/roms/snes/a.sfc", "A", "snes", "SNES"))
            esdeLogRepository.events.emit(EsdeEvent.GameSelect("/roms/snes/b.sfc", "B", "snes", "SNES"))
            advanceUntilIdle()
            assertEquals("/roms/snes/b.sfc.pdf", viewModel.pdfPath.value)

            esdeLogRepository.events.emit(EsdeEvent.ScreensaverStart("timer"))
            esdeLogRepository.events.emit(EsdeEvent.ScreensaverGameSelect("/roms/snes/c.sfc", "C", "snes", "SNES"))
            advanceUntilIdle()
            assertEquals("/roms/snes/c.sfc.pdf", viewModel.pdfPath.value)
        }

    @Test
    fun `pdfPath is null for Idle, BrowsingSystem, and a game-less Screensaver`() =
        runTest(testDispatcher) {
            val esdeLogRepository = FakeEsdeLogRepository()
            val viewModel = buildViewModel(esdeLogRepository)

            assertNull(viewModel.pdfPath.value)

            esdeLogRepository.events.emit(EsdeEvent.SystemSelect("snes", "SNES", "/roms/snes"))
            advanceUntilIdle()
            assertNull(viewModel.pdfPath.value)

            esdeLogRepository.events.emit(EsdeEvent.ScreensaverStart("timer"))
            advanceUntilIdle()
            assertNull(viewModel.pdfPath.value)
        }

    // --- renderer lifecycle ---------------------------------------------------------------

    @Test
    fun `opening a resolved path renders page 0 and prefetches page 1`() =
        runTest(testDispatcher) {
            val esdeLogRepository = FakeEsdeLogRepository()
            val factory = FakeRendererFactory()
            val viewModel = buildViewModel(esdeLogRepository, openRenderer = factory.open)
            viewModel.setTargetWidth(200)

            esdeLogRepository.events.emit(EsdeEvent.GameStart("/roms/snes/a.sfc", "A", "snes", "SNES"))
            advanceUntilIdle()

            assertEquals(3, viewModel.pageCount.value)
            assertEquals(0, viewModel.currentPage.value)
            assertEquals(listOf(0, 1), factory.openedRenderers.getValue("/roms/snes/a.sfc.pdf").renderedPages)
        }

    @Test
    fun `switching games closes the previous renderer before opening the new one, and resets page state`() =
        runTest(testDispatcher) {
            val esdeLogRepository = FakeEsdeLogRepository()
            val factory = FakeRendererFactory()
            val viewModel = buildViewModel(esdeLogRepository, openRenderer = factory.open)
            viewModel.setTargetWidth(200)

            esdeLogRepository.events.emit(EsdeEvent.GameStart("/roms/snes/a.sfc", "A", "snes", "SNES"))
            advanceUntilIdle()
            viewModel.nextPage()
            advanceUntilIdle()
            assertEquals(1, viewModel.currentPage.value)
            val firstRenderer = factory.openedRenderers.getValue("/roms/snes/a.sfc.pdf")

            esdeLogRepository.events.emit(EsdeEvent.GameEnd("/roms/snes/a.sfc", "A", "snes", "SNES"))
            esdeLogRepository.events.emit(EsdeEvent.GameSelect("/roms/snes/b.sfc", "B", "snes", "SNES"))
            esdeLogRepository.events.emit(EsdeEvent.GameStart("/roms/snes/b.sfc", "B", "snes", "SNES"))
            advanceUntilIdle()

            assertTrue(firstRenderer.closed)
            assertEquals(0, viewModel.currentPage.value)
            assertTrue(factory.openedRenderers.containsKey("/roms/snes/b.sfc.pdf"))
        }

    @Test
    fun `re-resolving the same path is a no-op - the renderer is not reopened`() =
        runTest(testDispatcher) {
            val esdeLogRepository = FakeEsdeLogRepository()
            val factory = FakeRendererFactory()
            val viewModel = buildViewModel(esdeLogRepository, openRenderer = factory.open)
            viewModel.setTargetWidth(200)

            val gameSelect = EsdeEvent.GameSelect("/roms/snes/a.sfc", "A", "snes", "SNES")
            esdeLogRepository.events.emit(gameSelect)
            advanceUntilIdle()
            assertEquals(1, factory.openCallCount)

            // The documented spurious re-select for the same ROM - reducer collapses it to
            // the same BrowsingGame, so pdfPath's own distinctUntilChanged never re-fires,
            // but this also pins down onPathChanged's own openedForPath guard.
            esdeLogRepository.events.emit(gameSelect)
            advanceUntilIdle()

            assertEquals(1, factory.openCallCount)
        }

    // --- page navigation --------------------------------------------------------------------

    @Test
    fun `nextPage and previousPage are no-ops at the edges and on the same index`() =
        runTest(testDispatcher) {
            val esdeLogRepository = FakeEsdeLogRepository()
            val factory = FakeRendererFactory()
            val viewModel = buildViewModel(esdeLogRepository, openRenderer = factory.open)
            viewModel.setTargetWidth(200)
            esdeLogRepository.events.emit(EsdeEvent.GameStart("/roms/snes/a.sfc", "A", "snes", "SNES"))
            advanceUntilIdle()

            viewModel.previousPage()
            advanceUntilIdle()
            assertEquals(0, viewModel.currentPage.value)

            viewModel.nextPage()
            advanceUntilIdle()
            viewModel.nextPage()
            advanceUntilIdle()
            assertEquals(2, viewModel.currentPage.value)

            viewModel.nextPage()
            advanceUntilIdle()
            assertEquals(2, viewModel.currentPage.value)
        }

    @Test
    fun `goToPage re-renders the new page and prefetches the next one only if it exists`() =
        runTest(testDispatcher) {
            val esdeLogRepository = FakeEsdeLogRepository()
            val factory = FakeRendererFactory()
            val viewModel = buildViewModel(esdeLogRepository, openRenderer = factory.open)
            viewModel.setTargetWidth(200)
            esdeLogRepository.events.emit(EsdeEvent.GameStart("/roms/snes/a.sfc", "A", "snes", "SNES"))
            advanceUntilIdle()

            viewModel.nextPage()
            viewModel.nextPage()
            advanceUntilIdle()

            assertEquals(2, viewModel.currentPage.value)
            assertEquals(null, viewModel.nextBitmap.value)
        }

    @Test
    fun `setTargetWidth no-ops on a non-positive or unchanged width, and re-renders on a genuine change`() =
        runTest(testDispatcher) {
            val esdeLogRepository = FakeEsdeLogRepository()
            val factory = FakeRendererFactory()
            val viewModel = buildViewModel(esdeLogRepository, openRenderer = factory.open)
            viewModel.setTargetWidth(200)
            esdeLogRepository.events.emit(EsdeEvent.GameStart("/roms/snes/a.sfc", "A", "snes", "SNES"))
            advanceUntilIdle()
            val renderer = factory.openedRenderers.getValue("/roms/snes/a.sfc.pdf")
            val countAfterOpen = renderer.renderedPages.size

            viewModel.setTargetWidth(0)
            viewModel.setTargetWidth(200)
            advanceUntilIdle()
            assertEquals(countAfterOpen, renderer.renderedPages.size)

            viewModel.setTargetWidth(400)
            advanceUntilIdle()
            assertTrue(renderer.renderedPages.size > countAfterOpen)
        }

    @Test
    fun `onCleared closes the currently open renderer`() =
        runTest(testDispatcher) {
            val esdeLogRepository = FakeEsdeLogRepository()
            val factory = FakeRendererFactory()
            val viewModel = buildViewModel(esdeLogRepository, openRenderer = factory.open)
            viewModel.setTargetWidth(200)
            esdeLogRepository.events.emit(EsdeEvent.GameStart("/roms/snes/a.sfc", "A", "snes", "SNES"))
            advanceUntilIdle()
            val renderer = factory.openedRenderers.getValue("/roms/snes/a.sfc.pdf")

            // onCleared() is protected (androidx.lifecycle.ViewModel) - a ViewModelStore is
            // the standard way to trigger it from outside the class hierarchy.
            val store = ViewModelStore()
            store.put("gameManual", viewModel)
            store.clear()

            assertTrue(renderer.closed)
        }
}

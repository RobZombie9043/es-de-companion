package com.esde.companion.domain.usecase

import app.cash.turbine.test
import app.cash.turbine.testIn
import app.cash.turbine.turbineScope
import com.esde.companion.domain.model.AppState
import com.esde.companion.domain.model.EsdeEvent
import com.esde.companion.domain.repository.EsdeLogRepository
import com.esde.companion.domain.state.AppStateReducer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveAppStateUseCaseTest {
    private class FakeEsdeLogRepository(
        private val events: Flow<EsdeEvent>,
    ) : EsdeLogRepository {
        override fun observeEvents(): Flow<EsdeEvent> = events

        override fun observeLogFileExists(): Flow<Boolean> = flowOf(true)
    }

    @Test
    fun `default reducer matches AppStateReducer directly when none is supplied`() =
        runTest(UnconfinedTestDispatcher()) {
            val events = MutableSharedFlow<EsdeEvent>()
            val repository = FakeEsdeLogRepository(events)
            val useCase = ObserveAppStateUseCase(repository, backgroundScope)

            useCase().test {
                assertEquals(AppState.Idle, awaitItem())

                events.emit(EsdeEvent.SystemSelect("gc", "Nintendo GameCube", "/roms/gc"))

                val updated = awaitItem()
                assertEquals("gc", (updated as AppState.BrowsingSystem).systemShortName)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `supplied reducer is invoked with the previous state and event, and its result is what's emitted`() =
        runTest(UnconfinedTestDispatcher()) {
            val events = MutableSharedFlow<EsdeEvent>()
            val repository = FakeEsdeLogRepository(events)
            val seen = mutableListOf<Pair<AppState, EsdeEvent>>()
            val useCase =
                ObserveAppStateUseCase(repository, backgroundScope) { state, event ->
                    seen += state to event
                    AppStateReducer.reduce(state, event)
                }

            useCase().test {
                assertEquals(AppState.Idle, awaitItem())

                val event = EsdeEvent.SystemSelect("gc", "Nintendo GameCube", "/roms/gc")
                events.emit(event)

                val updated = awaitItem()
                assertEquals("gc", (updated as AppState.BrowsingSystem).systemShortName)
                assertEquals(listOf(AppState.Idle to event), seen)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `the reducer folds exactly once per event, regardless of subscriber count`() =
        runTest(UnconfinedTestDispatcher()) {
            val events = MutableSharedFlow<EsdeEvent>()
            val repository = FakeEsdeLogRepository(events)
            var foldCount = 0
            val useCase =
                ObserveAppStateUseCase(repository, backgroundScope) { state, event ->
                    foldCount++
                    AppStateReducer.reduce(state, event)
                }

            turbineScope {
                val subscriber1 = useCase().testIn(backgroundScope)
                val subscriber2 = useCase().testIn(backgroundScope)
                assertEquals(AppState.Idle, subscriber1.awaitItem())
                assertEquals(AppState.Idle, subscriber2.awaitItem())

                events.emit(EsdeEvent.SystemSelect("gc", "Nintendo GameCube", "/roms/gc"))
                subscriber1.awaitItem()
                subscriber2.awaitItem()

                // Two subscribers, one event - if the scan/fold were rebuilt per subscriber
                // instead of shared via stateIn, this would be 2.
                assertEquals(1, foldCount)

                subscriber1.cancel()
                subscriber2.cancel()
            }
        }

    @Test
    fun `a subscriber joining after state has already advanced sees the current state immediately, not Idle`() =
        runTest(UnconfinedTestDispatcher()) {
            val events = MutableSharedFlow<EsdeEvent>()
            val repository = FakeEsdeLogRepository(events)
            val useCase = ObserveAppStateUseCase(repository, backgroundScope)

            useCase().test {
                assertEquals(AppState.Idle, awaitItem())
                events.emit(EsdeEvent.SystemSelect("gc", "Nintendo GameCube", "/roms/gc"))
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            useCase().test {
                val lateJoiner = awaitItem()
                assertEquals("gc", (lateJoiner as AppState.BrowsingSystem).systemShortName)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `state survives every subscriber disconnecting for an extended period, since sharing is Eagerly`() =
        runTest(UnconfinedTestDispatcher()) {
            // Sharing is SharingStarted.Eagerly, not WhileSubscribed - the upstream fold never
            // stops just because every downstream subscriber momentarily disconnects, which
            // matters because Flow.scan's accumulator would otherwise restart from its seed
            // (AppState.Idle) on any fresh upstream collection, clobbering the shared value.
            // advanceTimeBy here simulates a gap far longer than WhileSubscribed's old 5s
            // stop timeout would have tolerated, to prove that gap no longer matters.
            val events = MutableSharedFlow<EsdeEvent>()
            val repository = FakeEsdeLogRepository(events)
            val useCase = ObserveAppStateUseCase(repository, backgroundScope)

            useCase().test {
                assertEquals(AppState.Idle, awaitItem())
                events.emit(EsdeEvent.SystemSelect("gc", "Nintendo GameCube", "/roms/gc"))
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            advanceTimeBy(10_000)

            useCase().test {
                val lateJoiner = awaitItem()
                assertEquals("gc", (lateJoiner as AppState.BrowsingSystem).systemShortName)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `multiple concurrent subscribers observe an identical emission sequence in lockstep`() =
        runTest(UnconfinedTestDispatcher()) {
            val events = MutableSharedFlow<EsdeEvent>()
            val repository = FakeEsdeLogRepository(events)
            val useCase = ObserveAppStateUseCase(repository, backgroundScope)

            turbineScope {
                val subscribers = List(3) { useCase().testIn(backgroundScope) }
                subscribers.forEach { assertEquals(AppState.Idle, it.awaitItem()) }

                events.emit(EsdeEvent.SystemSelect("gc", "Nintendo GameCube", "/roms/gc"))
                val afterSystemSelect = subscribers.map { it.awaitItem() }
                assertEquals(1, afterSystemSelect.toSet().size)

                events.emit(EsdeEvent.GameSelect("/roms/gc/game.iso", "Game", "gc", "Nintendo GameCube"))
                val afterGameSelect = subscribers.map { it.awaitItem() }
                assertEquals(1, afterGameSelect.toSet().size)

                subscribers.forEach { it.cancel() }
            }
        }

    @Test
    fun `a cold-start anchor-replay burst folds correctly through the shared stateIn`() =
        runTest(UnconfinedTestDispatcher()) {
            // Mirrors EsdeLogFileRepositoryTest's anchor-replay shape: a burst of events
            // delivered synchronously on subscribe (not one-at-a-time via a hot flow), as
            // happens when the log repository replays forward from a found startup anchor.
            val burst =
                flowOf(
                    EsdeEvent.SystemSelect("gc", "Nintendo GameCube", "/roms/gc"),
                    EsdeEvent.ScreensaverStart("timer"),
                    EsdeEvent.ScreensaverGameSelect("/roms/gc/game.iso", "Game", "gc", "Nintendo GameCube"),
                    EsdeEvent.ScreensaverEnd("cancel"),
                )
            val repository = FakeEsdeLogRepository(burst)
            val useCase = ObserveAppStateUseCase(repository, backgroundScope)

            useCase().test {
                val settled = awaitItem()
                assertEquals("gc", (settled as AppState.BrowsingSystem).systemShortName)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a supplied reducer is also only invoked once per event across multiple subscribers`() =
        runTest(UnconfinedTestDispatcher()) {
            val events = MutableSharedFlow<EsdeEvent>()
            val repository = FakeEsdeLogRepository(events)
            var foldCount = 0
            val useCase =
                ObserveAppStateUseCase(repository, backgroundScope) { state, event ->
                    foldCount++
                    AppStateReducer.reduce(state, event)
                }

            turbineScope {
                val subscriber1 = useCase().testIn(backgroundScope)
                val subscriber2 = useCase().testIn(backgroundScope)
                val subscriber3 = useCase().testIn(backgroundScope)
                listOf(subscriber1, subscriber2, subscriber3).forEach { it.awaitItem() }

                events.emit(EsdeEvent.SystemSelect("gc", "Nintendo GameCube", "/roms/gc"))
                listOf(subscriber1, subscriber2, subscriber3).forEach { it.awaitItem() }

                assertEquals(1, foldCount)

                subscriber1.cancel()
                subscriber2.cancel()
                subscriber3.cancel()
            }
        }
}

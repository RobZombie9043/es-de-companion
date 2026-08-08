package com.esde.companion.domain.usecase

import app.cash.turbine.test
import com.esde.companion.domain.model.AppState
import com.esde.companion.domain.model.EsdeEvent
import com.esde.companion.domain.repository.EsdeLogRepository
import com.esde.companion.domain.state.AppStateReducer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
}

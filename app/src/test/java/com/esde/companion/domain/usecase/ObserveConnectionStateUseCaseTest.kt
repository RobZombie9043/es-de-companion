package com.esde.companion.domain.usecase

import app.cash.turbine.test
import com.esde.companion.domain.model.AppState
import com.esde.companion.domain.model.EsdeConnectionState
import com.esde.companion.domain.model.EsdeEvent
import com.esde.companion.domain.repository.EsdeLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveConnectionStateUseCaseTest {

    private class FakeEsdeLogRepository(
        private val events: Flow<EsdeEvent>,
        private val fileExists: Flow<Boolean>,
    ) : EsdeLogRepository {
        override fun observeEvents(): Flow<EsdeEvent> = events
        override fun observeLogFileExists(): Flow<Boolean> = fileExists
    }

    @Test
    fun `emits LogFileNotFound when the log file does not exist, regardless of AppState`() = runTest {
        val repository = FakeEsdeLogRepository(
            events = flowOf(),
            fileExists = flowOf(false),
        )
        val useCase = ObserveConnectionStateUseCase(repository, ObserveAppStateUseCase(repository))

        useCase().test {
            assertEquals(EsdeConnectionState.LogFileNotFound, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `emits Connected wrapping the reduced AppState when the log file exists`() = runTest {
        // A MutableSharedFlow, not flowOf, so this test controls exactly when each event
        // arrives - combine() only guarantees the *latest* combination reaches the
        // collector, so two synchronous upstream emissions (as flowOf would produce) can
        // conflate into one downstream item before the collector catches up. Emitting
        // explicitly, with an awaitItem() in between, keeps each step deterministic.
        val events = MutableSharedFlow<EsdeEvent>()
        val repository = FakeEsdeLogRepository(
            events = events,
            fileExists = flowOf(true),
        )
        val useCase = ObserveConnectionStateUseCase(repository, ObserveAppStateUseCase(repository))

        useCase().test {
            val initial = awaitItem()
            check(initial is EsdeConnectionState.Connected)
            assertEquals(AppState.Idle, initial.appState)

            events.emit(EsdeEvent.SystemSelect("gc", "Nintendo GameCube", "/roms/gc"))

            val updated = awaitItem()
            check(updated is EsdeConnectionState.Connected)
            assertEquals("gc", (updated.appState as AppState.BrowsingSystem).systemShortName)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
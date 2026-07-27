package com.esde.companion.domain.usecase

import app.cash.turbine.test
import com.esde.companion.domain.model.EsdeConnectionState
import com.esde.companion.domain.model.EsdeEvent
import com.esde.companion.domain.repository.EsdeLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Note: uses Turbine (see CLAUDE.md tech stack) for Flow assertions. If Turbine isn't
 * yet added as a test dependency, add `testImplementation(libs.turbine)` alongside
 * kotlinx-coroutines-test in app/build.gradle.kts.
 */
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
        val repository = FakeEsdeLogRepository(
            events = flowOf(EsdeEvent.SystemSelect("gc", "Nintendo GameCube", "/roms/gc")),
            fileExists = flowOf(true),
        )
        val useCase = ObserveConnectionStateUseCase(repository, ObserveAppStateUseCase(repository))

        useCase().test {
            val first = awaitItem()
            check(first is EsdeConnectionState.Connected)
            val second = awaitItem()
            check(second is EsdeConnectionState.Connected)
            assertEquals("gc", (second.appState as com.esde.companion.domain.model.AppState.BrowsingSystem).systemShortName)
            awaitComplete()
        }
    }
}
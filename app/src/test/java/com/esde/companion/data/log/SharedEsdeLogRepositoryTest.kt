package com.esde.companion.data.log

import app.cash.turbine.test
import app.cash.turbine.testIn
import app.cash.turbine.turbineScope
import com.esde.companion.domain.model.EsdeEvent
import com.esde.companion.domain.repository.EsdeLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SharedEsdeLogRepositoryTest {
    private class FakeEsdeLogRepository(
        private val events: Flow<EsdeEvent> = MutableSharedFlow(),
        private val fileExists: Flow<Boolean> = flowOf(true),
    ) : EsdeLogRepository {
        var eventsCollectCount = 0

        override fun observeEvents(): Flow<EsdeEvent> =
            flow {
                eventsCollectCount++
                events.collect { emit(it) }
            }

        override fun observeLogFileExists(): Flow<Boolean> = fileExists
    }

    @Test
    fun `two concurrent subscribers to observeEvents collect the inner repository exactly once`() =
        runTest(UnconfinedTestDispatcher()) {
            val events = MutableSharedFlow<EsdeEvent>()
            val fake = FakeEsdeLogRepository(events)
            val repository = SharedEsdeLogRepository(fake, backgroundScope)

            turbineScope {
                val subscriber1 = repository.observeEvents().testIn(backgroundScope)
                val subscriber2 = repository.observeEvents().testIn(backgroundScope)

                events.emit(EsdeEvent.Startup)
                subscriber1.awaitItem()
                subscriber2.awaitItem()

                assertEquals(1, fake.eventsCollectCount)

                subscriber1.cancel()
                subscriber2.cancel()
            }
        }

    @Test
    fun `observeLogFileExists replays its most recent value to a late-joining subscriber`() =
        runTest(UnconfinedTestDispatcher()) {
            val fileExists = MutableSharedFlow<Boolean>(replay = 1)
            val fake = FakeEsdeLogRepository(fileExists = fileExists)
            val repository = SharedEsdeLogRepository(fake, backgroundScope)

            repository.observeLogFileExists().test {
                fileExists.emit(true)
                assertEquals(true, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            // A fresh subscriber, joining after the previous one departed - built with
            // replay = 1, so it should see the last value immediately, with no new upstream
            // emission required.
            repository.observeLogFileExists().test {
                assertEquals(true, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `observeEvents does not replay to a subscriber joining after the prior subscriber departed`() =
        runTest(UnconfinedTestDispatcher()) {
            // observeEvents has no replay (unlike observeLogFileExists' replay = 1) - this
            // asymmetry is exactly why ObserveAppStateUseCase needs its own stateIn wrapping
            // one layer up, rather than relying on this shared event stream alone.
            val events = MutableSharedFlow<EsdeEvent>()
            val fake = FakeEsdeLogRepository(events)
            val repository = SharedEsdeLogRepository(fake, backgroundScope)

            repository.observeEvents().test {
                events.emit(EsdeEvent.Startup)
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            repository.observeEvents().test {
                expectNoEvents()

                events.emit(EsdeEvent.Quit)
                assertEquals(EsdeEvent.Quit, awaitItem())

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a subscriber joining after the stop timeout has elapsed triggers a fresh collection`() =
        runTest(UnconfinedTestDispatcher()) {
            val events = MutableSharedFlow<EsdeEvent>()
            val fake = FakeEsdeLogRepository(events)
            val repository = SharedEsdeLogRepository(fake, backgroundScope)

            repository.observeEvents().test {
                events.emit(EsdeEvent.Startup)
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(1, fake.eventsCollectCount)

            advanceTimeBy(5_001)

            repository.observeEvents().test {
                events.emit(EsdeEvent.Quit)
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(2, fake.eventsCollectCount)
        }
}

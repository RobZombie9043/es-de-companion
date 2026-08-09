package com.esde.companion.domain.usecase

import app.cash.turbine.test
import com.esde.companion.domain.model.EsdeEvent
import com.esde.companion.domain.repository.EsdeLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ObserveEsdeQuitEventUseCaseTest {
    private class FakeEsdeLogRepository(
        private val events: Flow<EsdeEvent>,
    ) : EsdeLogRepository {
        override fun observeEvents(): Flow<EsdeEvent> = events

        override fun observeLogFileExists(): Flow<Boolean> = flowOf(true)
    }

    @Test
    fun `emits once for a Quit event`() =
        runTest {
            val repository = FakeEsdeLogRepository(flowOf(EsdeEvent.Quit))
            val useCase = ObserveEsdeQuitEventUseCase(repository)

            useCase().test {
                awaitItem()
                awaitComplete()
            }
        }

    @Test
    fun `does not emit for non-Quit event types`() =
        runTest {
            val repository =
                FakeEsdeLogRepository(
                    flowOf(
                        EsdeEvent.Startup,
                        EsdeEvent.SystemSelect("gc", "Nintendo GameCube", "/roms/gc"),
                        EsdeEvent.Reload,
                    ),
                )
            val useCase = ObserveEsdeQuitEventUseCase(repository)

            useCase().test {
                awaitComplete()
            }
        }

    @Test
    fun `a mixed sequence emits exactly once per Quit, in order`() =
        runTest {
            val repository =
                FakeEsdeLogRepository(
                    flowOf(
                        EsdeEvent.SystemSelect("gc", "Nintendo GameCube", "/roms/gc"),
                        EsdeEvent.Quit,
                        EsdeEvent.GameSelect("/roms/gc/game.iso", "Game", "gc", "Nintendo GameCube"),
                        EsdeEvent.Quit,
                    ),
                )
            val useCase = ObserveEsdeQuitEventUseCase(repository)

            useCase().test {
                awaitItem()
                awaitItem()
                awaitComplete()
            }
        }
}

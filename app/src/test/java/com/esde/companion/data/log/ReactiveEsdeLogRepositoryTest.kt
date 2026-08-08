package com.esde.companion.data.log

import app.cash.turbine.test
import com.esde.companion.domain.model.EsdeEvent
import com.esde.companion.domain.repository.EsdeLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ReactiveEsdeLogRepositoryTest {
    private class FakeEsdeLogRepository(private val exists: Boolean) : EsdeLogRepository {
        override fun observeEvents(): Flow<EsdeEvent> = flowOf()

        override fun observeLogFileExists(): Flow<Boolean> = flowOf(exists)
    }

    @Test
    fun `observeLogFileExists is false when no folder is configured yet`() =
        runTest {
            val repository =
                ReactiveEsdeLogRepository(
                    logFolderPath = flowOf(null),
                    repositoryFactory = { FakeEsdeLogRepository(exists = true) },
                )

            repository.observeLogFileExists().test {
                assertEquals(false, awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `observeLogFileExists delegates to the repository built for the configured folder`() =
        runTest {
            val repository =
                ReactiveEsdeLogRepository(
                    logFolderPath = flowOf("/storage/emulated/0/ES-DE"),
                    repositoryFactory = { folder -> FakeEsdeLogRepository(exists = folder == "/storage/emulated/0/ES-DE") },
                )

            repository.observeLogFileExists().test {
                assertEquals(true, awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `switches to a new underlying repository when the folder path changes`() =
        runTest {
            val folderPath = MutableStateFlow<String?>("/storage/emulated/0/ES-DE")
            val repository =
                ReactiveEsdeLogRepository(
                    logFolderPath = folderPath,
                    repositoryFactory = { folder -> FakeEsdeLogRepository(exists = folder == "/sdcard/ES-DE") },
                )

            repository.observeLogFileExists().test {
                assertEquals(false, awaitItem())
                folderPath.value = "/sdcard/ES-DE"
                assertEquals(true, awaitItem())
            }
        }
}

package com.esde.companion.data.systems

import com.esde.companion.domain.repository.SystemPathRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReactiveSystemPathRepositoryTest {
    private class FakeSystemPathRepository(private val root: String) : SystemPathRepository {
        override suspend fun resolveSystemPath(systemShortName: String): String? = "$root/$systemShortName"
    }

    @Test
    fun `resolves to null when no root path is configured yet, without building a repository`() =
        runTest {
            var buildCount = 0
            val repository =
                ReactiveSystemPathRepository(
                    esdeRootPath = flowOf(null),
                    repositoryFactory = { root ->
                        buildCount++
                        FakeSystemPathRepository(root)
                    },
                )

            val result = repository.resolveSystemPath("dreamcast")

            assertNull(result)
            assertEquals(0, buildCount)
        }

    @Test
    fun `builds exactly one underlying repository for the first call with a given root`() =
        runTest {
            var buildCount = 0
            val repository =
                ReactiveSystemPathRepository(
                    esdeRootPath = flowOf("/storage/emulated/0/ES-DE"),
                    repositoryFactory = { root ->
                        buildCount++
                        FakeSystemPathRepository(root)
                    },
                )

            repository.resolveSystemPath("dreamcast")

            assertEquals(1, buildCount)
        }

    @Test
    fun `a second call with the same root reuses the cached repository instead of rebuilding`() =
        runTest {
            var buildCount = 0
            val repository =
                ReactiveSystemPathRepository(
                    esdeRootPath = flowOf("/storage/emulated/0/ES-DE"),
                    repositoryFactory = { root ->
                        buildCount++
                        FakeSystemPathRepository(root)
                    },
                )

            repository.resolveSystemPath("dreamcast")
            repository.resolveSystemPath("psx")

            assertEquals(1, buildCount)
        }

    @Test
    fun `a root change between calls rebuilds the repository and reflects the new root`() =
        runTest {
            var buildCount = 0
            val root = MutableStateFlow("/storage/emulated/0/ES-DE")
            val repository =
                ReactiveSystemPathRepository(
                    esdeRootPath = root,
                    repositoryFactory = { path ->
                        buildCount++
                        FakeSystemPathRepository(path)
                    },
                )

            repository.resolveSystemPath("dreamcast")
            root.value = "/sdcard/ES-DE"
            val second = repository.resolveSystemPath("dreamcast")

            assertEquals(2, buildCount)
            assertEquals("/sdcard/ES-DE/dreamcast", second)
        }
}

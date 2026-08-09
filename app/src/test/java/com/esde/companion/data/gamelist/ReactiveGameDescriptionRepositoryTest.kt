package com.esde.companion.data.gamelist

import com.esde.companion.domain.model.GameDescription
import com.esde.companion.domain.repository.GameDescriptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReactiveGameDescriptionRepositoryTest {
    private class FakeGameDescriptionRepository(private val root: String) : GameDescriptionRepository {
        override suspend fun resolveDescription(
            systemShortName: String,
            romPath: String,
        ): GameDescription = GameDescription(text = "$root/$systemShortName/$romPath")
    }

    @Test
    fun `resolves to an empty GameDescription when no root path is configured yet, without building a repository`() =
        runTest {
            var buildCount = 0
            val repository =
                ReactiveGameDescriptionRepository(
                    esdeRootPath = flowOf(null),
                    repositoryFactory = { root ->
                        buildCount++
                        FakeGameDescriptionRepository(root)
                    },
                )

            val result = repository.resolveDescription("dreamcast", "/roms/dreamcast/game.chd")

            assertNull(result.text)
            assertEquals(0, buildCount)
        }

    @Test
    fun `builds exactly one underlying repository for the first call with a given root`() =
        runTest {
            var buildCount = 0
            val repository =
                ReactiveGameDescriptionRepository(
                    esdeRootPath = flowOf("/storage/emulated/0/ES-DE"),
                    repositoryFactory = { root ->
                        buildCount++
                        FakeGameDescriptionRepository(root)
                    },
                )

            repository.resolveDescription("dreamcast", "/roms/dreamcast/game.chd")

            assertEquals(1, buildCount)
        }

    @Test
    fun `a second call with the same root reuses the cached repository instead of rebuilding`() =
        runTest {
            var buildCount = 0
            val repository =
                ReactiveGameDescriptionRepository(
                    esdeRootPath = flowOf("/storage/emulated/0/ES-DE"),
                    repositoryFactory = { root ->
                        buildCount++
                        FakeGameDescriptionRepository(root)
                    },
                )

            repository.resolveDescription("dreamcast", "/roms/dreamcast/game.chd")
            repository.resolveDescription("psx", "/roms/psx/game.chd")

            assertEquals(1, buildCount)
        }

    @Test
    fun `a root change between calls rebuilds the repository and reflects the new root`() =
        runTest {
            var buildCount = 0
            val root = MutableStateFlow("/storage/emulated/0/ES-DE")
            val repository =
                ReactiveGameDescriptionRepository(
                    esdeRootPath = root,
                    repositoryFactory = { path ->
                        buildCount++
                        FakeGameDescriptionRepository(path)
                    },
                )

            repository.resolveDescription("dreamcast", "/roms/dreamcast/game.chd")
            root.value = "/sdcard/ES-DE"
            val second = repository.resolveDescription("dreamcast", "/roms/dreamcast/game.chd")

            assertEquals(2, buildCount)
            assertEquals("/sdcard/ES-DE/dreamcast//roms/dreamcast/game.chd", second.text)
        }
}

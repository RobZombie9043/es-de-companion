package com.esde.companion.data.media

import com.esde.companion.domain.model.GameMedia
import com.esde.companion.domain.model.MediaType
import com.esde.companion.domain.repository.GameMediaRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReactiveGameMediaRepositoryTest {
    private class FakeGameMediaRepository(private val folder: String) : GameMediaRepository {
        override suspend fun resolveMedia(
            systemShortName: String,
            romPath: String,
            mediaTypes: Set<MediaType>,
        ): GameMedia =
            GameMedia(
                baseRelativePath = "resolved",
                filesByType = mapOf(MediaType.Covers to "$folder/$systemShortName/covers/resolved.png"),
            )
    }

    @Test
    fun `resolves to empty GameMedia when no media folder is configured yet`() =
        runTest {
            val repository =
                ReactiveGameMediaRepository(
                    mediaFolderPath = flowOf(null),
                    repositoryFactory = { folder -> FakeGameMediaRepository(folder) },
                )

            val result = repository.resolveMedia("dreamcast", "/roms/dreamcast/game.chd", setOf(MediaType.Covers))

            assertNull(result.path(MediaType.Covers))
        }

    @Test
    fun `delegates to the repository built for the currently configured media folder`() =
        runTest {
            val repository =
                ReactiveGameMediaRepository(
                    mediaFolderPath = flowOf("/sdcard/ES-DE/downloaded_media"),
                    repositoryFactory = { folder -> FakeGameMediaRepository(folder) },
                )

            val result = repository.resolveMedia("dreamcast", "/roms/dreamcast/game.chd", setOf(MediaType.Covers))

            assertEquals(
                "/sdcard/ES-DE/downloaded_media/dreamcast/covers/resolved.png",
                result.path(MediaType.Covers),
            )
        }
}

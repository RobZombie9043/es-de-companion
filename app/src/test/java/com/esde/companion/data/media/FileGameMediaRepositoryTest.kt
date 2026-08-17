package com.esde.companion.data.media

import com.esde.companion.domain.model.MediaType
import com.esde.companion.domain.repository.SystemPathRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class FileGameMediaRepositoryTest {
    private lateinit var mediaRoot: File
    private lateinit var romRoot: File

    private class FakeSystemPathRepository(
        private val paths: Map<String, String> = emptyMap(),
    ) : SystemPathRepository {
        var callCount = 0
            private set

        override suspend fun resolveSystemPath(systemShortName: String): String? {
            callCount++
            return paths[systemShortName]
        }
    }

    @Before
    fun setUp() {
        mediaRoot = Files.createTempDirectory("esde-media-test").toFile()
        romRoot = Files.createTempDirectory("esde-rom-test").toFile()
    }

    @After
    fun tearDown() {
        mediaRoot.deleteRecursively()
        romRoot.deleteRecursively()
    }

    @Test
    fun `finds a matching cover image by trying each valid extension`() =
        runTest {
            writeMediaFile("dreamcast/covers/Cosmic Smash (Japan).png")
            val repository = FileGameMediaRepository(mediaRoot.absolutePath, FakeSystemPathRepository())

            val result =
                repository.resolveMedia(
                    systemShortName = "dreamcast",
                    systemPath = null,
                    romPath = "/storage/E2AB-E84A/ROMs/dreamcast/Cosmic Smash (Japan).chd",
                    mediaTypes = setOf(MediaType.Covers),
                )

            assertEquals(
                File(mediaRoot, "dreamcast/covers/Cosmic Smash (Japan).png").absolutePath,
                result.path(MediaType.Covers),
            )
        }

    @Test
    fun `finds media nested in replicated subfolders`() =
        runTest {
            writeMediaFile("psx/screenshots/RPGs/Final Fantasy IX (USA).jpg")
            val repository = FileGameMediaRepository(mediaRoot.absolutePath, FakeSystemPathRepository())

            val result =
                repository.resolveMedia(
                    systemShortName = "psx",
                    systemPath = null,
                    romPath = "/storage/E2AB-E84A/ROMs/psx/RPGs/Final Fantasy IX (USA).chd",
                    mediaTypes = setOf(MediaType.Screenshots),
                )

            assertEquals(
                File(mediaRoot, "psx/screenshots/RPGs/Final Fantasy IX (USA).jpg").absolutePath,
                result.path(MediaType.Screenshots),
            )
        }

    @Test
    fun `treats a real directory rom path as ES-DE's directories-interpreted-as-files case`() =
        runTest {
            val gameDir = File(romRoot, "psx/Final Fantasy VII (USA).m3u")
            gameDir.mkdirs()
            File(gameDir, "Final Fantasy VII (USA).m3u").writeText("stub")
            writeMediaFile("psx/covers/Final Fantasy VII (USA).m3u.png")

            val repository = FileGameMediaRepository(mediaRoot.absolutePath, FakeSystemPathRepository())

            val result =
                repository.resolveMedia(
                    systemShortName = "psx",
                    systemPath = null,
                    romPath = gameDir.absolutePath.replace(File.separatorChar, '/'),
                    mediaTypes = setOf(MediaType.Covers),
                )

            assertEquals(
                File(mediaRoot, "psx/covers/Final Fantasy VII (USA).m3u.png").absolutePath,
                result.path(MediaType.Covers),
            )
        }

    @Test
    fun `returns no match for a type with no file on disk`() =
        runTest {
            val repository = FileGameMediaRepository(mediaRoot.absolutePath, FakeSystemPathRepository())

            val result =
                repository.resolveMedia(
                    systemShortName = "dreamcast",
                    systemPath = null,
                    romPath = "/storage/E2AB-E84A/ROMs/dreamcast/Missing Game.chd",
                    mediaTypes = setOf(MediaType.Covers),
                )

            assertNull(result.path(MediaType.Covers))
        }

    @Test
    fun `finds media via systemPath when the ROM folder isn't named after the system's shortname`() =
        runTest {
            writeMediaFile("nds/covers/dummy.png")
            val repository = FileGameMediaRepository(mediaRoot.absolutePath, FakeSystemPathRepository())

            val result =
                repository.resolveMedia(
                    systemShortName = "nds",
                    systemPath = "/storage/E2AB-E84A/ROMs/DS",
                    romPath = "/storage/E2AB-E84A/ROMs/DS/dummy.zip",
                    mediaTypes = setOf(MediaType.Covers),
                )

            assertEquals(
                File(mediaRoot, "nds/covers/dummy.png").absolutePath,
                result.path(MediaType.Covers),
            )
        }

    @Test
    fun `falls through to SystemPathRepository when neither systemPath nor the marker search resolve it`() =
        runTest {
            writeMediaFile("nds/covers/dummy.png")
            val systemPathRepository = FakeSystemPathRepository(mapOf("nds" to "/storage/E2AB-E84A/ROMs/DS"))
            val repository = FileGameMediaRepository(mediaRoot.absolutePath, systemPathRepository)

            val result =
                repository.resolveMedia(
                    systemShortName = "nds",
                    systemPath = null,
                    romPath = "/storage/E2AB-E84A/ROMs/DS/dummy.zip",
                    mediaTypes = setOf(MediaType.Covers),
                )

            assertEquals(
                File(mediaRoot, "nds/covers/dummy.png").absolutePath,
                result.path(MediaType.Covers),
            )
        }

    @Test
    fun `does not consult SystemPathRepository when systemPath is already known`() =
        runTest {
            writeMediaFile("nds/covers/dummy.png")
            val systemPathRepository = FakeSystemPathRepository(mapOf("nds" to "/storage/E2AB-E84A/ROMs/DS"))
            val repository = FileGameMediaRepository(mediaRoot.absolutePath, systemPathRepository)

            repository.resolveMedia(
                systemShortName = "nds",
                systemPath = "/storage/E2AB-E84A/ROMs/DS",
                romPath = "/storage/E2AB-E84A/ROMs/DS/dummy.zip",
                mediaTypes = setOf(MediaType.Covers),
            )

            assertEquals(0, systemPathRepository.callCount)
        }

    @Test
    fun `returns no media when all three resolution tiers miss`() =
        runTest {
            val repository = FileGameMediaRepository(mediaRoot.absolutePath, FakeSystemPathRepository())

            val result =
                repository.resolveMedia(
                    systemShortName = "nds",
                    systemPath = null,
                    romPath = "/storage/E2AB-E84A/ROMs/DS/dummy.zip",
                    mediaTypes = setOf(MediaType.Covers),
                )

            assertNull(result.path(MediaType.Covers))
        }

    private fun writeMediaFile(relativePath: String) {
        val file = File(mediaRoot, relativePath)
        file.parentFile?.mkdirs()
        file.writeText("stub")
    }
}

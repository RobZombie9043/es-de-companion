package com.esde.companion.data.gamelist

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class FileGameRomHashRepositoryTest {
    private lateinit var esdeRoot: File

    private fun repository() = FileGameRomHashRepository(GamelistFileReader(flowOf(esdeRoot.absolutePath)))

    @Before
    fun setUp() {
        esdeRoot = Files.createTempDirectory("esde-gamelist-hash-test").toFile()
    }

    @After
    fun tearDown() {
        esdeRoot.deleteRecursively()
    }

    private fun writeStandardGamelist(
        systemShortName: String,
        xml: String,
    ): File {
        val file = File(esdeRoot, "gamelists/$systemShortName/gamelist.xml")
        file.parentFile?.mkdirs()
        file.writeText(xml)
        return file
    }

    @Test
    fun `resolves a matching game's hash from the standard gamelist location`() =
        runTest {
            val file =
                writeStandardGamelist(
                    "a",
                    """
                    <gameList>
                        <game>
                            <path>./Cosmic Smash (Japan).chd</path>
                            <hash>aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa</hash>
                        </game>
                    </gameList>
                    """.trimIndent(),
                )

            val result = repository().resolveRomHash("a", "/roms/a/Cosmic Smash (Japan).chd")

            assertEquals("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", result.value)
            assertEquals(file.path, result.gamelistPath)
        }

    @Test
    fun `returns null value and null gamelistPath when no gamelist file exists at either location`() =
        runTest {
            val result = repository().resolveRomHash("a", "/roms/a/Missing.chd")

            assertNull(result.value)
            assertNull(result.gamelistPath)
        }

    @Test
    fun `reports the gamelist path even when the game has no hash`() =
        runTest {
            val file =
                writeStandardGamelist(
                    "a",
                    """
                    <gameList>
                        <game>
                            <path>./Game.chd</path>
                            <desc>No hash element at all.</desc>
                        </game>
                    </gameList>
                    """.trimIndent(),
                )

            val result = repository().resolveRomHash("a", "/roms/a/Game.chd")

            assertNull(result.value)
            assertEquals(file.path, result.gamelistPath)
        }

    @Test
    fun `falls back to the legacy ROMs-adjacent gamelist location when the standard one is absent`() =
        runTest {
            // LegacyGamelistPathResolver anchors on a "/<systemShortName>/" marker within the
            // romPath string itself - build it with forward slashes explicitly rather than via
            // File.absolutePath, which would use the host OS separator (backslash on Windows,
            // where these unit tests run) and never match the marker.
            val romsRoot = Files.createTempDirectory("esde-roms-hash-test").toFile()
            try {
                val legacy = File(romsRoot, "a/gamelist.xml")
                legacy.parentFile?.mkdirs()
                legacy.writeText(
                    """
                    <gameList>
                        <game>
                            <path>./Game.chd</path>
                            <hash>bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb</hash>
                        </game>
                    </gameList>
                    """.trimIndent(),
                )
                val romsRootUnixPath = romsRoot.absolutePath.replace('\\', '/')

                val result = repository().resolveRomHash("a", "$romsRootUnixPath/a/Game.chd")

                assertEquals("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", result.value)
            } finally {
                romsRoot.deleteRecursively()
            }
        }
}

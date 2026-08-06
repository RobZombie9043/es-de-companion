package com.esde.companion.data.gamelist

import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class FileGameDescriptionRepositoryTest {

    private lateinit var esdeRoot: File

    @Before
    fun setUp() {
        esdeRoot = Files.createTempDirectory("esde-gamelist-test").toFile()
    }

    @After
    fun tearDown() {
        esdeRoot.deleteRecursively()
    }

    private fun writeStandardGamelist(systemShortName: String, xml: String): File {
        val file = File(esdeRoot, "gamelists/$systemShortName/gamelist.xml")
        file.parentFile?.mkdirs()
        file.writeText(xml)
        return file
    }

    @Test
    fun `resolves a matching game's description from the standard gamelist location`() = runTest {
        writeStandardGamelist(
            "a",
            """
            <gameList>
                <game>
                    <path>./Cosmic Smash (Japan).chd</path>
                    <desc>A description.</desc>
                </game>
            </gameList>
            """.trimIndent(),
        )
        val repository = FileGameDescriptionRepository(esdeRoot.absolutePath)

        val description = repository.resolveDescription("a", "/roms/a/Cosmic Smash (Japan).chd")

        assertEquals("A description.", description.text)
    }

    @Test
    fun `returns null text when no gamelist file exists at either location`() = runTest {
        val repository = FileGameDescriptionRepository(esdeRoot.absolutePath)

        val description = repository.resolveDescription("a", "/roms/a/Missing.chd")

        assertNull(description.text)
    }

    @Test
    fun `reuses cached content on a second call without re-reading the file`() = runTest {
        val file = writeStandardGamelist(
            "a",
            """
            <gameList>
                <game>
                    <path>./Game.chd</path>
                    <desc>Cached description.</desc>
                </game>
            </gameList>
            """.trimIndent(),
        )
        val repository = FileGameDescriptionRepository(esdeRoot.absolutePath)

        val first = repository.resolveDescription("a", "/roms/a/Game.chd")
        // Mutating the file on disk without changing lastModified() must not affect the
        // second call - it should come from cache, not a fresh read.
        val originalLastModified = file.lastModified()
        file.writeText("not valid xml at all")
        file.setLastModified(originalLastModified)
        val second = repository.resolveDescription("a", "/roms/a/Game.chd")

        assertEquals("Cached description.", first.text)
        assertEquals("Cached description.", second.text)
    }

    @Test
    fun `a changed lastModified invalidates the cache and re-reads the file`() = runTest {
        val file = writeStandardGamelist(
            "a",
            """
            <gameList>
                <game>
                    <path>./Game.chd</path>
                    <desc>Original description.</desc>
                </game>
            </gameList>
            """.trimIndent(),
        )
        val repository = FileGameDescriptionRepository(esdeRoot.absolutePath)
        val first = repository.resolveDescription("a", "/roms/a/Game.chd")

        Thread.sleep(10)
        file.writeText(
            """
            <gameList>
                <game>
                    <path>./Game.chd</path>
                    <desc>Updated description.</desc>
                </game>
            </gameList>
            """.trimIndent(),
        )
        file.setLastModified(System.currentTimeMillis())
        val second = repository.resolveDescription("a", "/roms/a/Game.chd")

        assertEquals("Original description.", first.text)
        assertEquals("Updated description.", second.text)
    }

    @Test
    fun `falls back to the legacy ROMs-adjacent gamelist location when the standard one is absent`() = runTest {
        // LegacyGamelistPathResolver anchors on a "/<systemShortName>/" marker within the
        // romPath string itself - build it with forward slashes explicitly rather than via
        // File.absolutePath, which would use the host OS separator (backslash on Windows,
        // where these unit tests run) and never match the marker.
        val romsRoot = Files.createTempDirectory("esde-roms-test").toFile()
        try {
            val legacy = File(romsRoot, "a/gamelist.xml")
            legacy.parentFile?.mkdirs()
            legacy.writeText(
                """
                <gameList>
                    <game>
                        <path>./Game.chd</path>
                        <desc>Legacy description.</desc>
                    </game>
                </gameList>
                """.trimIndent(),
            )
            val repository = FileGameDescriptionRepository(esdeRoot.absolutePath)
            val romsRootUnixPath = romsRoot.absolutePath.replace('\\', '/')

            val description = repository.resolveDescription("a", "$romsRootUnixPath/a/Game.chd")

            assertEquals("Legacy description.", description.text)
        } finally {
            romsRoot.deleteRecursively()
        }
    }
}

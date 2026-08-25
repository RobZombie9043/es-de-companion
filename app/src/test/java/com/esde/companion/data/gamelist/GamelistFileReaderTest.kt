package com.esde.companion.data.gamelist

import com.esde.companion.domain.parser.GameListParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class GamelistFileReaderTest {
    private lateinit var rootA: File
    private lateinit var rootB: File

    @Before
    fun setUp() {
        rootA = Files.createTempDirectory("esde-gamelist-reader-a").toFile()
        rootB = Files.createTempDirectory("esde-gamelist-reader-b").toFile()
    }

    @After
    fun tearDown() {
        rootA.deleteRecursively()
        rootB.deleteRecursively()
    }

    private fun writeStandardGamelist(
        root: File,
        systemShortName: String,
        xml: String,
    ): File {
        val file = File(root, "gamelists/$systemShortName/gamelist.xml")
        file.parentFile?.mkdirs()
        file.writeText(xml)
        return file
    }

    @Test
    fun `returns null when no root is configured yet`() =
        runTest {
            val reader = GamelistFileReader(flowOf(null))

            val result = reader.read("a", "/roms/a/Game.chd")

            assertNull(result)
        }

    @Test
    fun `a second call with the same root reuses the cached content`() =
        runTest {
            val file = writeStandardGamelist(rootA, "a", sampleGamelistXml("Cached description."))
            val reader = GamelistFileReader(flowOf(rootA.absolutePath))

            val first = reader.read("a", "/roms/a/Game.chd")
            // Mutating the file on disk without changing lastModified() must not affect the
            // second call - it should come from cache, not a fresh read.
            val originalLastModified = file.lastModified()
            file.writeText("not valid xml at all")
            file.setLastModified(originalLastModified)
            val second = reader.read("a", "/roms/a/Game.chd")

            assertEquals("Cached description.", descriptionOf(first))
            assertEquals("Cached description.", descriptionOf(second))
        }

    @Test
    fun `a root change is picked up and reads from the new root's file`() =
        runTest {
            writeStandardGamelist(rootA, "a", sampleGamelistXml("From root A."))
            writeStandardGamelist(rootB, "a", sampleGamelistXml("From root B."))
            val root = MutableStateFlow(rootA.absolutePath)
            val reader = GamelistFileReader(root)

            val first = reader.read("a", "/roms/a/Game.chd")
            root.value = rootB.absolutePath
            val second = reader.read("a", "/roms/a/Game.chd")

            assertEquals("From root A.", descriptionOf(first))
            assertEquals("From root B.", descriptionOf(second))
        }

    @Test
    fun `returns null when no gamelist file exists at either location`() =
        runTest {
            val reader = GamelistFileReader(flowOf(rootA.absolutePath))

            val result = reader.read("a", "/roms/a/Missing.chd")

            assertNull(result)
        }

    private fun sampleGamelistXml(description: String) =
        """
        <gameList>
            <game>
                <path>./Game.chd</path>
                <desc>$description</desc>
            </game>
        </gameList>
        """.trimIndent()

    private fun descriptionOf(file: GamelistFile?): String? {
        return file?.let { GameListParser.findDescription(it.content, "/roms/a/Game.chd") }
    }
}

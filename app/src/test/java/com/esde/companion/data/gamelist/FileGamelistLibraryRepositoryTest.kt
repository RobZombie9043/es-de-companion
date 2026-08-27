package com.esde.companion.data.gamelist

import com.esde.companion.domain.model.GamelistSystemSummary
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class FileGamelistLibraryRepositoryTest {
    private lateinit var esdeRoot: File

    private fun repository() = FileGamelistLibraryRepository(GamelistFileReader(flowOf(esdeRoot.absolutePath)))

    @Before
    fun setUp() {
        esdeRoot = Files.createTempDirectory("esde-gamelist-library-test").toFile()
    }

    @After
    fun tearDown() {
        esdeRoot.deleteRecursively()
    }

    private fun writeStandardGamelist(
        systemShortName: String,
        xml: String,
    ) {
        val file = File(esdeRoot, "gamelists/$systemShortName/gamelist.xml")
        file.parentFile?.mkdirs()
        file.writeText(xml)
    }

    @Test
    fun `listSystems reports each system's short name and game count`() =
        runTest {
            writeStandardGamelist(
                "n64",
                """
                <gameList>
                    <game><path>./GameOne.z64</path><name>Game One</name></game>
                    <game><path>./GameTwo.z64</path><name>Game Two</name></game>
                </gameList>
                """.trimIndent(),
            )
            writeStandardGamelist(
                "gc",
                """
                <gameList>
                    <game><path>./GameOne.iso</path><name>Game One</name></game>
                </gameList>
                """.trimIndent(),
            )

            val result = repository().listSystems()

            assertEquals(
                listOf(GamelistSystemSummary("gc", 1), GamelistSystemSummary("n64", 2)),
                result,
            )
        }

    @Test
    fun `listSystems returns an empty list when no gamelists exist`() =
        runTest {
            val result = repository().listSystems()

            assertTrue(result.isEmpty())
        }

    @Test
    fun `listGames returns every game entry for a system`() =
        runTest {
            writeStandardGamelist(
                "n64",
                """
                <gameList>
                    <game><path>./GameOne.z64</path><name>Game One</name></game>
                    <game><path>./GameTwo.z64</path><name>Game Two</name></game>
                </gameList>
                """.trimIndent(),
            )

            val result = repository().listGames("n64")

            assertEquals(2, result.size)
            assertEquals("Game One", result[0].name)
            assertEquals("Game Two", result[1].name)
        }

    @Test
    fun `listGames returns an empty list for a system with no gamelist file`() =
        runTest {
            val result = repository().listGames("missing")

            assertTrue(result.isEmpty())
        }
}

package com.esde.companion.data.systems

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class FileSystemPathRepositoryTest {
    private lateinit var esdeRoot: File

    @Before
    fun setUp() {
        esdeRoot = Files.createTempDirectory("esde-systempath-test").toFile()
    }

    @After
    fun tearDown() {
        esdeRoot.deleteRecursively()
    }

    private fun writeSystemsXml(xml: String): File {
        val file = File(esdeRoot, "custom_systems/es_systems.xml")
        file.parentFile?.mkdirs()
        file.writeText(xml)
        return file
    }

    private fun writeSettingsXml(romDirectory: String) {
        val file = File(esdeRoot, "settings/es_settings.xml")
        file.parentFile?.mkdirs()
        file.writeText(
            """
            <?xml version="1.0"?>
            <config>
                <string name="ROMDirectory" value="$romDirectory" />
            </config>
            """.trimIndent(),
        )
    }

    @Test
    fun `resolves a literal absolute path with no settings file needed`() =
        runTest {
            writeSystemsXml(
                """
                <systemList>
                    <system>
                        <name>ps2</name>
                        <path>/storage/emulated/0/Roms/PS2</path>
                    </system>
                </systemList>
                """.trimIndent(),
            )
            val repository = FileSystemPathRepository(esdeRoot.absolutePath)

            val result = repository.resolveSystemPath("ps2")

            assertEquals("/storage/emulated/0/Roms/PS2", result)
        }

    @Test
    fun `substitutes ROMPATH using the settings file's ROMDirectory`() =
        runTest {
            writeSystemsXml(
                """
                <systemList>
                    <system>
                        <name>nds</name>
                        <path>%ROMPATH%/DS</path>
                    </system>
                </systemList>
                """.trimIndent(),
            )
            writeSettingsXml("/storage/E2AB-E84A/ROMs")
            val repository = FileSystemPathRepository(esdeRoot.absolutePath)

            val result = repository.resolveSystemPath("nds")

            assertEquals("/storage/E2AB-E84A/ROMs/DS", result)
        }

    @Test
    fun `returns null when custom_systems es_systems xml does not exist`() =
        runTest {
            val repository = FileSystemPathRepository(esdeRoot.absolutePath)

            val result = repository.resolveSystemPath("nds")

            assertNull(result)
        }

    @Test
    fun `returns null when the system has no entry in the file`() =
        runTest {
            writeSystemsXml(
                """
                <systemList>
                    <system>
                        <name>ps2</name>
                        <path>/storage/emulated/0/Roms/PS2</path>
                    </system>
                </systemList>
                """.trimIndent(),
            )
            val repository = FileSystemPathRepository(esdeRoot.absolutePath)

            val result = repository.resolveSystemPath("nds")

            assertNull(result)
        }

    @Test
    fun `reuses cached content on a second call without re-reading the file`() =
        runTest {
            val file =
                writeSystemsXml(
                    """
                    <systemList>
                        <system>
                            <name>ps2</name>
                            <path>/storage/emulated/0/Roms/PS2</path>
                        </system>
                    </systemList>
                    """.trimIndent(),
                )
            val repository = FileSystemPathRepository(esdeRoot.absolutePath)

            val first = repository.resolveSystemPath("ps2")
            val originalLastModified = file.lastModified()
            file.writeText("not valid xml at all")
            file.setLastModified(originalLastModified)
            val second = repository.resolveSystemPath("ps2")

            assertEquals("/storage/emulated/0/Roms/PS2", first)
            assertEquals("/storage/emulated/0/Roms/PS2", second)
        }

    @Test
    fun `a changed lastModified invalidates the cache and re-reads the file`() =
        runTest {
            val file =
                writeSystemsXml(
                    """
                    <systemList>
                        <system>
                            <name>ps2</name>
                            <path>/storage/emulated/0/Roms/PS2</path>
                        </system>
                    </systemList>
                    """.trimIndent(),
                )
            val repository = FileSystemPathRepository(esdeRoot.absolutePath)
            val first = repository.resolveSystemPath("ps2")

            Thread.sleep(10)
            file.writeText(
                """
                <systemList>
                    <system>
                        <name>ps2</name>
                        <path>/storage/emulated/0/RomsUpdated/PS2</path>
                    </system>
                </systemList>
                """.trimIndent(),
            )
            file.setLastModified(System.currentTimeMillis())
            val second = repository.resolveSystemPath("ps2")

            assertEquals("/storage/emulated/0/Roms/PS2", first)
            assertEquals("/storage/emulated/0/RomsUpdated/PS2", second)
        }
}

package com.esde.companion.data.media

import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FileSystemMediaRepositoryTest {

    private lateinit var mediaRoot: File

    @Before
    fun setUp() {
        mediaRoot = Files.createTempDirectory("esde-system-media-test").toFile()
    }

    @After
    fun tearDown() {
        mediaRoot.deleteRecursively()
    }

    @Test
    fun `returns null when the system has no fanart folder`() = runTest {
        val repository = FileSystemMediaRepository(mediaRoot.absolutePath)

        assertNull(repository.randomFanart("dreamcast"))
    }

    @Test
    fun `returns the only fanart file when exactly one exists`() = runTest {
        val file = writeFanartFile("dreamcast/fanart/Cosmic Smash (Japan).png")
        val repository = FileSystemMediaRepository(mediaRoot.absolutePath)

        assertEquals(file.absolutePath, repository.randomFanart("dreamcast"))
    }

    @Test
    fun `picks among nested subfolder fanart and ignores non-image files`() = runTest {
        val nested = writeFanartFile("psx/fanart/RPGs/Final Fantasy IX (USA).jpg")
        File(mediaRoot, "psx/fanart/RPGs/readme.txt").apply { parentFile?.mkdirs(); writeText("stub") }
        val repository = FileSystemMediaRepository(mediaRoot.absolutePath)

        assertEquals(nested.absolutePath, repository.randomFanart("psx"))
    }

    @Test
    fun `only considers fanart belonging to the requested system`() = runTest {
        writeFanartFile("dreamcast/fanart/Cosmic Smash (Japan).png")
        val psxFile = writeFanartFile("psx/fanart/Final Fantasy IX (USA).jpg")
        val repository = FileSystemMediaRepository(mediaRoot.absolutePath)

        assertEquals(psxFile.absolutePath, repository.randomFanart("psx"))
    }

    private fun writeFanartFile(relativePath: String): File {
        val file = File(mediaRoot, relativePath)
        file.parentFile?.mkdirs()
        file.writeText("stub")
        return file
    }
}
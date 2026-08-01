package com.esde.companion.data.music

import com.esde.companion.domain.model.MusicPool
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FileMusicLibraryRepositoryTest {

    private lateinit var musicRoot: File

    @Before
    fun setUp() {
        musicRoot = Files.createTempDirectory("esde-music-test").toFile()
    }

    @After
    fun tearDown() {
        musicRoot.deleteRecursively()
    }

    @Test
    fun `general pool includes root-level tracks and arbitrarily nested non-systems folders`() = runTest {
        val rootTrack = writeTrackFile("Lobby Theme.mp3")
        val nestedTrack = writeTrackFile("Ambient/Chill/Late Night.ogg")
        val repository = FileMusicLibraryRepository(musicRoot.absolutePath)

        val contents = repository.loadPool(MusicPool.General)

        assertEquals(MusicPool.General, contents.pool)
        val paths = contents.tracks.map { it.filePath }
        assertTrue(paths.contains(rootTrack.absolutePath))
        assertTrue(paths.contains(nestedTrack.absolutePath))
    }

    @Test
    fun `general pool excludes the systems folder entirely`() = runTest {
        writeTrackFile("Lobby Theme.mp3")
        writeTrackFile("systems/dreamcast/Boot.mp3")
        val repository = FileMusicLibraryRepository(musicRoot.absolutePath)

        val contents = repository.loadPool(MusicPool.General)

        assertEquals(1, contents.tracks.size)
    }

    @Test
    fun `general pool ignores non-audio files`() = runTest {
        writeTrackFile("Lobby Theme.mp3")
        File(musicRoot, "readme.txt").writeText("stub")
        val repository = FileMusicLibraryRepository(musicRoot.absolutePath)

        val contents = repository.loadPool(MusicPool.General)

        assertEquals(1, contents.tracks.size)
    }

    @Test
    fun `per-system pool never touches general-pool or other systems' files`() = runTest {
        writeTrackFile("Lobby Theme.mp3")
        val dreamcastTrack = writeTrackFile("systems/dreamcast/Boot.mp3")
        writeTrackFile("systems/psx/Boot.mp3")
        val repository = FileMusicLibraryRepository(musicRoot.absolutePath)

        val contents = repository.loadPool(MusicPool.PerSystem("dreamcast"))

        assertEquals(MusicPool.PerSystem("dreamcast"), contents.pool)
        assertEquals(listOf(dreamcastTrack.absolutePath), contents.tracks.map { it.filePath })
    }

    @Test
    fun `per-system pool with no tracks falls back to the general pool`() = runTest {
        val generalTrack = writeTrackFile("Lobby Theme.mp3")
        val repository = FileMusicLibraryRepository(musicRoot.absolutePath)

        val contents = repository.loadPool(MusicPool.PerSystem("dreamcast"))

        assertEquals(MusicPool.General, contents.pool)
        assertEquals(listOf(generalTrack.absolutePath), contents.tracks.map { it.filePath })
    }

    private fun writeTrackFile(relativePath: String): File {
        val file = File(musicRoot, relativePath)
        file.parentFile?.mkdirs()
        file.writeText("stub")
        return file
    }
}

package com.esde.companion.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameMediaPathResolverTest {

    @Test
    fun `strips the rom extension for a simple top-level rom file`() {
        val result = GameMediaPathResolver.resolveBaseRelativePath(
            systemShortName = "dreamcast",
            romPath = "/storage/E2AB-E84A/ROMs/dreamcast/Dead or Alive 2 (USA).chd",
            romIsDirectory = false,
        )

        assertEquals("Dead or Alive 2 (USA)", result)
    }

    @Test
    fun `preserves subfolder structure beneath the system folder for a rom file`() {
        val result = GameMediaPathResolver.resolveBaseRelativePath(
            systemShortName = "psx",
            romPath = "/storage/E2AB-E84A/ROMs/psx/RPGs/Working/Final Fantasy IX (USA).chd",
            romIsDirectory = false,
        )

        assertEquals("RPGs/Working/Final Fantasy IX (USA)", result)
    }

    @Test
    fun `strips the extension of an ordinary m3u playlist file, since it is a real file not a directory`() {
        val result = GameMediaPathResolver.resolveBaseRelativePath(
            systemShortName = "psx",
            romPath = "/storage/E2AB-E84A/ROMs/psx/Chrono Cross (USA).m3u",
            romIsDirectory = false,
        )

        assertEquals("Chrono Cross (USA)", result)
    }

    @Test
    fun `keeps the extension for ES-DE directories-interpreted-as-files, since it is the folder's literal name`() {
        val result = GameMediaPathResolver.resolveBaseRelativePath(
            systemShortName = "psx",
            romPath = "/storage/E2AB-E84A/ROMs/psx/Final Fantasy VII (USA).m3u",
            romIsDirectory = true,
        )

        assertEquals("Final Fantasy VII (USA).m3u", result)
    }

    @Test
    fun `keeps the extension for directories-interpreted-as-files nested inside subfolders`() {
        val result = GameMediaPathResolver.resolveBaseRelativePath(
            systemShortName = "psx",
            romPath = "/storage/E2AB-E84A/ROMs/psx/RPGs/Chrono Cross (USA).m3u",
            romIsDirectory = true,
        )

        assertEquals("RPGs/Chrono Cross (USA).m3u", result)
    }

    @Test
    fun `rom with no extension is used as-is`() {
        val result = GameMediaPathResolver.resolveBaseRelativePath(
            systemShortName = "arcade",
            romPath = "/storage/E2AB-E84A/ROMs/arcade/tapper",
            romIsDirectory = false,
        )

        assertEquals("tapper", result)
    }

    @Test
    fun `returns null when the system folder segment can't be found in the rom path`() {
        val result = GameMediaPathResolver.resolveBaseRelativePath(
            systemShortName = "psx",
            romPath = "/storage/E2AB-E84A/ROMs/dreamcast/game.chd",
            romIsDirectory = false,
        )

        assertNull(result)
    }
}
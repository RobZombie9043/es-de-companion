package com.esde.companion.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameMediaPathResolverTest {
    @Test
    fun `strips the rom extension for a simple top-level rom file`() {
        val result =
            GameMediaPathResolver.resolveBaseRelativePath(
                systemShortName = "dreamcast",
                systemPath = null,
                romPath = "/storage/E2AB-E84A/ROMs/dreamcast/Dead or Alive 2 (USA).chd",
                romIsDirectory = false,
            )

        assertEquals("Dead or Alive 2 (USA)", result)
    }

    @Test
    fun `preserves subfolder structure beneath the system folder for a rom file`() {
        val result =
            GameMediaPathResolver.resolveBaseRelativePath(
                systemShortName = "psx",
                systemPath = null,
                romPath = "/storage/E2AB-E84A/ROMs/psx/RPGs/Working/Final Fantasy IX (USA).chd",
                romIsDirectory = false,
            )

        assertEquals("RPGs/Working/Final Fantasy IX (USA)", result)
    }

    @Test
    fun `strips the extension of an ordinary m3u playlist file, since it is a real file not a directory`() {
        val result =
            GameMediaPathResolver.resolveBaseRelativePath(
                systemShortName = "psx",
                systemPath = null,
                romPath = "/storage/E2AB-E84A/ROMs/psx/Chrono Cross (USA).m3u",
                romIsDirectory = false,
            )

        assertEquals("Chrono Cross (USA)", result)
    }

    @Test
    fun `keeps the extension for ES-DE directories-interpreted-as-files, since it is the folder's literal name`() {
        val result =
            GameMediaPathResolver.resolveBaseRelativePath(
                systemShortName = "psx",
                systemPath = null,
                romPath = "/storage/E2AB-E84A/ROMs/psx/Final Fantasy VII (USA).m3u",
                romIsDirectory = true,
            )

        assertEquals("Final Fantasy VII (USA).m3u", result)
    }

    @Test
    fun `keeps the extension for directories-interpreted-as-files nested inside subfolders`() {
        val result =
            GameMediaPathResolver.resolveBaseRelativePath(
                systemShortName = "psx",
                systemPath = null,
                romPath = "/storage/E2AB-E84A/ROMs/psx/RPGs/Chrono Cross (USA).m3u",
                romIsDirectory = true,
            )

        assertEquals("RPGs/Chrono Cross (USA).m3u", result)
    }

    @Test
    fun `rom with no extension is used as-is`() {
        val result =
            GameMediaPathResolver.resolveBaseRelativePath(
                systemShortName = "arcade",
                systemPath = null,
                romPath = "/storage/E2AB-E84A/ROMs/arcade/tapper",
                romIsDirectory = false,
            )

        assertEquals("tapper", result)
    }

    @Test
    fun `returns null when neither systemPath nor the system folder segment can resolve the rom path`() {
        val result =
            GameMediaPathResolver.resolveBaseRelativePath(
                systemShortName = "psx",
                systemPath = null,
                romPath = "/storage/E2AB-E84A/ROMs/dreamcast/game.chd",
                romIsDirectory = false,
            )

        assertNull(result)
    }

    @Test
    fun `prefers systemPath over the shortname marker when a custom ROM folder name doesn't match the shortname`() {
        // Real reported setup: DS roms stored under a "DS" folder while the system's actual
        // shortname is "nds" - no "/nds/" marker exists anywhere in the rom path, so only
        // ES-DE's own reported systemPath (from system-select) can resolve this.
        val result =
            GameMediaPathResolver.resolveBaseRelativePath(
                systemShortName = "nds",
                systemPath = "/storage/E2AB-E84A/ROMs/DS",
                romPath = "/storage/E2AB-E84A/ROMs/DS/dummy.zip",
                romIsDirectory = false,
            )

        assertEquals("dummy", result)
    }

    @Test
    fun `preserves subfolder structure beneath a custom-named systemPath`() {
        val result =
            GameMediaPathResolver.resolveBaseRelativePath(
                systemShortName = "nds",
                systemPath = "/storage/E2AB-E84A/ROMs/DS",
                romPath = "/storage/E2AB-E84A/ROMs/DS/Action/dummy.zip",
                romIsDirectory = false,
            )

        assertEquals("Action/dummy", result)
    }

    @Test
    fun `falls back to the shortname marker when romPath doesn't actually sit under systemPath`() {
        // A stale/mismatched systemPath (e.g. from a different system entirely) should not
        // block the marker fallback from still finding a match.
        val result =
            GameMediaPathResolver.resolveBaseRelativePath(
                systemShortName = "dreamcast",
                systemPath = "/storage/E2AB-E84A/ROMs/gamegear",
                romPath = "/storage/E2AB-E84A/ROMs/dreamcast/Dead or Alive 2 (USA).chd",
                romIsDirectory = false,
            )

        assertEquals("Dead or Alive 2 (USA)", result)
    }

    @Test
    fun `matches the shortname marker case-insensitively`() {
        // Real reported setup: PS2 roms overridden onto internal storage under a "PS2"
        // folder while the shortname is "ps2" - Android's filesystem is case-sensitive, so
        // an exact-case marker search misses this even though it's the same folder by
        // ES-DE's own (case-insensitive) convention.
        val result =
            GameMediaPathResolver.resolveBaseRelativePath(
                systemShortName = "ps2",
                systemPath = null,
                romPath = "/storage/emulated/0/Roms/PS2/dummy.iso",
                romIsDirectory = false,
            )

        assertEquals("dummy", result)
    }
}

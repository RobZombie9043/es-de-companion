package com.esde.companion.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LegacyGamelistPathResolverTest {
    @Test
    fun `resolves the system folder's gamelist for a top-level rom file`() {
        val result =
            LegacyGamelistPathResolver.resolvePath(
                systemShortName = "dreamcast",
                romPath = "/storage/E2AB-E84A/ROMs/dreamcast/Dead or Alive 2 (USA).chd",
            )

        assertEquals("/storage/E2AB-E84A/ROMs/dreamcast/gamelist.xml", result)
    }

    @Test
    fun `resolves the system folder's gamelist for a rom nested in a subfolder`() {
        val result =
            LegacyGamelistPathResolver.resolvePath(
                systemShortName = "psx",
                romPath = "/storage/E2AB-E84A/ROMs/psx/RPGs/Final Fantasy IX (USA).chd",
            )

        assertEquals("/storage/E2AB-E84A/ROMs/psx/gamelist.xml", result)
    }

    @Test
    fun `returns null when the system folder segment can't be found in the rom path`() {
        val result =
            LegacyGamelistPathResolver.resolvePath(
                systemShortName = "psx",
                romPath = "/storage/E2AB-E84A/ROMs/dreamcast/game.chd",
            )

        assertNull(result)
    }
}

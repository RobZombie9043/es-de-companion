package com.esde.companion.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EsdeSystemToRaConsoleMappingTest {
    @Test
    fun `maps well-known ES-DE system short names to their RetroAchievements console`() {
        assertEquals(RetroAchievementsConsole.MegaDrive, EsdeSystemToRaConsoleMapping.consoleFor("genesis"))
        assertEquals(RetroAchievementsConsole.Snes, EsdeSystemToRaConsoleMapping.consoleFor("snes"))
        assertEquals(RetroAchievementsConsole.Nes, EsdeSystemToRaConsoleMapping.consoleFor("nes"))
        assertEquals(RetroAchievementsConsole.PlayStation, EsdeSystemToRaConsoleMapping.consoleFor("psx"))
        assertEquals(RetroAchievementsConsole.GameBoyAdvance, EsdeSystemToRaConsoleMapping.consoleFor("gba"))
    }

    @Test
    fun `regional variants of the same physical console map to the same RetroAchievements console`() {
        assertEquals(RetroAchievementsConsole.MegaDrive, EsdeSystemToRaConsoleMapping.consoleFor("megadrive"))
        assertEquals(RetroAchievementsConsole.MegaDrive, EsdeSystemToRaConsoleMapping.consoleFor("megadrivejp"))
        assertEquals(RetroAchievementsConsole.MegaDrive, EsdeSystemToRaConsoleMapping.consoleFor("genesis"))
    }

    @Test
    fun `an unmapped or unknown system short name returns null`() {
        assertNull(EsdeSystemToRaConsoleMapping.consoleFor("does-not-exist"))
        assertNull(EsdeSystemToRaConsoleMapping.consoleFor("windows"))
    }

    @Test
    fun `Nintendo DS maps to its RetroAchievements console`() {
        assertEquals(RetroAchievementsConsole.NintendoDs, EsdeSystemToRaConsoleMapping.consoleFor("nds"))
    }

    @Test
    fun `a representative sample of the expanded system table maps correctly`() {
        assertEquals(RetroAchievementsConsole.GameCube, EsdeSystemToRaConsoleMapping.consoleFor("gc"))
        assertEquals(RetroAchievementsConsole.Wii, EsdeSystemToRaConsoleMapping.consoleFor("wii"))
        assertEquals(RetroAchievementsConsole.PlayStation2, EsdeSystemToRaConsoleMapping.consoleFor("ps2"))
        assertEquals(RetroAchievementsConsole.Psp, EsdeSystemToRaConsoleMapping.consoleFor("psp"))
        assertEquals(RetroAchievementsConsole.Dreamcast, EsdeSystemToRaConsoleMapping.consoleFor("dreamcast"))
        assertEquals(RetroAchievementsConsole.Saturn, EsdeSystemToRaConsoleMapping.consoleFor("saturn"))
        assertEquals(RetroAchievementsConsole.Amiga, EsdeSystemToRaConsoleMapping.consoleFor("amiga1200"))
        assertEquals(RetroAchievementsConsole.Msx, EsdeSystemToRaConsoleMapping.consoleFor("msx2"))
        assertEquals(RetroAchievementsConsole.Commodore64, EsdeSystemToRaConsoleMapping.consoleFor("c64"))
    }

    @Test
    fun `MAME-arcade systems are deliberately left unmapped`() {
        assertNull(EsdeSystemToRaConsoleMapping.consoleFor("arcade"))
        assertNull(EsdeSystemToRaConsoleMapping.consoleFor("mame"))
        assertNull(EsdeSystemToRaConsoleMapping.consoleFor("neogeo"))
        assertNull(EsdeSystemToRaConsoleMapping.consoleFor("fbneo"))
    }
}

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
}

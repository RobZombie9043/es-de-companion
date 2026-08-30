package com.esde.companion.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameReferenceTest {
    @Test
    fun `identical references identify each other`() {
        val reference = GameReference("gc", "/roms/gc/game.iso", "/roms/gc")

        assertTrue(reference.identifies(reference.copy()))
    }

    @Test
    fun `differing systemPath is ignored`() {
        val stored = GameReference("gc", "/roms/gc/game.iso", systemPath = null)
        val live = GameReference("gc", "/roms/gc/game.iso", systemPath = "/roms/gc")

        assertTrue(stored.identifies(live))
        assertTrue(live.identifies(stored))
    }

    @Test
    fun `a different romPath does not identify`() {
        val a = GameReference("gc", "/roms/gc/game-a.iso", "/roms/gc")
        val b = GameReference("gc", "/roms/gc/game-b.iso", "/roms/gc")

        assertFalse(a.identifies(b))
    }

    @Test
    fun `a different systemShortName does not identify`() {
        val a = GameReference("gc", "/roms/game.iso", "/roms/gc")
        val b = GameReference("wii", "/roms/game.iso", "/roms/gc")

        assertFalse(a.identifies(b))
    }

    @Test
    fun `a gamelist-relative romPath identifies the same absolute romPath`() {
        val addedFromGamelist = GameReference("gc", "./Metroid Prime.iso")
        val live = GameReference("gc", "/storage/emulated/0/ROMs/gc/Metroid Prime.iso", "/storage/emulated/0/ROMs/gc")

        assertTrue(addedFromGamelist.identifies(live))
        assertTrue(live.identifies(addedFromGamelist))
    }

    @Test
    fun `a gamelist-relative romPath does not identify an unrelated absolute romPath`() {
        val addedFromGamelist = GameReference("gc", "./Metroid Prime.iso")
        val live = GameReference("gc", "/storage/emulated/0/ROMs/gc/Other Game.iso")

        assertFalse(addedFromGamelist.identifies(live))
    }
}

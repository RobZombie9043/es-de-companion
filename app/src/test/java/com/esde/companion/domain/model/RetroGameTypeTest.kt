package com.esde.companion.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RetroGameTypeTest {
    @Test
    fun `an untagged title is Retail`() {
        assertEquals(setOf(RetroGameType.Retail), "Chrono Trigger".retroGameTypes())
    }

    @Test
    fun `each tag maps to its own game type`() {
        assertEquals(setOf(RetroGameType.Hack), "~Hack~ Knuckles the Echidna in Sonic the Hedgehog".retroGameTypes())
        assertEquals(setOf(RetroGameType.Homebrew), "~Homebrew~ Some Fan Game".retroGameTypes())
        assertEquals(setOf(RetroGameType.Prototype), "~Prototype~ Unreleased Build".retroGameTypes())
        assertEquals(setOf(RetroGameType.Unlicensed), "~Unlicensed~ Bootleg Cart".retroGameTypes())
        assertEquals(setOf(RetroGameType.Demo), "~Demo~ Trade Show Build".retroGameTypes())
    }

    @Test
    fun `tag matching is case-insensitive`() {
        assertEquals(setOf(RetroGameType.Hack), "~hack~ Some Rom Hack".retroGameTypes())
    }

    @Test
    fun `a title carrying multiple tags returns every matching type`() {
        val types = "~Homebrew~ ~Demo~ Foo".retroGameTypes()

        assertEquals(setOf(RetroGameType.Homebrew, RetroGameType.Demo), types)
    }

    @Test
    fun `Retail is never returned alongside another matched tag`() {
        val types = "~Hack~ Foo".retroGameTypes()

        assertEquals(setOf(RetroGameType.Hack), types)
    }
}

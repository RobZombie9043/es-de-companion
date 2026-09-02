package com.esde.companion.domain.gameguides

import org.junit.Assert.assertEquals
import org.junit.Test

class GuideTitleCleanerTest {
    @Test
    fun `strips game name and dash separator when both are present`() {
        val raw = "Persona 4 Golden - Guide and Walkthrough - PlayStation Vita - By Zoel - GameFAQs"

        assertEquals(
            "Guide and Walkthrough - PlayStation Vita - By Zoel",
            GuideTitleCleaner.clean(raw, "Persona 4 Golden"),
        )
    }

    @Test
    fun `strips game name concatenated with no separator`() {
        val raw = "Metroid Prime Walkthrough & Guide - GameCube - By jken324 - GameFAQs"

        assertEquals(
            "Walkthrough & Guide - GameCube - By jken324",
            GuideTitleCleaner.clean(raw, "Metroid Prime"),
        )
    }

    @Test
    fun `matches the game name case-insensitively`() {
        val raw = "METROID PRIME Walkthrough & Guide - GameFAQs"

        assertEquals("Walkthrough & Guide", GuideTitleCleaner.clean(raw, "metroid prime"))
    }

    @Test
    fun `still strips the trailing GameFAQs suffix when the game name does not prefix the title`() {
        val raw = "Some Guide Title - GameFAQs"

        assertEquals("Some Guide Title", GuideTitleCleaner.clean(raw, "A Totally Different Game"))
    }

    @Test
    fun `still strips the trailing GameFAQs suffix when the game name is blank`() {
        val raw = "Persona 4 Golden - Guide and Walkthrough - GameFAQs"

        assertEquals("Persona 4 Golden - Guide and Walkthrough", GuideTitleCleaner.clean(raw, ""))
    }

    @Test
    fun `falls back to the raw title when stripping would leave nothing`() {
        val raw = "Persona 4 Golden"

        assertEquals(raw, GuideTitleCleaner.clean(raw, "Persona 4 Golden"))
    }
}

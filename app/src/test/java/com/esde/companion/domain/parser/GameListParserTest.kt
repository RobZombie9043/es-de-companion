package com.esde.companion.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameListParserTest {
    @Test
    fun `matches a top-level game path and returns its description`() {
        val xml =
            """
            <gameList>
              <game>
                <path>./Game.zip</path>
                <desc>A great game.</desc>
              </game>
            </gameList>
            """.trimIndent()

        val result = GameListParser.findDescription(xml, "/roms/gc/Game.zip")

        assertEquals("A great game.", result)
    }

    @Test
    fun `matches a subfolder game path without false-positive matching a same-named game in a different subfolder`() {
        val xml =
            """
            <gameList>
              <game>
                <path>./disc1/Name.zip</path>
                <desc>Disc 1 description.</desc>
              </game>
              <game>
                <path>./disc2/Name.zip</path>
                <desc>Disc 2 description.</desc>
              </game>
            </gameList>
            """.trimIndent()

        val result = GameListParser.findDescription(xml, "/roms/gc/disc2/Name.zip")

        assertEquals("Disc 2 description.", result)
    }

    @Test
    fun `trims the returned description text`() {
        val xml =
            """
            <gameList>
              <game>
                <path>./Game.zip</path>
                <desc>
                  Padded description.
                </desc>
              </game>
            </gameList>
            """.trimIndent()

        val result = GameListParser.findDescription(xml, "/roms/gc/Game.zip")

        assertEquals("Padded description.", result)
    }

    @Test
    fun `returns null for a blank description`() {
        val xml =
            """
            <gameList>
              <game>
                <path>./Game.zip</path>
                <desc>   </desc>
              </game>
            </gameList>
            """.trimIndent()

        val result = GameListParser.findDescription(xml, "/roms/gc/Game.zip")

        assertNull(result)
    }

    @Test
    fun `returns null when no game path matches`() {
        val xml =
            """
            <gameList>
              <game>
                <path>./OtherGame.zip</path>
                <desc>Not this one.</desc>
              </game>
            </gameList>
            """.trimIndent()

        val result = GameListParser.findDescription(xml, "/roms/gc/Game.zip")

        assertNull(result)
    }

    @Test
    fun `returns null instead of throwing on malformed XML`() {
        val xml =
            """
            <gameList>
              <game>
                <path>./Game.zip</path>
                <desc>Tom & Jerry</desc>
              </game>
            </gameList>
            """.trimIndent()

        val result = GameListParser.findDescription(xml, "/roms/gc/Game.zip")

        assertNull(result)
    }

    @Test
    fun `slices out just the gameList element when a sibling alternativeEmulator precedes it at document root`() {
        val xml =
            """
            <alternativeEmulator>
              <label>RetroArch</label>
            </alternativeEmulator>
            <gameList>
              <game>
                <path>./Game.zip</path>
                <desc>A great game.</desc>
              </game>
            </gameList>
            """.trimIndent()

        val result = GameListParser.findDescription(xml, "/roms/gc/Game.zip")

        assertEquals("A great game.", result)
    }

    @Test
    fun `returns null when there is no gameList tag at all`() {
        val xml = "<somethingElse></somethingElse>"

        val result = GameListParser.findDescription(xml, "/roms/gc/Game.zip")

        assertNull(result)
    }

    @Test
    fun `returns null when gameList is never closed`() {
        val xml =
            """
            <gameList>
              <game>
                <path>./Game.zip</path>
                <desc>A great game.</desc>
              </game>
            """.trimIndent()

        val result = GameListParser.findDescription(xml, "/roms/gc/Game.zip")

        assertNull(result)
    }

    @Test
    fun `findRomHash matches the right game's hash among several`() {
        val xml =
            """
            <gameList>
              <game>
                <path>./GameOne.zip</path>
                <hash>aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa</hash>
              </game>
              <game>
                <path>./GameTwo.zip</path>
                <hash>bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb</hash>
              </game>
            </gameList>
            """.trimIndent()

        val result = GameListParser.findRomHash(xml, "/roms/gc/GameTwo.zip")

        assertEquals("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", result)
    }

    @Test
    fun `findRomHash returns null for a game with no hash element`() {
        val xml =
            """
            <gameList>
              <game>
                <path>./Game.zip</path>
                <desc>A great game.</desc>
              </game>
            </gameList>
            """.trimIndent()

        val result = GameListParser.findRomHash(xml, "/roms/gc/Game.zip")

        assertNull(result)
    }

    @Test
    fun `findRomHash returns null for a blank hash element`() {
        val xml =
            """
            <gameList>
              <game>
                <path>./Game.zip</path>
                <hash>   </hash>
              </game>
            </gameList>
            """.trimIndent()

        val result = GameListParser.findRomHash(xml, "/roms/gc/Game.zip")

        assertNull(result)
    }

    @Test
    fun `findRomHash is found alongside a sibling alternativeEmulator element`() {
        val xml =
            """
            <alternativeEmulator>
              <label>RetroArch</label>
            </alternativeEmulator>
            <gameList>
              <game>
                <path>./Game.zip</path>
                <hash>aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa</hash>
              </game>
            </gameList>
            """.trimIndent()

        val result = GameListParser.findRomHash(xml, "/roms/gc/Game.zip")

        assertEquals("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", result)
    }

    @Test
    fun `findRomHash matches a subfolder game path via the suffix rule`() {
        val xml =
            """
            <gameList>
              <game>
                <path>./disc1/Name.zip</path>
                <hash>aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa</hash>
              </game>
              <game>
                <path>./disc2/Name.zip</path>
                <hash>bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb</hash>
              </game>
            </gameList>
            """.trimIndent()

        val result = GameListParser.findRomHash(xml, "/roms/gc/disc2/Name.zip")

        assertEquals("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", result)
    }

    @Test
    fun `findRomHash returns null instead of throwing on malformed XML`() {
        val xml =
            """
            <gameList>
              <game>
                <path>./Game.zip</path>
                <hash>Tom & Jerry</hash>
              </game>
            </gameList>
            """.trimIndent()

        val result = GameListParser.findRomHash(xml, "/roms/gc/Game.zip")

        assertNull(result)
    }
}

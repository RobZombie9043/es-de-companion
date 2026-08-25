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

    @Test
    fun `findRating matches the right game's rating among several`() {
        val xml =
            """
            <gameList>
              <game>
                <path>./GameOne.zip</path>
                <rating>0.200000</rating>
              </game>
              <game>
                <path>./GameTwo.zip</path>
                <rating>0.900000</rating>
              </game>
            </gameList>
            """.trimIndent()

        val result = GameListParser.findRating(xml, "/roms/gc/GameTwo.zip")

        assertEquals(0.9f, result)
    }

    @Test
    fun `findRating matches a subfolder game path via the suffix rule`() {
        val xml =
            """
            <gameList>
              <game>
                <path>./disc1/Name.zip</path>
                <rating>0.200000</rating>
              </game>
              <game>
                <path>./disc2/Name.zip</path>
                <rating>0.900000</rating>
              </game>
            </gameList>
            """.trimIndent()

        val result = GameListParser.findRating(xml, "/roms/gc/disc2/Name.zip")

        assertEquals(0.9f, result)
    }

    @Test
    fun `findRating clamps an out-of-range rating into 0f to 1f`() {
        val xml =
            """
            <gameList>
              <game>
                <path>./Game.zip</path>
                <rating>1.500000</rating>
              </game>
            </gameList>
            """.trimIndent()

        val result = GameListParser.findRating(xml, "/roms/gc/Game.zip")

        assertEquals(1f, result)
    }

    @Test
    fun `findRating returns null for a blank rating`() {
        val xml =
            """
            <gameList>
              <game>
                <path>./Game.zip</path>
                <rating>   </rating>
              </game>
            </gameList>
            """.trimIndent()

        val result = GameListParser.findRating(xml, "/roms/gc/Game.zip")

        assertNull(result)
    }

    @Test
    fun `findRating returns null for a non-numeric rating`() {
        val xml =
            """
            <gameList>
              <game>
                <path>./Game.zip</path>
                <rating>not-a-number</rating>
              </game>
            </gameList>
            """.trimIndent()

        val result = GameListParser.findRating(xml, "/roms/gc/Game.zip")

        assertNull(result)
    }

    @Test
    fun `findRating returns null for a game with no rating element`() {
        val xml =
            """
            <gameList>
              <game>
                <path>./Game.zip</path>
                <desc>A great game.</desc>
              </game>
            </gameList>
            """.trimIndent()

        val result = GameListParser.findRating(xml, "/roms/gc/Game.zip")

        assertNull(result)
    }

    @Test
    fun `findRating returns null when no game path matches`() {
        val xml =
            """
            <gameList>
              <game>
                <path>./OtherGame.zip</path>
                <rating>0.500000</rating>
              </game>
            </gameList>
            """.trimIndent()

        val result = GameListParser.findRating(xml, "/roms/gc/Game.zip")

        assertNull(result)
    }

    @Test
    fun `findRating is found alongside a sibling alternativeEmulator element`() {
        val xml =
            """
            <alternativeEmulator>
              <label>RetroArch</label>
            </alternativeEmulator>
            <gameList>
              <game>
                <path>./Game.zip</path>
                <rating>0.700000</rating>
              </game>
            </gameList>
            """.trimIndent()

        val result = GameListParser.findRating(xml, "/roms/gc/Game.zip")

        assertEquals(0.7f, result)
    }
}

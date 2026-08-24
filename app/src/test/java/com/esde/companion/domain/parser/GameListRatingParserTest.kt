package com.esde.companion.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameListRatingParserTest {
    @Test
    fun `matches a top-level game path and returns its rating`() {
        val xml =
            """
            <gameList>
              <game>
                <path>./Game.zip</path>
                <rating>0.800000</rating>
              </game>
            </gameList>
            """.trimIndent()

        val result = GameListRatingParser.findRating(xml, "/roms/gc/Game.zip")

        assertEquals(0.8f, result)
    }

    @Test
    fun `matches a subfolder game path without false-positive matching a same-named game in a different subfolder`() {
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

        val result = GameListRatingParser.findRating(xml, "/roms/gc/disc2/Name.zip")

        assertEquals(0.9f, result)
    }

    @Test
    fun `clamps an out-of-range rating into 0f to 1f`() {
        val xml =
            """
            <gameList>
              <game>
                <path>./Game.zip</path>
                <rating>1.500000</rating>
              </game>
            </gameList>
            """.trimIndent()

        val result = GameListRatingParser.findRating(xml, "/roms/gc/Game.zip")

        assertEquals(1f, result)
    }

    @Test
    fun `returns null for a blank rating`() {
        val xml =
            """
            <gameList>
              <game>
                <path>./Game.zip</path>
                <rating>   </rating>
              </game>
            </gameList>
            """.trimIndent()

        val result = GameListRatingParser.findRating(xml, "/roms/gc/Game.zip")

        assertNull(result)
    }

    @Test
    fun `returns null for a non-numeric rating`() {
        val xml =
            """
            <gameList>
              <game>
                <path>./Game.zip</path>
                <rating>not-a-number</rating>
              </game>
            </gameList>
            """.trimIndent()

        val result = GameListRatingParser.findRating(xml, "/roms/gc/Game.zip")

        assertNull(result)
    }

    @Test
    fun `returns null when the game has no rating tag at all`() {
        val xml =
            """
            <gameList>
              <game>
                <path>./Game.zip</path>
              </game>
            </gameList>
            """.trimIndent()

        val result = GameListRatingParser.findRating(xml, "/roms/gc/Game.zip")

        assertNull(result)
    }

    @Test
    fun `returns null when no game path matches`() {
        val xml =
            """
            <gameList>
              <game>
                <path>./OtherGame.zip</path>
                <rating>0.500000</rating>
              </game>
            </gameList>
            """.trimIndent()

        val result = GameListRatingParser.findRating(xml, "/roms/gc/Game.zip")

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
                <rating>0.700000</rating>
              </game>
            </gameList>
            """.trimIndent()

        val result = GameListRatingParser.findRating(xml, "/roms/gc/Game.zip")

        assertEquals(0.7f, result)
    }

    @Test
    fun `returns null when there is no gameList tag at all`() {
        val xml = "<somethingElse></somethingElse>"

        val result = GameListRatingParser.findRating(xml, "/roms/gc/Game.zip")

        assertNull(result)
    }

    @Test
    fun `returns null when gameList is never closed`() {
        val xml =
            """
            <gameList>
              <game>
                <path>./Game.zip</path>
                <rating>0.500000</rating>
              </game>
            """.trimIndent()

        val result = GameListRatingParser.findRating(xml, "/roms/gc/Game.zip")

        assertNull(result)
    }
}

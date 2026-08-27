package com.esde.companion.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GamelistBulkParserTest {
    @Test
    fun `parses every game in the file`() {
        val xml =
            """
            <gameList>
              <game>
                <path>./GameOne.zip</path>
                <name>Game One</name>
              </game>
              <game>
                <path>./GameTwo.zip</path>
                <name>Game Two</name>
              </game>
            </gameList>
            """.trimIndent()

        val result = GamelistBulkParser.parseAllGames(xml)

        assertEquals(
            listOf(
                GamelistGameEntry("./GameOne.zip", "Game One"),
                GamelistGameEntry("./GameTwo.zip", "Game Two"),
            ),
            result,
        )
    }

    @Test
    fun `skips a game with no path element`() {
        val xml =
            """
            <gameList>
              <game>
                <name>No Path</name>
              </game>
              <game>
                <path>./GameTwo.zip</path>
                <name>Game Two</name>
              </game>
            </gameList>
            """.trimIndent()

        val result = GamelistBulkParser.parseAllGames(xml)

        assertEquals(listOf(GamelistGameEntry("./GameTwo.zip", "Game Two")), result)
    }

    @Test
    fun `skips a game with a blank path element`() {
        val xml =
            """
            <gameList>
              <game>
                <path>   </path>
                <name>Blank Path</name>
              </game>
            </gameList>
            """.trimIndent()

        val result = GamelistBulkParser.parseAllGames(xml)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `falls back to the path when name is missing`() {
        val xml =
            """
            <gameList>
              <game>
                <path>./subfolder/Game.zip</path>
              </game>
            </gameList>
            """.trimIndent()

        val result = GamelistBulkParser.parseAllGames(xml)

        assertEquals(listOf(GamelistGameEntry("./subfolder/Game.zip", "subfolder/Game.zip")), result)
    }

    @Test
    fun `falls back to the path when name is blank`() {
        val xml =
            """
            <gameList>
              <game>
                <path>./Game.zip</path>
                <name>   </name>
              </game>
            </gameList>
            """.trimIndent()

        val result = GamelistBulkParser.parseAllGames(xml)

        assertEquals(listOf(GamelistGameEntry("./Game.zip", "Game.zip")), result)
    }

    @Test
    fun `returns an empty list instead of throwing on malformed XML`() {
        val xml =
            """
            <gameList>
              <game>
                <path>./Game.zip</path>
                <name>Tom & Jerry</name>
              </game>
            </gameList>
            """.trimIndent()

        val result = GamelistBulkParser.parseAllGames(xml)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `returns an empty list when there is no gameList tag at all`() {
        val result = GamelistBulkParser.parseAllGames("<somethingElse></somethingElse>")

        assertTrue(result.isEmpty())
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
                <name>Game</name>
              </game>
            </gameList>
            """.trimIndent()

        val result = GamelistBulkParser.parseAllGames(xml)

        assertEquals(listOf(GamelistGameEntry("./Game.zip", "Game")), result)
    }

    @Test
    fun `returns an empty list for a gameList with no games`() {
        val result = GamelistBulkParser.parseAllGames("<gameList></gameList>")

        assertTrue(result.isEmpty())
    }
}

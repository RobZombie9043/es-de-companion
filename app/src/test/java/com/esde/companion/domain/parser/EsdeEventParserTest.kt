package com.esde.companion.domain.parser

import com.esde.companion.domain.model.EsdeEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Fixture lines below are taken verbatim from a real es_log.txt capture, including the
 * timestamp/log-level prefix and the game path with spaces and parentheses in it - that
 * combination is exactly the kind of thing a naive parser gets wrong.
 */
class EsdeEventParserTest {

    private val parser = EsdeEventParser()

    @Test
    fun `parses system-select`() {
        val line = "Jul 28 07:01:30 Debug:  Scripting::fireEvent(): system-select " +
            "\"gamegear\" \"Sega Game Gear\" \"/storage/E2AB-E84A/ROMs/gamegear\" \"\""

        assertEquals(
            EsdeEvent.SystemSelect(
                systemShortName = "gamegear",
                systemFullName = "Sega Game Gear",
                systemPath = "/storage/E2AB-E84A/ROMs/gamegear",
            ),
            parser.parseLine(line),
        )
    }

    @Test
    fun `parses game-select with spaces and parentheses in the rom path`() {
        val line = "Jul 28 07:01:32 Debug:  Scripting::fireEvent(): game-select " +
            "\"/storage/E2AB-E84A/ROMs/dreamcast/Dead or Alive 2 (USA).chd\" " +
            "\"Dead or Alive 2\" \"dreamcast\" \"Sega Dreamcast\""

        assertEquals(
            EsdeEvent.GameSelect(
                romPath = "/storage/E2AB-E84A/ROMs/dreamcast/Dead or Alive 2 (USA).chd",
                gameName = "Dead or Alive 2",
                systemShortName = "dreamcast",
                systemFullName = "Sega Dreamcast",
            ),
            parser.parseLine(line),
        )
    }

    @Test
    fun `parses game-start`() {
        val line = "Jul 28 07:01:46 Debug:  Scripting::fireEvent(): game-start " +
            "\"/storage/E2AB-E84A/ROMs/dreamcast/Cosmic Smash (Japan).chd\" " +
            "\"Cosmic Smash\" \"dreamcast\" \"Sega Dreamcast\""

        assertEquals(
            EsdeEvent.GameStart(
                romPath = "/storage/E2AB-E84A/ROMs/dreamcast/Cosmic Smash (Japan).chd",
                gameName = "Cosmic Smash",
                systemShortName = "dreamcast",
                systemFullName = "Sega Dreamcast",
            ),
            parser.parseLine(line),
        )
    }

    @Test
    fun `parses game-start with shell-escaped path and unescapes it`() {
        val line = "Jul 28 10:01:54 Debug:  Scripting::fireEvent(): game-start " +
                "\"/storage/E2AB-E84A/ROMs/gba/Mega\\ Man\\ Battle\\ Network\\ 3\\ -\\ White\\ Version\\ \\(USA\\).zip\" " +
                "\"Mega Man Battle Network 3 : White\" \"gba\" \"Nintendo Game Boy Advance\""

        val result = parser.parseLine(line)

        assertEquals(
            EsdeEvent.GameStart(
                romPath = "/storage/E2AB-E84A/ROMs/gba/Mega Man Battle Network 3 - White Version (USA).zip",
                gameName = "Mega Man Battle Network 3 : White",
                systemShortName = "gba",
                systemFullName = "Nintendo Game Boy Advance",
            ),
            result,
        )
    }

    @Test
    fun `parses game-select whose display name has unescaped embedded double quotes`() {
        val line = "Debug:  Scripting::fireEvent(): game-select " +
            "\"/storage/E2AB-E84A/ROMs/nes/Ivan 'Ironman' Stewart's Super Off Road (USA).zip\" " +
            "\"Ivan \"Ironman\" Stewart's Super Off Road\" \"nes\" \"Nintendo Entertainment System\""

        assertEquals(
            EsdeEvent.GameSelect(
                romPath = "/storage/E2AB-E84A/ROMs/nes/Ivan 'Ironman' Stewart's Super Off Road (USA).zip",
                gameName = "Ivan \"Ironman\" Stewart's Super Off Road",
                systemShortName = "nes",
                systemFullName = "Nintendo Entertainment System",
            ),
            parser.parseLine(line),
        )
    }

    @Test
    fun `parses game-end`() {
        val line = "Jul 28 07:01:55 Debug:  Scripting::fireEvent(): game-end " +
            "\"/storage/E2AB-E84A/ROMs/dreamcast/Cosmic Smash (Japan).chd\" " +
            "\"Cosmic Smash\" \"dreamcast\" \"Sega Dreamcast\""

        assertEquals(
            EsdeEvent.GameEnd(
                romPath = "/storage/E2AB-E84A/ROMs/dreamcast/Cosmic Smash (Japan).chd",
                gameName = "Cosmic Smash",
                systemShortName = "dreamcast",
                systemFullName = "Sega Dreamcast",
            ),
            parser.parseLine(line),
        )
    }

    @Test
    fun `parses screensaver-start`() {
        val line = "Jul 28 07:01:35 Debug:  Scripting::fireEvent(): screensaver-start \"manual\" \"\" \"\" \"\""

        assertEquals(EsdeEvent.ScreensaverStart("manual"), parser.parseLine(line))
    }

    @Test
    fun `parses screensaver-game-select`() {
        val line = "Jul 28 07:01:36 Debug:  Scripting::fireEvent(): screensaver-game-select " +
            "\"/storage/E2AB-E84A/ROMs/arcade/tapper.zip\" \"Tapper\" \"arcade\" \"Arcade\""

        assertEquals(
            EsdeEvent.ScreensaverGameSelect(
                romPath = "/storage/E2AB-E84A/ROMs/arcade/tapper.zip",
                gameName = "Tapper",
                systemShortName = "arcade",
                systemFullName = "Arcade",
            ),
            parser.parseLine(line),
        )
    }

    @Test
    fun `parses screensaver-end`() {
        val line = "Jul 28 07:01:39 Debug:  Scripting::fireEvent(): screensaver-end \"cancel\" \"\" \"\" \"\""

        assertEquals(EsdeEvent.ScreensaverEnd("cancel"), parser.parseLine(line))
    }

    @Test
    fun `parses startup`() {
        val line = "Aug 05 17:13:32 Debug:  Scripting::fireEvent(): startup \"\" \"\" \"\" \"\""

        assertEquals(EsdeEvent.Startup, parser.parseLine(line))
    }

    @Test
    fun `parses quit`() {
        val line = "Aug 04 14:52:13 Debug:  Scripting::fireEvent(): quit \"\" \"\" \"\" \"\""

        assertEquals(EsdeEvent.Quit, parser.parseLine(line))
    }

    @Test
    fun `parses a window-size-changed line as Reload`() {
        val line = "Aug 04 17:08:54 Info:   Window size has changed from 1240x1080 to 1920x1080, reloading..."

        assertEquals(EsdeEvent.Reload, parser.parseLine(line))
    }

    @Test
    fun `ignores lines with no fireEvent marker`() {
        val line = "Jul 28 07:01:30 Debug:  Window::logInput(Xbox Wireless Controller): " +
            "Button 14, isMappedTo=right, value=1"

        assertNull(parser.parseLine(line))
    }

    @Test
    fun `ignores unrecognized event names`() {
        val line = "Jul 28 07:01:30 Debug:  Scripting::fireEvent(): some-future-event \"a\" \"b\" \"c\" \"d\""

        assertNull(parser.parseLine(line))
    }

    @Test
    fun `returns null for a blank line`() {
        assertNull(parser.parseLine(""))
    }

    @Test
    fun `returns null when the event name has no trailing args at all`() {
        val line = "Jul 28 07:01:30 Debug:  Scripting::fireEvent(): screensaver-start"

        assertNull(parser.parseLine(line))
    }
}

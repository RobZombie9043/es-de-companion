package com.esde.companion.domain.parser

import com.esde.companion.domain.model.NavigationDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fixture lines below are taken verbatim from a real es_log.txt capture (see the
 * conversation that introduced directional logo slides), same rationale as
 * EsdeEventParserTest - a naive parser gets tripped up by real device names/timestamps.
 */
class NavigationDirectionParserTest {
    @Test
    fun `parses up press`() {
        val line = "Aug 02 13:31:33 Debug:  Window::logInput(Xbox Wireless Controller): Button 11, isMappedTo=up, value=1"
        assertEquals(NavigationDirection.Up, NavigationDirectionParser.parseDirectionalPress(line))
    }

    @Test
    fun `parses down press`() {
        val line = "Aug 02 13:31:31 Debug:  Window::logInput(Xbox Wireless Controller): Button 12, isMappedTo=down, value=1"
        assertEquals(NavigationDirection.Down, NavigationDirectionParser.parseDirectionalPress(line))
    }

    @Test
    fun `parses left press`() {
        val line = "Aug 02 13:31:35 Debug:  Window::logInput(Xbox Wireless Controller): Button 13, isMappedTo=left, value=1"
        assertEquals(NavigationDirection.Left, NavigationDirectionParser.parseDirectionalPress(line))
    }

    @Test
    fun `parses right press`() {
        val line = "Aug 02 13:31:34 Debug:  Window::logInput(Xbox Wireless Controller): Button 14, isMappedTo=right, value=1"
        assertEquals(NavigationDirection.Right, NavigationDirectionParser.parseDirectionalPress(line))
    }

    @Test
    fun `release returns null`() {
        val line = "Aug 02 13:31:33 Debug:  Window::logInput(Xbox Wireless Controller): Button 11, isMappedTo=up, value=0"
        assertNull(NavigationDirectionParser.parseDirectionalPress(line))
    }

    @Test
    fun `release with trailing space returns null`() {
        val line = "Aug 02 13:31:33 Debug:  Window::logInput(Xbox Wireless Controller): Button 11, isMappedTo=up, value=0 "
        assertNull(NavigationDirectionParser.parseDirectionalPress(line))
    }

    @Test
    fun `non-directional button press returns null`() {
        val line = "Aug 02 13:31:34 Debug:  Window::logInput(Xbox Wireless Controller): Button 1, isMappedTo=b, value=1"
        assertNull(NavigationDirectionParser.parseDirectionalPress(line))
    }

    @Test
    fun `non-directional button press is still well-formed`() {
        val line = "Aug 02 13:31:34 Debug:  Window::logInput(Xbox Wireless Controller): Button 1, isMappedTo=b, value=1"
        assertTrue(NavigationDirectionParser.isWellFormedInputLine(line))
    }

    @Test
    fun `fireEvent line is not a logInput line`() {
        val line = "Aug 02 13:31:34 Debug:  Scripting::fireEvent(): game-select \"/roms/game.zip\" \"Game\" \"nes\" \"Nintendo\""
        assertFalse(NavigationDirectionParser.isLogInputLine(line))
        assertNull(NavigationDirectionParser.parseDirectionalPress(line))
    }

    @Test
    fun `empty line is not a logInput line`() {
        assertFalse(NavigationDirectionParser.isLogInputLine(""))
        assertNull(NavigationDirectionParser.parseDirectionalPress(""))
    }

    @Test
    fun `malformed logInput line missing value is not well-formed`() {
        val line = "Aug 02 13:31:34 Debug:  Window::logInput(Xbox Wireless Controller): Button 14, isMappedTo=right"
        assertTrue(NavigationDirectionParser.isLogInputLine(line))
        assertFalse(NavigationDirectionParser.isWellFormedInputLine(line))
        assertNull(NavigationDirectionParser.parseDirectionalPress(line))
    }

    @Test
    fun `logInput line truncated mid-line is not well-formed`() {
        val line = "Aug 02 13:31:34 Debug:  Window::logInput(Xbox Wireless Controller): Button 14, isMappedTo=rig"
        assertTrue(NavigationDirectionParser.isLogInputLine(line))
        assertFalse(NavigationDirectionParser.isWellFormedInputLine(line))
        assertNull(NavigationDirectionParser.parseDirectionalPress(line))
    }

    @Test
    fun `unrecognized mapped name is well-formed but not a direction`() {
        val line = "Aug 02 13:31:34 Debug:  Window::logInput(Xbox Wireless Controller): Button 4, isMappedTo=leftshoulder, value=1"
        assertTrue(NavigationDirectionParser.isWellFormedInputLine(line))
        assertNull(NavigationDirectionParser.parseDirectionalPress(line))
    }
}

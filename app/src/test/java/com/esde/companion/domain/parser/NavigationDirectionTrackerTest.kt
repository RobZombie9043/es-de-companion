package com.esde.companion.domain.parser

import com.esde.companion.domain.model.NavigationDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Line sequence mirrors a real es_log.txt capture: every fireEvent is immediately
 * preceded by a directional press and followed by that same button's release.
 */
class NavigationDirectionTrackerTest {
    private val tracker = NavigationDirectionTracker()

    @Test
    fun `starts with no known direction`() {
        assertNull(tracker.direction)
    }

    @Test
    fun `a directional press sets the direction`() {
        tracker.observeLine("Window::logInput(Xbox Wireless Controller): Button 14, isMappedTo=right, value=1")
        assertEquals(NavigationDirection.Right, tracker.direction)
    }

    @Test
    fun `direction survives the fireEvent line that follows the press`() {
        tracker.observeLine("Window::logInput(Xbox Wireless Controller): Button 14, isMappedTo=right, value=1")
        tracker.observeLine("Scripting::fireEvent(): game-select \"/roms/game.zip\" \"Game\" \"nes\" \"Nintendo\"")
        assertEquals(NavigationDirection.Right, tracker.direction)
    }

    @Test
    fun `release clears the direction`() {
        tracker.observeLine("Window::logInput(Xbox Wireless Controller): Button 14, isMappedTo=right, value=1")
        tracker.observeLine("Scripting::fireEvent(): game-select \"/roms/game.zip\" \"Game\" \"nes\" \"Nintendo\"")
        tracker.observeLine("Window::logInput(Xbox Wireless Controller): Button 14, isMappedTo=right, value=0")
        assertNull(tracker.direction)
    }

    @Test
    fun `a later unrelated fireEvent with no preceding press carries no direction`() {
        tracker.observeLine("Window::logInput(Xbox Wireless Controller): Button 14, isMappedTo=right, value=1")
        tracker.observeLine("Scripting::fireEvent(): game-select \"/roms/game.zip\" \"Game\" \"nes\" \"Nintendo\"")
        tracker.observeLine("Window::logInput(Xbox Wireless Controller): Button 14, isMappedTo=right, value=0")
        // A long stretch of unrelated lines, none of them controller input.
        tracker.observeLine("Some unrelated log line")
        assertNull(tracker.direction)
    }

    @Test
    fun `a non-directional button press clears a previously tracked direction`() {
        tracker.observeLine("Window::logInput(Xbox Wireless Controller): Button 11, isMappedTo=up, value=1")
        tracker.observeLine("Scripting::fireEvent(): game-select \"/roms/game.zip\" \"Game\" \"nes\" \"Nintendo\"")
        tracker.observeLine("Window::logInput(Xbox Wireless Controller): Button 11, isMappedTo=up, value=0")
        tracker.observeLine("Window::logInput(Xbox Wireless Controller): Button 1, isMappedTo=b, value=1")
        assertNull(tracker.direction)
    }

    @Test
    fun `b-triggered system-select carries no direction, reproducing the real log excerpt`() {
        tracker.observeLine("Window::logInput(Xbox Wireless Controller): Button 11, isMappedTo=up, value=1")
        tracker.observeLine("Scripting::fireEvent(): game-select \"/roms/game.zip\" \"Buggy Run\" \"mastersystem\" \"Sega Master System\"")
        tracker.observeLine("Window::logInput(Xbox Wireless Controller): Button 11, isMappedTo=up, value=0")
        tracker.observeLine("Window::logInput(Xbox Wireless Controller): Button 1, isMappedTo=b, value=1")
        // At this point, the direction for the following system-select must be null.
        assertNull(tracker.direction)
        tracker.observeLine("Scripting::fireEvent(): system-select \"mastersystem\" \"Sega Master System\" \"/roms/mastersystem\" \"\"")
        assertNull(tracker.direction)
    }

    @Test
    fun `a malformed input line leaves a tracked direction untouched`() {
        tracker.observeLine("Window::logInput(Xbox Wireless Controller): Button 14, isMappedTo=right, value=1")
        tracker.observeLine("Window::logInput(Xbox Wireless Controller): Button 14, isMappedTo=rig")
        assertEquals(NavigationDirection.Right, tracker.direction)
    }

    @Test
    fun `a malformed input line does not fabricate a direction when none was tracked`() {
        tracker.observeLine("Window::logInput(Xbox Wireless Controller): Button 14, isMappedTo=rig")
        assertNull(tracker.direction)
    }
}

package com.esde.companion.domain.gameguides

import org.junit.Assert.assertEquals
import org.junit.Test

class GuideTextReflowTest {
    @Test
    fun `a hard-wrapped prose paragraph is joined into one line`() {
        val text = "This is a line\nthat was wrapped\nat a narrow width."

        assertEquals("This is a line that was wrapped at a narrow width.", GuideTextReflow.reflow(text))
    }

    @Test
    fun `separate paragraphs are reflowed independently and stay separated by one blank line`() {
        val text = "First paragraph\nwrapped across\ntwo lines.\n\nSecond paragraph\nalso wrapped."

        val result = GuideTextReflow.reflow(text)

        assertEquals("First paragraph wrapped across two lines.\n\nSecond paragraph also wrapped.", result)
    }

    @Test
    fun `a paragraph containing box-drawing characters is preserved untouched`() {
        val ascii = "+----------+\n|  Room 1  |\n+----------+"

        assertEquals(ascii, GuideTextReflow.reflow(ascii))
    }

    @Test
    fun `a paragraph with table-like multi-space alignment is preserved untouched`() {
        val table = "Item          Price\nSword         100 gil\nShield        50 gil"

        assertEquals(table, GuideTextReflow.reflow(table))
    }

    @Test
    fun `a paragraph where most lines are indented is preserved untouched`() {
        val indented = "  Step 1: do this\n  Step 2: do that\n  Step 3: done"

        assertEquals(indented, GuideTextReflow.reflow(indented))
    }

    @Test
    fun `a single-line paragraph is returned unchanged`() {
        assertEquals("Just one line.", GuideTextReflow.reflow("Just one line."))
    }
}

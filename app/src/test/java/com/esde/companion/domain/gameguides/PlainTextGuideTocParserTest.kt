package com.esde.companion.domain.gameguides

import com.esde.companion.domain.model.GuideTocEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlainTextGuideTocParserTest {
    @Test
    fun `empty text produces no entries`() {
        assertEquals(emptyList<GuideTocEntry>(), PlainTextGuideTocParser.parse(""))
    }

    @Test
    fun `text with no banner headers produces no entries`() {
        val text = "Just some ordinary prose.\nNo borders anywhere in sight.\n"
        assertTrue(PlainTextGuideTocParser.parse(text).isEmpty())
    }

    @Test
    fun `a border line followed by a title line produces one entry`() {
        val text =
            listOf(
                "===============================",
                " 1. INTRODUCTION",
                "===============================",
                "Some body text here.",
            ).joinToString("\n")

        val entries = PlainTextGuideTocParser.parse(text)

        assertEquals(1, entries.size)
        assertEquals("1. INTRODUCTION", entries[0].title)
    }

    @Test
    fun `multiple banner headers each produce their own entry in order`() {
        val text =
            listOf(
                "--------------------",
                "Introduction",
                "--------------------",
                "Some intro text.",
                "",
                "--------------------",
                "Walkthrough",
                "--------------------",
                "Some walkthrough text.",
            ).joinToString("\n")

        val entries = PlainTextGuideTocParser.parse(text)

        assertEquals(listOf("Introduction", "Walkthrough"), entries.map { it.title })
    }

    @Test
    fun `a border shorter than the minimum length is not treated as a header`() {
        val text = listOf("----", "Too Short", "----").joinToString("\n")

        assertTrue(PlainTextGuideTocParser.parse(text).isEmpty())
    }

    @Test
    fun `a border line with mixed characters is not treated as a border`() {
        val text = listOf("==-==-==-==-==-==", "Mixed Border", "==-==-==-==-==-==").joinToString("\n")

        assertTrue(PlainTextGuideTocParser.parse(text).isEmpty())
    }

    @Test
    fun `a border immediately followed by another border produces no entry`() {
        val text = listOf("========================", "========================", "Body text").joinToString("\n")

        assertTrue(PlainTextGuideTocParser.parse(text).isEmpty())
    }

    @Test
    fun `a border followed by a blank line produces no entry`() {
        val text = listOf("========================", "", "Body text").joinToString("\n")

        assertTrue(PlainTextGuideTocParser.parse(text).isEmpty())
    }

    @Test
    fun `anchorId is the character offset of the title line within the text`() {
        val border = "=".repeat(20)
        val text = "$border\nTitle\n$border\n"

        val entries = PlainTextGuideTocParser.parse(text)

        val expectedOffset = border.length + 1
        assertEquals(expectedOffset.toString(), entries.single().anchorId)
        assertEquals("Title", text.substring(expectedOffset, expectedOffset + "Title".length))
    }

    // --- Hierarchical (Roman numeral / letter / number) table of contents ------------------

    @Test
    fun `a printed Roman-letter-number table of contents produces nested entries in order`() {
        val tocBlock =
            listOf(
                "_________________________ ____ _______________________________",
                "|Table of Contents \\____/",
                "-------------------------------------------------------------------------------",
                "| I. Intro \\",
                "\\ a. Gameplay |",
                "| b. Story \\",
                "| II. Walkthrough |",
                "| a. Unidentified distress signal.... |",
                "| b. Speedy Recovery |",
                "| 1. Getting the Missile Launcher |",
                "| 2. Grabbing the Morph Ball |",
                "\\___________/",
            )
        val body =
            listOf(
                "Some filler paragraph before any real heading appears.",
                "",
                "/ Intro \\",
                "-----------< Gameplay >------------------------------------------------",
                "Gameplay body text.",
                "-----------< Story >------------------------------------------------",
                "Story body text.",
                "===============================================================================",
                "Unidentified distress signal.... /",
                "====================================",
                "Distress signal body text.",
                "===============================================================================",
                "Speedy Recovery /",
                "====================================",
                "-------------------------------------------------------------------------------",
                "Getting the Missile Launcher /",
                "------------------------------------",
                "Missile launcher body text.",
                "-------------------------------------------------------------------------------",
                "Grabbing the Morph Ball /",
                "------------------------------------",
                "Morph ball body text.",
            )
        val text = (tocBlock + body).joinToString("\n")

        val entries = PlainTextGuideTocParser.parse(text)

        assertEquals(
            listOf(
                "Intro" to 0,
                "Gameplay" to 1,
                "Story" to 1,
                "Walkthrough" to 0,
                "Unidentified distress signal...." to 1,
                "Speedy Recovery" to 1,
                "Getting the Missile Launcher" to 2,
                "Grabbing the Morph Ball" to 2,
            ),
            entries.map { it.title to it.depth },
        )
    }

    @Test
    fun `hierarchical anchors point at the real body heading, not the table of contents listing`() {
        val tocBlock = listOf("| I. Intro |", "| a. Gameplay |", "| b. Story |", "| II. Walkthrough |")
        val body = listOf("Intro", "Gameplay /", "Story /", "Walkthrough /")
        val text = (tocBlock + body).joinToString("\n")
        val tocBlockLength = tocBlock.joinToString("\n").length + 1

        val entries = PlainTextGuideTocParser.parse(text)

        entries.forEach { entry -> assertTrue(entry.anchorId.toInt() >= tocBlockLength) }
        assertEquals(listOf("Intro", "Gameplay", "Story", "Walkthrough"), entries.map { it.title })
    }

    @Test
    fun `a single-level numbered list in prose is not mistaken for a hierarchical table of contents`() {
        val text =
            listOf(
                "1. Do the first thing",
                "2. Do the second thing",
                "3. Do the third thing",
                "4. Do the fourth thing",
                "5. Do the fifth thing",
            ).joinToString("\n")

        assertTrue(PlainTextGuideTocParser.parse(text).isEmpty())
    }

    @Test
    fun `too few numbered lines falls back to the flat border parser`() {
        val text =
            listOf(
                "| I. Intro |",
                "| a. Gameplay |",
                "========================",
                "Fallback Header",
                "========================",
            ).joinToString("\n")

        val entries = PlainTextGuideTocParser.parse(text)

        assertEquals(listOf("Fallback Header"), entries.map { it.title })
    }
}

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

    @Test
    fun `Chapter and Section headings are recognized as a hierarchical table of contents`() {
        val tocBlock =
            listOf(
                "Chapter 1: Intro",
                "Section 1.1: Gameplay",
                "Section 1.2: Story",
                "Chapter 2: Walkthrough",
            )
        val body = listOf("Some filler paragraph.", "", "Intro", "Gameplay", "Story", "Walkthrough")
        val text = (tocBlock + body).joinToString("\n")

        val entries = PlainTextGuideTocParser.parse(text)

        assertEquals(
            listOf("Intro" to 0, "Gameplay" to 1, "Story" to 1, "Walkthrough" to 0),
            entries.map { it.title to it.depth },
        )
    }

    @Test
    fun `anchor prefers a whole matching line over an earlier incidental substring`() {
        val tocBlock = listOf("| I. Introduction |", "| a. Gameplay |", "| b. Story |", "| II. Walkthrough |")
        val body =
            listOf(
                "This guide's Introduction was written over several weeks.",
                "",
                "Introduction",
                "Gameplay /",
                "Story /",
                "Walkthrough /",
            )
        val text = (tocBlock + body).joinToString("\n")

        val entries = PlainTextGuideTocParser.parse(text)

        val introEntry = entries.first { it.title == "Introduction" }
        val realHeadingOffset = text.indexOf("\nIntroduction\n") + 1
        assertEquals(realHeadingOffset.toString(), introEntry.anchorId)
    }

    // --- Author-printed "Search Code" table of contents -------------------------------------

    @Test
    fun `a printed Search Code table of contents is parsed ahead of the noisy flat border scan`() {
        val toc =
            listOf(
                "(Table of Contents)",
                "",
                "Search Code",
                " ----",
                " PA1        Controls",
                " PA2        The World",
                " PA3        Characters",
                " PG1        Gameplay Basics",
                "",
            )
        // A real guide has far more than CODE_TOC_MAX_GAP_LINES of ordinary prose between the
        // printed table of contents and the first real body heading - reproduced here with
        // plain filler so the scan actually stops at the TOC's real end (a too-short gap would
        // let scanning run straight into the body, defeating the point of this test).
        val filler = List(30) { "Ordinary prose line $it, not part of any table of contents." }
        // Every visited location in a real guide's body is wrapped in an underscore banner -
        // exactly the "hundreds of lines" symptom this fix targets - none of which should
        // show up now that a real printed table of contents was found.
        val body =
            filler +
                listOf(
                    "===============================",
                    "PA1 Controls",
                    "===============================",
                    "Controls body text.",
                    "_______________________________",
                    "Random Location One",
                    "_______________________________",
                    "Filler location text.",
                    "_______________________________",
                    "Random Location Two",
                    "_______________________________",
                    "More filler location text.",
                    "===============================",
                    "PA2 The World",
                    "===============================",
                    "World body text.",
                    "===============================",
                    "PA3 Characters",
                    "===============================",
                    "Characters body text.",
                    "===============================",
                    "PG1 Gameplay Basics",
                    "===============================",
                    "Gameplay basics body text.",
                )
        val text = (toc + body).joinToString("\n")

        val entries = PlainTextGuideTocParser.parse(text)

        assertEquals(
            listOf("Controls", "The World", "Characters", "Gameplay Basics"),
            entries.map { it.title },
        )
        val realControlsHeadingOffset = text.indexOf("PA1 Controls", startIndex = toc.joinToString("\n").length)
        assertEquals(realControlsHeadingOffset.toString(), entries.first { it.title == "Controls" }.anchorId)
    }

    @Test
    fun `a two-column Search Code line produces column-major entries, not row-major`() {
        val toc =
            listOf(
                "Table of Contents",
                "",
                "Search Code",
                " PM1        April\t\tPM7        October",
                " PM2        May\t\tPM8        November",
                " PM3        June\t\tPM9        December",
                " PM4        July\t\tPMA1       January",
                "",
            )
        val body =
            listOf(
                "===============================",
                "PM1 April",
                "===============================",
                "April body text.",
            )
        val text = (toc + body).joinToString("\n")

        val entries = PlainTextGuideTocParser.parse(text)

        // Reading down the first column (April, May, June, July) before the second
        // (October, November, December, January) - not across each printed row in turn.
        assertEquals(
            listOf("April", "May", "June", "July", "October", "November", "December", "January"),
            entries.map { it.title },
        )
    }

    @Test
    fun `trailing column-only rows are appended after the shorter first column, not before it`() {
        // Mirrors a real guide's calendar-month listing (Persona 4 Golden, GameFAQs FAQ id
        // 64387): column 1 runs out after 2 rows, but column 2 keeps going for 2 more - those
        // extra rows have only a column-2 entry, with nothing in column 1's position.
        val toc =
            listOf(
                "Table of Contents",
                "",
                "Search Code",
                " PM1        April\t\tPM7        October",
                " PM2        May\t\tPM8        November",
                "\t\t\t\tPMA4\t   Ending",
                "\t\t\t\tPMA5\t   Second Playthrough",
                "",
            )
        val body =
            listOf(
                "===============================",
                "PM1 April",
                "===============================",
                "April body text.",
            )
        val text = (toc + body).joinToString("\n")

        val entries = PlainTextGuideTocParser.parse(text)

        assertEquals(
            listOf("April", "May", "October", "November", "Ending", "Second Playthrough"),
            entries.map { it.title },
        )
    }

    @Test
    fun `Section headers group their codes under one parent entry, in column-major order`() {
        val filler = List(30) { "Ordinary prose line $it, not part of any table of contents." }
        val toc =
            listOf(
                "(Table of Contents)",
                "",
                "Search Code",
                "-----------------------------",
                "  Section 1 About the game  /",
                "----------------------------",
                " PA1        Controls",
                " PA2        The World",
                " PA3        Characters",
                "--------------------------------",
                "  Section 2 Main Walkthrough /",
                "------------------------------",
                " PM1        April\t\tPM7        October",
                " PM2        May\t\tPM8        November",
            )
        val body =
            filler +
                listOf(
                    "===============================",
                    "PA1 Controls",
                    "===============================",
                    "Controls body text.",
                    "===============================",
                    "PA2 The World",
                    "===============================",
                    "World body text.",
                    "===============================",
                    "PA3 Characters",
                    "===============================",
                    "Characters body text.",
                    "===============================",
                    "April 2011 (PM1)",
                    "===============================",
                    "April body text.",
                )
        val text = (toc + body).joinToString("\n")

        val entries = PlainTextGuideTocParser.parse(text)

        assertEquals(
            listOf(
                "About the game" to 0,
                "Controls" to 1,
                "The World" to 1,
                "Characters" to 1,
                "Main Walkthrough" to 0,
                "April" to 1,
                "May" to 1,
                "October" to 1,
                "November" to 1,
            ),
            entries.map { it.title to it.depth },
        )
        // No real "Section 1" text exists anywhere in a real guide's body (confirmed on a real
        // guide, see parseCodeToc's kdoc) - its entry is anchored at its first child instead.
        val sectionEntry = entries.first { it.title == "About the game" }
        val controlsEntry = entries.first { it.title == "Controls" }
        assertEquals(controlsEntry.anchorId, sectionEntry.anchorId)
    }

    @Test
    fun `too few Search Code lines falls back to the hierarchical parser`() {
        val text =
            listOf(
                "Table of Contents",
                " PA1        Controls",
                "",
                "| I. Intro |",
                "| a. Gameplay |",
                "| b. Story |",
                "| II. Walkthrough |",
            ).joinToString("\n")

        val entries = PlainTextGuideTocParser.parse(text)

        assertEquals(listOf("Intro", "Gameplay", "Story", "Walkthrough"), entries.map { it.title })
    }

    @Test
    fun `underscore-bordered headings are ignored by the flat border parser`() {
        val text =
            listOf(
                "_______________________________",
                "Random Location",
                "_______________________________",
                "Filler location text.",
            ).joinToString("\n")

        assertTrue(PlainTextGuideTocParser.parse(text).isEmpty())
    }
}

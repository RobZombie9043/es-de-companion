package com.esde.companion.domain.gameguides

import com.esde.companion.domain.model.GuideTocEntry

private const val MIN_BORDER_LENGTH = 8
private const val MAX_TITLE_LENGTH = 80
private val BORDER_CHARS = setOf('=', '-', '*', '~', '_', '#')

// matchHeaderAt()'s return value: how many lines to advance past.
private const val ADVANCE_NO_MATCH = 1
private const val ADVANCE_DOUBLE_BORDER = 2
private const val ADVANCE_HEADER_NO_CLOSING_BORDER = 2
private const val ADVANCE_HEADER_WITH_CLOSING_BORDER = 3

// A genuine printed table of contents has many entries; this rules out a stray one-off
// numbered sentence ("see step 3 above") in ordinary prose being mistaken for one.
private const val MIN_HIERARCHICAL_ENTRIES = 4

// Leading/trailing ASCII-art box-drawing characters stripped before testing a line against
// the numbering patterns below - deliberately narrower than BORDER_CHARS (no '=' or '-'):
// a real horizontal-rule border line should still end/split a run rather than be silently
// swallowed as connective tissue, so a hierarchical block that turns out not to follow this
// exact convention degrades to fewer (or zero) entries rather than a corrupted mix.
private val DECORATION_TRIM = Regex("""^[|\\/~_\s]+|[|\\/~_\s]+$""")
private val LEVEL0_ROMAN = Regex("""^[IVXLCDM]{1,6}\.\s*(.+)$""")
private val LEVEL1_LETTER = Regex("""^[a-z]\.\s*(.+)$""")
private val LEVEL2_DIGIT = Regex("""^[0-9]{1,3}\.\s*(.+)$""")

/**
 * Best-effort table of contents for a plain-text guide.
 *
 * Tries [parseHierarchicalToc] first: many classic GameFAQs-style guides print their own
 * table of contents near the top using a Roman-numeral/letter/number convention, e.g.
 * ```
 * I. Intro
 *   a. Gameplay
 *   b. Story
 * II. Walkthrough
 *   a. Unidentified distress signal....
 *   b. Speedy Recovery
 *     1. Getting the Missile Launcher
 * ```
 * often wrapped in ASCII-art box-drawing (`|`, `\`, `/`, `_`) for decoration. When a long
 * enough run of such numbered lines is found, this reproduces that author-supplied hierarchy
 * directly (see [GuideTocEntry.depth]) - far more useful than a flat list of every single
 * subsection, which is what the older [parseFlatBorderToc] fallback below produces from
 * scanning the guide's actual body headers (a decorative border line immediately followed by
 * a short title line), e.g.:
 * ```
 * ===============================
 *  1. INTRODUCTION
 * ===============================
 * ```
 * Neither is a real structural parser - many guides don't follow either convention at all, in
 * which case this simply finds nothing (an empty table of contents, not a crash or a wrong
 * guess). A matching entry's [GuideTocEntry.anchorId] is a character offset within [text] -
 * see [GuideTocEntry]'s kdoc for why the viewer can only use this as an approximate scroll
 * position, not an exact jump.
 */
object PlainTextGuideTocParser {
    fun parse(text: String): List<GuideTocEntry> {
        val lines = text.split("\n")
        val lineStartOffsets = lineStartOffsets(lines)
        return parseHierarchicalToc(text, lines, lineStartOffsets)
            ?: parseFlatBorderToc(lines, lineStartOffsets)
    }

    private data class TocCandidate(val lineIndex: Int, val depth: Int, val title: String)

    /** Returns null when no run of numbered lines both long enough ([MIN_HIERARCHICAL_ENTRIES])
     * and varied enough (at least two distinct [TocCandidate.depth] values - a single-level
     * run is more likely an ordinary numbered list in prose than a real, nested ToC) was
     * found anywhere in the text. */
    private fun parseHierarchicalToc(
        text: String,
        lines: List<String>,
        lineStartOffsets: IntArray,
    ): List<GuideTocEntry>? {
        val run = findHierarchicalRun(lines) ?: return null
        val searchFrom = lineStartOffsets.getOrElse(run.last().lineIndex + 1) { text.length }
        return run.map { candidate ->
            val anchor = findAnchorOffset(text, candidate.title, searchFrom)
            GuideTocEntry(title = candidate.title, anchorId = anchor.toString(), depth = candidate.depth)
        }
    }

    private fun findHierarchicalRun(lines: List<String>): List<TocCandidate>? {
        var qualifyingRun: List<TocCandidate>? = null
        var current = mutableListOf<TocCandidate>()
        for (index in lines.indices) {
            val stripped = DECORATION_TRIM.replace(lines[index], "")
            val candidate = classify(stripped, index)
            when {
                candidate != null -> current.add(candidate)
                stripped.isBlank() -> Unit
                else -> {
                    if (qualifyingRun == null && isQualifyingRun(current)) qualifyingRun = current
                    current = mutableListOf()
                }
            }
        }
        return qualifyingRun ?: current.takeIf { isQualifyingRun(it) }
    }

    private fun isQualifyingRun(run: List<TocCandidate>): Boolean =
        run.size >= MIN_HIERARCHICAL_ENTRIES && run.map { it.depth }.distinct().size >= 2

    private fun classify(
        stripped: String,
        index: Int,
    ): TocCandidate? =
        LEVEL0_ROMAN.matchEntire(stripped)?.let { TocCandidate(index, 0, it.groupValues[1].trim()) }
            ?: LEVEL1_LETTER.matchEntire(stripped)?.let { TocCandidate(index, 1, it.groupValues[1].trim()) }
            ?: LEVEL2_DIGIT.matchEntire(stripped)?.let { TocCandidate(index, 2, it.groupValues[1].trim()) }

    private fun parseFlatBorderToc(
        lines: List<String>,
        lineStartOffsets: IntArray,
    ): List<GuideTocEntry> {
        val entries = mutableListOf<GuideTocEntry>()
        var index = 0
        while (index < lines.size) {
            val advance = matchHeaderAt(lines, index, lineStartOffsets, entries)
            index += advance
        }
        return entries
    }

    /** Returns how many lines to advance past - more than 1 when a border-title(-border)
     * pattern was consumed, so the closing border (or a second, header-less border
     * immediately following the first) is never itself re-examined as its own opener. */
    private fun matchHeaderAt(
        lines: List<String>,
        index: Int,
        lineStartOffsets: IntArray,
        entries: MutableList<GuideTocEntry>,
    ): Int {
        val nextLine = lines.getOrNull(index + 1)
        val titleLine = nextLine?.trim()
        return when {
            !isBorderLine(lines[index]) || nextLine == null -> ADVANCE_NO_MATCH
            // Two consecutive border lines - a thick/double separator, not a border-title
            // pair. Skip both so the second border isn't then mistaken for opening its own
            // header out of whatever text happens to follow.
            isBorderLine(nextLine) -> ADVANCE_DOUBLE_BORDER
            titleLine == null || !isPlausibleTitle(titleLine) -> ADVANCE_NO_MATCH
            else -> {
                entries += GuideTocEntry(title = titleLine, anchorId = lineStartOffsets[index + 1].toString())
                val hasClosingBorder = index + 2 < lines.size && isBorderLine(lines[index + 2])
                if (hasClosingBorder) ADVANCE_HEADER_WITH_CLOSING_BORDER else ADVANCE_HEADER_NO_CLOSING_BORDER
            }
        }
    }

    private fun lineStartOffsets(lines: List<String>): IntArray {
        val offsets = IntArray(lines.size)
        var running = 0
        for (i in lines.indices) {
            offsets[i] = running
            running += lines[i].length + 1
        }
        return offsets
    }

    private fun isBorderLine(line: String): Boolean {
        val trimmed = line.trim()
        if (trimmed.length < MIN_BORDER_LENGTH) return false
        val first = trimmed[0]
        return first in BORDER_CHARS && trimmed.all { it == first }
    }

    private fun isPlausibleTitle(line: String): Boolean {
        if (line.isEmpty() || line.length > MAX_TITLE_LENGTH || isBorderLine(line)) return false
        return line.any { it.isLetter() }
    }
}

/** The first later occurrence of [title] in [text] (a plain, case-insensitive substring
 * search, starting after the ToC block itself so an entry's own listing there is never
 * mistaken for its actual body heading) - matches loosely-decorated real headers like
 * `Walkthrough /` or `/ Walkthrough \` without needing to know which exact decoration a given
 * guide uses. Falls back to [searchFrom] itself (the position right after the ToC block) when
 * no later occurrence is found, rather than dropping the entry. */
private fun findAnchorOffset(
    text: String,
    title: String,
    searchFrom: Int,
): Int {
    val index = text.indexOf(title, startIndex = searchFrom.coerceIn(0, text.length), ignoreCase = true)
    return if (index >= 0) index else searchFrom
}

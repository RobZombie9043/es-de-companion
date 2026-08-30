package com.esde.companion.domain.gameguides

import com.esde.companion.domain.model.GuideTocEntry

private const val MIN_BORDER_LENGTH = 8
private const val MAX_TITLE_LENGTH = 80

// '_' deliberately excluded - confirmed on a real guide (Persona 4 Golden, GameFAQs FAQ id
// 64387) whose walkthrough body uses a "_______\n<location name>\n_______" banner for every
// single place the player visits, hundreds of them, none of which are real top-level
// sections; '=', '-', '*', '~', '#' are the characters real chapter-banner borders actually
// use on the guides sampled for this parser.
private val BORDER_CHARS = setOf('=', '-', '*', '~', '#')

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
private val LEVEL0_CHAPTER = Regex("""^(?:Chapter|Part)\s+[IVXLCDM0-9]+\s*[:.\-]?\s+(.+)$""", RegexOption.IGNORE_CASE)
private val LEVEL1_LETTER = Regex("""^[a-z]\.\s*(.+)$""")
private val LEVEL1_SECTION = Regex("""^Section\s+[0-9]+(?:\.[0-9]+)?\s*[:.\-]?\s+(.+)$""", RegexOption.IGNORE_CASE)
private val LEVEL2_DIGIT = Regex("""^[0-9]{1,3}\.\s*(.+)$""")

// --- Author-printed "Search Code" quick-navigation table of contents -----------------------
// Some GameFAQs guides print their own short unique code per section (e.g. "PA1", "PMA5")
// next to its human-readable title, meant for the reader to Ctrl+F the code directly - see
// parseCodeToc's kdoc below.
private const val MIN_CODE_TOC_ENTRIES = 4
private const val CODE_TOC_MAX_GAP_LINES = 25
private const val CODE_TOC_MAX_SCAN_LINES = 400
private val CODE_TOC_MARKER = Regex("""table of contents|search code""", RegexOption.IGNORE_CASE)
private val CODE_TOKEN = Regex("""^[A-Z]{2,4}[0-9]{1,3}$""")
private val CODE_TOC_COLUMN_SPLIT = Regex("""\s{2,}""")

// A "Section N <title>" line (optionally trailed by a decorative "/") groups the codes
// beneath it under one parent entry - see scanCodeToc's kdoc.
private val CODE_TOC_SECTION_HEADER =
    Regex("""^Section\s+[0-9]+(?:\.[0-9]+)?\s+(.+?)\s*/?\s*$""", RegexOption.IGNORE_CASE)

/**
 * Best-effort table of contents for a plain-text guide.
 *
 * Tries [parseCodeToc] first: some guides print their own "Search Code" quick-navigation
 * table of contents, a short unique code per section that the reader is meant to Ctrl+F -
 * when present, its codes make far more precise anchors than any title-text search, and its
 * printed structure is the guide's own genuine table of contents rather than something
 * inferred from scanning the body. Falls back to [parseHierarchicalToc]: many classic
 * GameFAQs-style guides print their own table of contents near the top using a Roman-numeral/
 * letter/number convention, e.g.
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
 * the viewer resolves it against the text's own rendered line layout for an exact jump, so
 * the only remaining source of imprecision is this parser mislocating the heading itself
 * (e.g. [findAnchorOffset] matching an earlier incidental occurrence of a title's text), not
 * the jump mechanism.
 */
object PlainTextGuideTocParser {
    fun parse(text: String): List<GuideTocEntry> {
        val lines = text.split("\n")
        val lineStartOffsets = lineStartOffsets(lines)
        return parseCodeToc(text, lines, lineStartOffsets)
            ?: parseHierarchicalToc(text, lines, lineStartOffsets)
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
            ?: LEVEL0_CHAPTER.matchEntire(stripped)?.let { TocCandidate(index, 0, it.groupValues[1].trim()) }
            ?: LEVEL1_LETTER.matchEntire(stripped)?.let { TocCandidate(index, 1, it.groupValues[1].trim()) }
            ?: LEVEL1_SECTION.matchEntire(stripped)?.let { TocCandidate(index, 1, it.groupValues[1].trim()) }
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

    /**
     * Detects the guide's own printed "Search Code" quick-navigation table of contents (see
     * [scanCodeToc]) and resolves each code, and each "Section N" group's own entry, to its
     * real body position (see [findCodeAnchorOffset]). A section's own entry is anchored at
     * its first child's position, not searched for directly - confirmed on a real guide
     * (Persona 4 Golden, GameFAQs FAQ id 64387) that a "Section N <title>" heading is printed
     * only once, in the table of contents itself, and never repeated anywhere in the body, so
     * there is no real body occurrence for it to jump to. Returns null (falling through to
     * [parseHierarchicalToc]/[parseFlatBorderToc]) when nothing was found or too few entries
     * qualify - most guides don't use this convention at all.
     */
    private fun parseCodeToc(
        text: String,
        lines: List<String>,
        lineStartOffsets: IntArray,
    ): List<GuideTocEntry>? {
        val scan = scanCodeToc(text, lines, lineStartOffsets) ?: return null
        var cursor = lineStartOffsets.getOrElse(scan.bodyStartLine) { text.length }
        val result = mutableListOf<GuideTocEntry>()
        for (section in scan.sections) {
            val childDepth = if (section.title != null) 1 else 0
            val children =
                section.codes.map { (code, title) ->
                    val anchor = findCodeAnchorOffset(text, code, cursor)
                    cursor = anchor + code.length
                    GuideTocEntry(title = title, anchorId = anchor.toString(), depth = childDepth)
                }
            val sectionTitle = section.title
            if (sectionTitle != null) {
                result += GuideTocEntry(title = sectionTitle, anchorId = children.first().anchorId, depth = 0)
            }
            result += children
        }
        return result
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

/** The first later occurrence of [title] in [text], starting after the ToC block itself so an
 * entry's own listing there is never mistaken for its actual body heading. Prefers a line
 * that consists of exactly [title] (plus optional surrounding whitespace) over a raw
 * substring match - a real body heading is usually the entire line, while a bare substring
 * match can land on the title's own text mentioned in passing, in ordinary prose, before the
 * real heading. Falls back to a plain substring search (still needed for a heading embedded
 * in its own decoration on the same line, e.g. `-----< Walkthrough >-----`, which is never an
 * exact whole-line match), and finally to [searchFrom] itself when no later occurrence is
 * found at all, rather than dropping the entry. */
private fun findAnchorOffset(
    text: String,
    title: String,
    searchFrom: Int,
): Int {
    val from = searchFrom.coerceIn(0, text.length)
    val wholeLinePattern = Regex("""(?m)^[ \t]*${Regex.escape(title)}[ \t]*$""", RegexOption.IGNORE_CASE)
    val wholeLineMatch = wholeLinePattern.find(text, from)
    if (wholeLineMatch != null) return wholeLineMatch.range.first
    val index = text.indexOf(title, startIndex = from, ignoreCase = true)
    return if (index >= 0) index else from
}

/** One "Section N <title>" group from a printed code table of contents, and the codes listed
 * under it, in reading order (see [buildCodeTocSection]) - [title] is null for a guide that
 * lists codes with no such section grouping at all, in which case the codes become flat,
 * top-level entries instead of a group with children. */
private data class CodeTocSection(val title: String?, val codes: List<Pair<String, String>>)

private data class CodeTocScan(val sections: List<CodeTocSection>, val bodyStartLine: Int)

/**
 * Scans forward from a "table of contents"/"search code" marker for "Section N <title>"
 * group headers and lines built as "<code>  <title>" - one or two pairs per line, the latter
 * for a guide's own two-column layout (e.g. a calendar-month listing, "PM1  April    PM7
 * October"). Stops once [CODE_TOC_MAX_GAP_LINES] consecutive lines in a row match nothing,
 * i.e. the scan has run off the end of the printed listing into ordinary body prose -
 * [CodeTocScan.bodyStartLine] is that stopping point, where the real body is assumed to
 * begin. Returns null when no marker was found or too few entries qualify.
 */
private fun scanCodeToc(
    text: String,
    lines: List<String>,
    lineStartOffsets: IntArray,
): CodeTocScan? {
    val markerIndex = CODE_TOC_MARKER.find(text)?.range?.first ?: return null
    val startLine = lineIndexAtOffset(lineStartOffsets, markerIndex) + 1

    val sections = mutableListOf<CodeTocSection>()
    var sectionTitle: String? = null
    var sectionRows = mutableListOf<List<Pair<String, String>>>()
    var totalCodes = 0
    var gap = 0
    var index = startLine
    while (index < lines.size && index - startLine < CODE_TOC_MAX_SCAN_LINES && gap < CODE_TOC_MAX_GAP_LINES) {
        val line = lines[index]
        val nextSectionTitle = parseSectionHeader(line)
        val rowEntries = extractCodeEntriesInLine(line)
        when {
            nextSectionTitle != null -> {
                sections += buildCodeTocSection(sectionTitle, sectionRows)
                sectionTitle = nextSectionTitle
                sectionRows = mutableListOf()
                gap = 0
            }
            rowEntries.isNotEmpty() -> {
                sectionRows += rowEntries
                totalCodes += rowEntries.size
                gap = 0
            }
            else -> gap++
        }
        index++
    }
    sections += buildCodeTocSection(sectionTitle, sectionRows)
    val nonEmptySections = sections.filter { it.codes.isNotEmpty() }
    return if (totalCodes >= MIN_CODE_TOC_ENTRIES) CodeTocScan(nonEmptySections, index) else null
}

/** Reorders [rows] (each the "<code> <title>" pairs found on one printed line, left to right)
 * into column-major reading order - all of column 0 in row order, then all of column 1, and
 * so on - rather than the row-major order they were read in. A guide's own two-column layout
 * (e.g. "PM1 April    PM7 October" / "PM2 May    PM8 November" / ...) reads down the first
 * column (April, May, ...) before starting the second (October, November, ...), the same way
 * a human reader would, not across each row in turn.
 *
 * A row with fewer entries than [columnCount] is missing its EARLIER column(s), not its later
 * one(s): a real two-column listing fills column 0 top-to-bottom then column 1 top-to-bottom,
 * so once column 0 runs out, its trailing rows still have a column-1 entry with nothing in
 * column 0 - confirmed on a real guide's calendar-month listing (Persona 4 Golden, GameFAQs
 * FAQ id 64387), where the last two rows are column-1-only ("Ending", "Second Playthrough").
 * A row's entries are therefore right-aligned into the trailing columns rather than assumed
 * to start at column 0. */
private fun buildCodeTocSection(
    title: String?,
    rows: List<List<Pair<String, String>>>,
): CodeTocSection {
    val columnCount = rows.maxOfOrNull { it.size } ?: 0
    val ordered =
        (0 until columnCount).flatMap { column ->
            rows.mapNotNull { row -> row.getOrNull(column - (columnCount - row.size)) }
        }
    return CodeTocSection(title, ordered)
}

/** [line] trimmed down to a "Section N <title>" heading's own title, or null when it isn't
 * one - see [CODE_TOC_SECTION_HEADER]. */
private fun parseSectionHeader(line: String): String? =
    CODE_TOC_SECTION_HEADER.matchEntire(line.trim())?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }

/** Every "<code>  <title>" pair found in [line], left to right (see [scanCodeToc]'s kdoc for
 * the two-per-line case). */
private fun extractCodeEntriesInLine(line: String): List<Pair<String, String>> {
    val tokens = line.split(CODE_TOC_COLUMN_SPLIT).map { it.trim() }.filter { it.isNotEmpty() }
    val entries = mutableListOf<Pair<String, String>>()
    var i = 0
    while (i < tokens.size - 1) {
        if (CODE_TOKEN.matches(tokens[i])) {
            entries += tokens[i] to tokens[i + 1]
            i += 2
        } else {
            i++
        }
    }
    return entries
}

/** The first later occurrence of [code] as a whole word, starting from [cursor] - unlike
 * [findAnchorOffset]'s title search, no whole-line preference is needed: a short letters+digits
 * code being present at all is already a near-unique signal, since it's extremely unlikely to
 * appear anywhere else in the guide's ordinary prose. */
private fun findCodeAnchorOffset(
    text: String,
    code: String,
    cursor: Int,
): Int {
    val match = Regex("""\b${Regex.escape(code)}\b""").find(text, cursor.coerceIn(0, text.length))
    return match?.range?.first ?: cursor
}

/** The line index containing character [offset] - binary search over [lineStartOffsets]'
 * ascending line-start boundaries. */
private fun lineIndexAtOffset(
    lineStartOffsets: IntArray,
    offset: Int,
): Int {
    val insertionPoint = lineStartOffsets.binarySearch(offset)
    return if (insertionPoint >= 0) insertionPoint else (-insertionPoint - 2).coerceAtLeast(0)
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

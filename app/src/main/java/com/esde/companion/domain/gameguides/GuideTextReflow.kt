package com.esde.companion.domain.gameguides

private val SYMBOL_HEAVY_CHARS = "|+_=~*#<>-".toSet()
private const val SYMBOL_DENSITY_THRESHOLD = 0.15
private const val MIN_LINE_LENGTH_FOR_DENSITY_CHECK = 6
private const val PARAGRAPH_SEPARATOR = "\n\n"

/**
 * Best-effort "smart reflow" for old fixed-width-wrapped plain-text guides, so long guide
 * text re-wraps naturally at the viewer's current width/font size instead of keeping
 * whatever line breaks the original author wrapped it at (often ~60-80 chars, looking
 * ragged or too-narrow on a wider screen). Joins the lines within a paragraph (a run of
 * lines separated from others by a blank line) into one logical line, UNLESS that paragraph
 * looks like ASCII art, a table, or an indented/aligned block (see [isPreformatted]), in
 * which case its original line breaks are preserved untouched, since joining those would
 * garble the layout they depend on.
 */
object GuideTextReflow {
    fun reflow(text: String): String {
        return text
            .split(Regex("\n[ \t]*\n"))
            .joinToString(PARAGRAPH_SEPARATOR) { paragraph -> reflowParagraph(paragraph) }
    }

    private fun reflowParagraph(paragraph: String): String {
        val lines = paragraph.split("\n")
        if (lines.size <= 1 || isPreformatted(lines)) return paragraph
        return lines.joinToString(" ") { it.trim() }.trim()
    }

    private fun isPreformatted(lines: List<String>): Boolean {
        val indentedLineCount = lines.count { it.startsWith("  ") }
        if (lines.isNotEmpty() && indentedLineCount * 2 >= lines.size) return true
        return lines.any { line -> isSymbolHeavy(line) || hasAlignmentSpacing(line) }
    }

    private fun isSymbolHeavy(line: String): Boolean {
        if (line.length < MIN_LINE_LENGTH_FOR_DENSITY_CHECK) return false
        val symbolCount = line.count { it in SYMBOL_HEAVY_CHARS }
        return symbolCount.toDouble() / line.length >= SYMBOL_DENSITY_THRESHOLD
    }

    private fun hasAlignmentSpacing(line: String): Boolean = line.trim().contains("   ")
}

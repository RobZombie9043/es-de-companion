package com.esde.companion.domain.parser

import com.esde.companion.domain.model.NavigationDirection

/**
 * Parses a single raw es_log.txt line for a controller button event, e.g.:
 *   Window::logInput(Xbox Wireless Controller): Button 14, isMappedTo=right, value=1
 *
 * Deliberately a pure function with no Android dependencies, same rationale as
 * EsdeEventParser - unit testable directly against fixture log lines.
 */
object NavigationDirectionParser {

    /** Cheap check for whether [rawLine] is a controller-input line at all, independent of
     * whether it's well-formed or a directional press - see NavigationDirectionTracker,
     * which needs to tell "not an input line" (leave tracked state alone) apart from both
     * [isWellFormedInputLine] and [parseDirectionalPress]. */
    fun isLogInputLine(rawLine: String): Boolean = rawLine.contains(LOG_INPUT_MARKER)

    /** Whether [rawLine] is a controller-input line whose button/value fields fully match
     * the expected shape. False for a line that has the marker but is otherwise malformed
     * or truncated (e.g. cut off mid-line at a tail-window boundary) - the tracker treats
     * that as unparseable noise, not a definitive "no direction" signal, and leaves
     * whatever direction was already tracked untouched. */
    fun isWellFormedInputLine(rawLine: String): Boolean =
        isLogInputLine(rawLine) && BUTTON_REGEX.containsMatchIn(rawLine)

    /**
     * Returns the pressed direction for a well-formed directional button *press* line
     * only. Null for a release (value=0) or a press mapped to anything other than
     * up/down/left/right (b, a, start, select, shoulder buttons, ...) - both are
     * expected, routine outcomes that should clear any previously-tracked direction (see
     * NavigationDirectionTracker), not malformed input. Callers should check
     * [isWellFormedInputLine] first to distinguish that case from genuinely malformed
     * lines.
     */
    fun parseDirectionalPress(rawLine: String): NavigationDirection? {
        val match = BUTTON_REGEX.find(rawLine) ?: return null
        val (mappedTo, value) = match.destructured
        if (value != "1") return null
        return NavigationDirection.entries.firstOrNull { it.name.equals(mappedTo, ignoreCase = true) }
    }

    private const val LOG_INPUT_MARKER = "Window::logInput("
    private val BUTTON_REGEX = Regex("""isMappedTo=(\w+),\s*value=(\d+)""")
}

package com.esde.companion.domain.parser

import com.esde.companion.domain.model.NavigationDirection

/**
 * Tracks the most recent directional controller press, so it can be attached to whichever
 * GameSelect/SystemSelect event immediately follows it in the raw log (see
 * EsdeLogFileRepository). Scoped to a single log-tailing session, the same way `position`
 * is - a fresh instance per [EsdeLogFileRepository.observeEvents] collection.
 *
 * A directional press sets [direction]; a release *or* a non-directional press (b, a,
 * start, ...) clears it back to null, so a much-earlier press never gets misattributed to
 * an unrelated later event (e.g. a `b`/back-triggered system-select correctly carries no
 * direction). A malformed/unparseable input line leaves [direction] untouched rather than
 * clearing it - an unrelated parse hiccup shouldn't wipe out a real in-flight direction.
 * Any other line (including the fireEvent line itself) also leaves it untouched.
 */
class NavigationDirectionTracker {
    var direction: NavigationDirection? = null
        private set

    fun observeLine(rawLine: String) {
        if (!NavigationDirectionParser.isWellFormedInputLine(rawLine)) return
        direction = NavigationDirectionParser.parseDirectionalPress(rawLine)
    }
}

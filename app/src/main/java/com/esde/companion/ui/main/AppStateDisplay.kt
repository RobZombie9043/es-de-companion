package com.esde.companion.ui.main

import com.esde.companion.domain.model.AppState

/**
 * Turns an [AppState] into human-readable text for this initial text-only display.
 * Deliberately kept in the ui layer, not domain - how state is presented is a UI
 * concern and is expected to change often as real screens are built; the state
 * model itself should not need to change alongside it.
 *
 * Format: "State - <name>" followed by each of that state's fields on its own line,
 * in declaration order.
 */
fun AppState.toDisplayText(): String = when (this) {
    is AppState.Idle ->
        "State - Idle"

    is AppState.BrowsingSystem -> buildString {
        appendLine("State - Browsing System")
        appendLine(systemShortName)
        appendLine(systemFullName)
        append(systemPath)
    }

    is AppState.BrowsingGame -> buildString {
        appendLine("State - Browsing Game")
        appendLine(romPath)
        appendLine(gameName)
        appendLine(systemShortName)
        append(systemFullName)
    }

    is AppState.PlayingGame -> buildString {
        appendLine("State - Playing Game")
        appendLine(romPath)
        appendLine(gameName)
        appendLine(systemShortName)
        append(systemFullName)
    }

    is AppState.Screensaver -> buildString {
        appendLine("State - Screensaver")
        append(mode)
        currentGame?.let {
            appendLine()
            appendLine(it.romPath)
            appendLine(it.gameName)
            appendLine(it.systemShortName)
            append(it.systemFullName)
        }
    }
}

/**
 * Formats the cover-art lookup for whatever game is currently in view. Returns an empty
 * string when there's no game in view or nothing to show yet, so callers can append this
 * unconditionally without worrying about stray blank sections.
 */
fun CoverImageStatus?.toDisplayText(): String {
    val status = this ?: return ""
    return buildString {
        appendLine()
        appendLine()
        appendLine("Cover image path (example)")
        appendLine(status.exampleFilePath)
        append(if (status.found) "Found: yes" else "Found: no")
    }
}
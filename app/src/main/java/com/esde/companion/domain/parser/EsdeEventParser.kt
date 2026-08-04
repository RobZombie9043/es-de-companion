package com.esde.companion.domain.parser

import com.esde.companion.domain.model.EsdeEvent

/**
 * Parses a single raw line from es_log.txt into a typed [EsdeEvent].
 *
 * Deliberately a pure function with no Android dependencies so it can be unit tested
 * directly against fixture log lines, with no emulator or instrumentation required.
 *
 * A line that isn't a recognized fireEvent() call, or whose event name isn't one we
 * understand, returns null and is silently ignored by the caller - malformed or
 * not-yet-handled input here is an expected, routine outcome, not an error.
 */
class EsdeEventParser {

    fun parseLine(rawLine: String): EsdeEvent? {
        // Not a Scripting::fireEvent() line - ES-DE logs this one plainly as its window
        // is torn down and rebuilt (e.g. a secondary display attaching/detaching), so it
        // needs its own marker rather than going through the fireEvent parsing below.
        if (rawLine.contains(WINDOW_RELOAD_MARKER)) return EsdeEvent.Reload

        val markerIndex = rawLine.indexOf(FIRE_EVENT_MARKER)
        if (markerIndex == -1) return null

        val payload = rawLine.substring(markerIndex + FIRE_EVENT_MARKER.length).trim()
        val nameEnd = payload.indexOf(' ')
        if (nameEnd == -1) return null

        val eventName = payload.substring(0, nameEnd)
        val argsText = payload.substring(nameEnd + 1)
        val args = argsText.splitQuotedArgs()

        return toEvent(eventName, args)
    }

    private fun toEvent(eventName: String, args: List<String>): EsdeEvent? = when (eventName) {
        "system-select" -> args.toSystemSelect()

        "game-select" -> args.toGameArgs()?.let {
            EsdeEvent.GameSelect(it.romPath, it.gameName, it.systemShortName, it.systemFullName)
        }

        "game-start" -> args.toGameArgs()?.let {
            EsdeEvent.GameStart(it.romPath, it.gameName, it.systemShortName, it.systemFullName)
        }

        "game-end" -> args.toGameArgs()?.let {
            EsdeEvent.GameEnd(it.romPath, it.gameName, it.systemShortName, it.systemFullName)
        }

        "screensaver-start" -> args.getOrNull(0)?.let { EsdeEvent.ScreensaverStart(it) }

        "screensaver-game-select" -> args.toGameArgs()?.let {
            EsdeEvent.ScreensaverGameSelect(it.romPath, it.gameName, it.systemShortName, it.systemFullName)
        }

        "screensaver-end" -> args.getOrNull(0)?.let { EsdeEvent.ScreensaverEnd(it) }

        "quit" -> EsdeEvent.Quit

        else -> null
    }

    private fun List<String>.toSystemSelect(): EsdeEvent.SystemSelect? {
        val shortName = getOrNull(0) ?: return null
        return EsdeEvent.SystemSelect(
            systemShortName = shortName,
            systemFullName = getOrElse(1) { "" },
            systemPath = getOrElse(2) { "" },
        )
    }

    private fun List<String>.toGameArgs(): GameArgs? {
        val romPath = getOrNull(0) ?: return null
        return GameArgs(
            romPath = romPath,
            gameName = getOrElse(1) { "" },
            systemShortName = getOrElse(2) { "" },
            systemFullName = getOrElse(3) { "" },
        )
    }

    private data class GameArgs(
        val romPath: String,
        val gameName: String,
        val systemShortName: String,
        val systemFullName: String,
    )

    private companion object {
        const val FIRE_EVENT_MARKER = "Scripting::fireEvent():"
        const val WINDOW_RELOAD_MARKER = "Window size has changed from"

        // Splitting on this exact 3-char sequence (rather than matching independent
        // "..." groups) is what lets a field's own content safely contain unescaped
        // double quotes - e.g. ES-DE writes the game title `Ivan "Ironman" Stewart's
        // Super Off Road` with its embedded quotes completely unescaped. A field
        // boundary's closing-quote/space/opening-quote never appears as a run inside
        // real field content (title quoting is always "word"-adjacent, not
        // "word"-space-"word"), so this delimiter reliably identifies only true
        // boundaries and leaves embedded quotes in the middle of the split intact.
        const val FIELD_DELIMITER = "\" \""
        val BACKSLASH_ESCAPE_REGEX = Regex("""\\(.)""")

        fun String.splitQuotedArgs(): List<String> {
            val trimmed = trim()
            if (!trimmed.startsWith('"') || !trimmed.endsWith('"')) return emptyList()

            val pieces = trimmed.split(FIELD_DELIMITER)
            return pieces.mapIndexed { index, piece ->
                val stripped = piece
                    .let { if (index == 0) it.removePrefix("\"") else it }
                    .let { if (index == pieces.lastIndex) it.removeSuffix("\"") else it }
                stripped.unescapeBackslashes()
            }
        }

        fun String.unescapeBackslashes(): String =
            BACKSLASH_ESCAPE_REGEX.replace(this) { it.groupValues[1] }
    }
}
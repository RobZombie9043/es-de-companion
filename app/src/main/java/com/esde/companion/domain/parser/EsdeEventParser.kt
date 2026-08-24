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
        // Neither of these is a Scripting::fireEvent() line - ES-DE logs them plainly as
        // it tears down and rebuilds state (a window/display change, or a game-system
        // rescan triggered by e.g. importing a game via GuiGameImporter without a full
        // app restart), so they need their own markers rather than going through the
        // fireEvent parsing below.
        if (rawLine.contains(WINDOW_RELOAD_MARKER) || rawLine.contains(GAME_SYSTEMS_RELOAD_MARKER)) {
            return EsdeEvent.Reload
        }

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

    private fun toEvent(
        eventName: String,
        args: List<String>,
    ): EsdeEvent? =
        when (eventName) {
            "system-select" -> args.toSystemSelect()

            "game-select" ->
                args.toGameArgs()?.let {
                    EsdeEvent.GameSelect(it.romPath, it.gameName, it.systemShortName, it.systemFullName)
                }

            "game-start" ->
                args.toGameArgs()?.let {
                    EsdeEvent.GameStart(it.romPath, it.gameName, it.systemShortName, it.systemFullName)
                }

            "game-end" ->
                args.toGameArgs()?.let {
                    EsdeEvent.GameEnd(it.romPath, it.gameName, it.systemShortName, it.systemFullName)
                }

            "screensaver-start" -> args.getOrNull(0)?.let { EsdeEvent.ScreensaverStart(it) }

            "screensaver-game-select" ->
                args.toGameArgs()?.let {
                    EsdeEvent.ScreensaverGameSelect(it.romPath, it.gameName, it.systemShortName, it.systemFullName)
                }

            "screensaver-end" -> args.getOrNull(0)?.let { EsdeEvent.ScreensaverEnd(it) }

            "startup" -> EsdeEvent.Startup

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
        val romPath = getOrNull(GAME_ARG_ROM_PATH_INDEX) ?: return null
        return GameArgs(
            romPath = romPath,
            gameName = getOrElse(GAME_ARG_GAME_NAME_INDEX) { "" },
            systemShortName = getOrElse(GAME_ARG_SYSTEM_SHORT_NAME_INDEX) { "" },
            systemFullName = getOrElse(GAME_ARG_SYSTEM_FULL_NAME_INDEX) { "" },
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
        const val GAME_SYSTEMS_RELOAD_MARKER = "Populating game systems"

        const val GAME_ARG_ROM_PATH_INDEX = 0
        const val GAME_ARG_GAME_NAME_INDEX = 1
        const val GAME_ARG_SYSTEM_SHORT_NAME_INDEX = 2
        const val GAME_ARG_SYSTEM_FULL_NAME_INDEX = 3

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
                val stripped =
                    piece
                        .let { if (index == 0) it.removePrefix("\"") else it }
                        .let { if (index == pieces.lastIndex) it.removeSuffix("\"") else it }
                stripped.unescapeBackslashes()
            }
        }

        fun String.unescapeBackslashes(): String = BACKSLASH_ESCAPE_REGEX.replace(this) { it.groupValues[1] }
    }
}

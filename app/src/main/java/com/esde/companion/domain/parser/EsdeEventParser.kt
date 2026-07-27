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
        val markerIndex = rawLine.indexOf(FIRE_EVENT_MARKER)
        if (markerIndex == -1) return null

        val payload = rawLine.substring(markerIndex + FIRE_EVENT_MARKER.length).trim()
        val nameEnd = payload.indexOf(' ')
        if (nameEnd == -1) return null

        val eventName = payload.substring(0, nameEnd)
        val argsText = payload.substring(nameEnd + 1)
        val args = QUOTED_ARG_REGEX.findAll(argsText)
            .map { it.groupValues[1].unescapeBackslashes() }
            .toList()

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
        val QUOTED_ARG_REGEX = Regex("\"([^\"]*)\"")
        val BACKSLASH_ESCAPE_REGEX = Regex("""\\(.)""")

        fun String.unescapeBackslashes(): String =
            BACKSLASH_ESCAPE_REGEX.replace(this) { it.groupValues[1] }
    }
}
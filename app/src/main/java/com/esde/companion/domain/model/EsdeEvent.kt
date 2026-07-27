package com.esde.companion.domain.model

/**
 * Mirrors the literal event names ES-DE writes via Scripting::fireEvent() in es_log.txt.
 *
 * Each fireEvent() log line has the shape:
 *   Scripting::fireEvent(): <event-name> "<arg1>" "<arg2>" "<arg3>" "<arg4>"
 *
 * Unused positional args come through as empty strings ("") in the raw log - the parser
 * (see EsdeEventParser) maps them onto whichever fields are actually meaningful for that
 * event type.
 */
sealed class EsdeEvent {

    data class SystemSelect(
        val systemShortName: String,
        val systemFullName: String,
        val systemPath: String,
    ) : EsdeEvent()

    data class GameSelect(
        val romPath: String,
        val gameName: String,
        val systemShortName: String,
        val systemFullName: String,
    ) : EsdeEvent()

    data class GameStart(
        val romPath: String,
        val gameName: String,
        val systemShortName: String,
        val systemFullName: String,
    ) : EsdeEvent()

    data class GameEnd(
        val romPath: String,
        val gameName: String,
        val systemShortName: String,
        val systemFullName: String,
    ) : EsdeEvent()

    /** [mode] is whatever ES-DE reports as the trigger, e.g. "manual" or "timeout". */
    data class ScreensaverStart(val mode: String) : EsdeEvent()

    data class ScreensaverGameSelect(
        val romPath: String,
        val gameName: String,
        val systemShortName: String,
        val systemFullName: String,
    ) : EsdeEvent()

    /** [reason] is whatever ES-DE reports, e.g. "cancel" or "timeout". */
    data class ScreensaverEnd(val reason: String) : EsdeEvent()
}

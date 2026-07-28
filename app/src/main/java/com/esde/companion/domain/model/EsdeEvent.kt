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

    /** [mode] is whatever ES-DE reports as the trigger, e.g. "timer" or "manual". */
    data class ScreensaverStart(val mode: String) : EsdeEvent()

    data class ScreensaverGameSelect(
        val romPath: String,
        val gameName: String,
        val systemShortName: String,
        val systemFullName: String,
    ) : EsdeEvent()

    /** [reason] is whatever ES-DE reports, e.g. "cancel", "game-jump", or "game-start". */
    data class ScreensaverEnd(val reason: String) : EsdeEvent()
}

/**
 * Whether this event fully determines [AppState] on its own, independent of whatever
 * state came before it. Used only for startup - see EsdeLogFileRepository - to decide
 * how far back a cold-start replay needs to go: screensaver events specifically lean on
 * previousState/mode carried over from prior events (see AppStateReducer), so they are
 * never safe to treat as a starting point by themselves.
 */
fun EsdeEvent.isStartupAnchor(): Boolean = when (this) {
    is EsdeEvent.SystemSelect,
    is EsdeEvent.GameSelect,
    is EsdeEvent.GameStart,
    is EsdeEvent.GameEnd -> true

    is EsdeEvent.ScreensaverStart,
    is EsdeEvent.ScreensaverGameSelect,
    is EsdeEvent.ScreensaverEnd -> false
}
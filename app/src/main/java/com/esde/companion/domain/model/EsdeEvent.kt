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
 *
 * [Reload] is the one exception: it isn't a fireEvent() line at all, just an ordinary
 * ES-DE log line the parser also recognizes - see its own kdoc.
 */
sealed class EsdeEvent {

    data class SystemSelect(
        val systemShortName: String,
        val systemFullName: String,
        val systemPath: String,
        val direction: NavigationDirection? = null,
    ) : EsdeEvent()

    data class GameSelect(
        val romPath: String,
        val gameName: String,
        val systemShortName: String,
        val systemFullName: String,
        val direction: NavigationDirection? = null,
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

    /**
     * Fired as ES-DE begins its startup routine, which can take several seconds (theme/
     * gamelist parsing, etc.) before it fires a system-select or similar to say what's
     * actually on screen - see AppStateReducer, which resets to Idle for the duration.
     */
    data object Startup : EsdeEvent()

    /** Fired as ES-DE begins a clean shutdown - see AppStateReducer, which resets to Idle. */
    data object Quit : EsdeEvent()

    /**
     * ES-DE is rebuilding its UI/game-system list, either from a window/display size
     * change (e.g. a secondary display attaching/detaching, logged as "Window size has
     * changed from ... to ..., reloading...") or a game-system rescan (logged as
     * "Populating game systems..." - this also fires without a prior Startup/Quit, e.g.
     * after importing a game via GuiGameImporter). Neither is a Scripting::fireEvent()
     * line (see EsdeEventParser). What was on screen is stale until reload finishes, at
     * which point ES-DE re-fires a fresh system-select/game-select - see AppStateReducer,
     * which resets to Idle in the meantime, the same as Quit.
     */
    data object Reload : EsdeEvent()
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
    is EsdeEvent.GameEnd,
    is EsdeEvent.Startup,
    is EsdeEvent.Quit,
    is EsdeEvent.Reload -> true

    is EsdeEvent.ScreensaverStart,
    is EsdeEvent.ScreensaverGameSelect,
    is EsdeEvent.ScreensaverEnd -> false
}

/**
 * Attaches [direction] to whichever event variants render a directional logo slide -
 * SystemSelect/GameSelect - a no-op for every other variant. See NavigationDirectionTracker
 * for how [direction] is derived from the raw log.
 */
fun EsdeEvent.withDirection(direction: NavigationDirection?): EsdeEvent = when (this) {
    is EsdeEvent.SystemSelect -> copy(direction = direction)
    is EsdeEvent.GameSelect -> copy(direction = direction)
    is EsdeEvent.GameStart,
    is EsdeEvent.GameEnd,
    is EsdeEvent.ScreensaverStart,
    is EsdeEvent.ScreensaverGameSelect,
    is EsdeEvent.ScreensaverEnd,
    is EsdeEvent.Startup,
    is EsdeEvent.Quit,
    is EsdeEvent.Reload -> this
}
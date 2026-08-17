package com.esde.companion.domain.state

import com.esde.companion.domain.model.AppState
import com.esde.companion.domain.model.EsdeEvent
import com.esde.companion.domain.model.ScreensaverGame

/**
 * Pure reduction of (current state, new event) -> next state.
 *
 * Kept as a stateless object rather than a class with fields - there is no internal
 * state here, only a deterministic transformation, and that should stay true as this
 * evolves. Every branch of [EsdeEvent] is handled explicitly and without an `else`,
 * so adding a new event variant without updating this function is a compile error,
 * not a silently-ignored runtime gap.
 */
object AppStateReducer {
    fun reduce(
        current: AppState,
        event: EsdeEvent,
    ): AppState =
        when (event) {
            is EsdeEvent.SystemSelect ->
                AppState.BrowsingSystem(
                    systemShortName = event.systemShortName,
                    systemFullName = event.systemFullName,
                    systemPath = event.systemPath,
                    navigationDirection = event.direction,
                )

            is EsdeEvent.GameSelect -> {
                // ES-DE fires a spurious game-select for the currently-playing ROM shortly
                // after game-start (it re-touches the gamelist entry, e.g. play-count/time
                // metadata, without the game actually having exited back to the frontend).
                // There's no game-end preceding it, so treat it as a no-op rather than
                // dropping back to BrowsingGame while the game is still running.
                if (current is AppState.PlayingGame && current.romPath == event.romPath) {
                    current
                } else {
                    AppState.BrowsingGame(
                        romPath = event.romPath,
                        gameName = event.gameName,
                        systemShortName = event.systemShortName,
                        systemFullName = event.systemFullName,
                        navigationDirection = event.direction,
                        systemPath = current.systemPathFor(event.systemShortName),
                    )
                }
            }

            is EsdeEvent.GameStart ->
                AppState.PlayingGame(
                    romPath = event.romPath,
                    gameName = event.gameName,
                    systemShortName = event.systemShortName,
                    systemFullName = event.systemFullName,
                    systemPath = current.systemPathFor(event.systemShortName),
                )

            is EsdeEvent.GameEnd ->
                AppState.BrowsingGame(
                    romPath = event.romPath,
                    gameName = event.gameName,
                    systemShortName = event.systemShortName,
                    systemFullName = event.systemFullName,
                    systemPath = current.systemPathFor(event.systemShortName),
                )

            is EsdeEvent.ScreensaverStart ->
                AppState.Screensaver(
                    mode = event.mode,
                    currentGame = null,
                    // Don't nest: if we're already in a screensaver (shouldn't normally happen,
                    // but ES-DE's event ordering has already surprised us once), preserve the
                    // original previousState rather than overwriting it with the screensaver itself.
                    previousState = (current as? AppState.Screensaver)?.previousState ?: current,
                )

            is EsdeEvent.ScreensaverGameSelect -> {
                val game =
                    ScreensaverGame(
                        romPath = event.romPath,
                        gameName = event.gameName,
                        systemShortName = event.systemShortName,
                        systemFullName = event.systemFullName,
                        systemPath = current.systemPathFor(event.systemShortName),
                    )
                // A screensaver-game-select can arrive without a preceding screensaver-start
                // being observed (e.g. the app was launched mid-screensaver) - fall back to
                // an "unknown" mode and Idle as the previousState rather than dropping the
                // event or crashing.
                val existing = current as? AppState.Screensaver
                AppState.Screensaver(
                    mode = existing?.mode ?: "unknown",
                    currentGame = game,
                    previousState = existing?.previousState ?: AppState.Idle,
                )
            }

            is EsdeEvent.ScreensaverEnd ->
                // Restore whatever was on screen before the screensaver started - ES-DE does
                // not reliably re-fire system-select/game-select on exit. If we somehow got
                // a screensaver-end without ever seeing screensaver-start (current isn't a
                // Screensaver), Idle is the only safe fallback.
                (current as? AppState.Screensaver)?.previousState ?: AppState.Idle

            // ES-DE has just begun its startup routine, which can take several seconds before
            // it fires a system-select/game-select saying what's actually on screen - whatever
            // was on screen before (e.g. left over from before an ungraceful restart with no
            // preceding quit) is no longer real, so drop to Idle for the duration.
            is EsdeEvent.Startup -> AppState.Idle

            // ES-DE is shutting down - whatever was on screen is no longer real, regardless
            // of what it was. See WidgetOverlay's Disconnected case for the resulting UI.
            is EsdeEvent.Quit -> AppState.Idle

            // ES-DE is rebuilding its UI (window/display size change) - whatever was on
            // screen is stale until it re-fires system-select/game-select once reload
            // finishes, so drop to Idle in the meantime, same as Quit.
            is EsdeEvent.Reload -> AppState.Idle
        }

    /**
     * ES-DE's `game-select`/`game-start`/`game-end` fireEvent() lines don't carry the
     * system's ROM directory the way `system-select` does (see [EsdeEvent.SystemSelect]) -
     * only `systemShortName`. Since browsing into a system (firing system-select) always
     * precedes selecting a game within it, [current] - or, for a repeated screensaver, its
     * carried-over previousState - already holds the right systemPath for a matching system
     * whenever one has been observed this session; this is how GameMediaPathResolver gets an
     * authoritative ROM root without depending on the ROM folder's name on disk (see its
     * kdoc). Returns null only when no matching system-select has been seen yet (e.g. a
     * cold-start replay anchored directly on a game event) - GameMediaPathResolver falls
     * back to a marker search in that case.
     */
    private fun AppState.systemPathFor(systemShortName: String): String? =
        when (this) {
            is AppState.BrowsingSystem -> systemPath.takeIf { this.systemShortName == systemShortName }
            is AppState.BrowsingGame -> systemPath?.takeIf { this.systemShortName == systemShortName }
            is AppState.PlayingGame -> systemPath?.takeIf { this.systemShortName == systemShortName }
            is AppState.Screensaver -> previousState.systemPathFor(systemShortName)
            is AppState.Idle -> null
        }
}

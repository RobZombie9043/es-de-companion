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

    fun reduce(current: AppState, event: EsdeEvent): AppState = when (event) {
        is EsdeEvent.SystemSelect -> AppState.BrowsingSystem(
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
                )
            }
        }

        is EsdeEvent.GameStart -> AppState.PlayingGame(
            romPath = event.romPath,
            gameName = event.gameName,
            systemShortName = event.systemShortName,
            systemFullName = event.systemFullName,
        )

        is EsdeEvent.GameEnd -> AppState.BrowsingGame(
            romPath = event.romPath,
            gameName = event.gameName,
            systemShortName = event.systemShortName,
            systemFullName = event.systemFullName,
        )

        is EsdeEvent.ScreensaverStart -> AppState.Screensaver(
            mode = event.mode,
            currentGame = null,
            // Don't nest: if we're already in a screensaver (shouldn't normally happen,
            // but ES-DE's event ordering has already surprised us once), preserve the
            // original previousState rather than overwriting it with the screensaver itself.
            previousState = (current as? AppState.Screensaver)?.previousState ?: current,
        )

        is EsdeEvent.ScreensaverGameSelect -> {
            val game = ScreensaverGame(
                romPath = event.romPath,
                gameName = event.gameName,
                systemShortName = event.systemShortName,
                systemFullName = event.systemFullName,
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
    }
}
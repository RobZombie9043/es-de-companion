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
        )

        is EsdeEvent.GameSelect -> AppState.BrowsingGame(
            romPath = event.romPath,
            gameName = event.gameName,
            systemShortName = event.systemShortName,
            systemFullName = event.systemFullName,
        )

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
            // an "unknown" mode rather than dropping the event or crashing.
            val mode = (current as? AppState.Screensaver)?.mode ?: "unknown"
            AppState.Screensaver(mode = mode, currentGame = game)
        }

        is EsdeEvent.ScreensaverEnd -> AppState.Idle
    }
}

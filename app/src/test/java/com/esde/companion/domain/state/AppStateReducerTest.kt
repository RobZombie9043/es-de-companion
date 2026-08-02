package com.esde.companion.domain.state

import com.esde.companion.domain.model.AppState
import com.esde.companion.domain.model.EsdeEvent
import com.esde.companion.domain.model.NavigationDirection
import com.esde.companion.domain.model.ScreensaverGame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppStateReducerTest {

    @Test
    fun `system-select produces BrowsingSystem`() {
        val event = EsdeEvent.SystemSelect("dreamcast", "Sega Dreamcast", "/roms/dreamcast")

        val result = AppStateReducer.reduce(AppState.Idle, event)

        assertEquals(
            AppState.BrowsingSystem("dreamcast", "Sega Dreamcast", "/roms/dreamcast"),
            result,
        )
    }

    @Test
    fun `game-select produces BrowsingGame`() {
        val event = EsdeEvent.GameSelect("/roms/dc/game.chd", "Game", "dreamcast", "Sega Dreamcast")

        val result = AppStateReducer.reduce(AppState.Idle, event)

        assertEquals(
            AppState.BrowsingGame("/roms/dc/game.chd", "Game", "dreamcast", "Sega Dreamcast"),
            result,
        )
    }

    @Test
    fun `system-select carries the event's navigation direction into BrowsingSystem`() {
        val event = EsdeEvent.SystemSelect("dreamcast", "Sega Dreamcast", "/roms/dreamcast", direction = NavigationDirection.Right)

        val result = AppStateReducer.reduce(AppState.Idle, event)

        assertEquals(
            AppState.BrowsingSystem("dreamcast", "Sega Dreamcast", "/roms/dreamcast", navigationDirection = NavigationDirection.Right),
            result,
        )
    }

    @Test
    fun `game-select carries the event's navigation direction into BrowsingGame`() {
        val event = EsdeEvent.GameSelect("/roms/dc/game.chd", "Game", "dreamcast", "Sega Dreamcast", direction = NavigationDirection.Up)

        val result = AppStateReducer.reduce(AppState.Idle, event)

        assertEquals(
            AppState.BrowsingGame("/roms/dc/game.chd", "Game", "dreamcast", "Sega Dreamcast", navigationDirection = NavigationDirection.Up),
            result,
        )
    }

    @Test
    fun `system-select with no known direction produces a null navigationDirection`() {
        val event = EsdeEvent.SystemSelect("dreamcast", "Sega Dreamcast", "/roms/dreamcast")

        val result = AppStateReducer.reduce(AppState.Idle, event) as AppState.BrowsingSystem

        assertNull(result.navigationDirection)
    }

    @Test
    fun `game-end produces a null navigationDirection - no controller press precedes quitting a game`() {
        val playing = AppState.PlayingGame("/roms/dc/game.chd", "Game", "dreamcast", "Sega Dreamcast")
        val event = EsdeEvent.GameEnd("/roms/dc/game.chd", "Game", "dreamcast", "Sega Dreamcast")

        val result = AppStateReducer.reduce(playing, event) as AppState.BrowsingGame

        assertNull(result.navigationDirection)
    }

    @Test
    fun `game-select for the same rom while playing is ignored`() {
        val playing = AppState.PlayingGame("/roms/dc/game.chd", "Game", "dreamcast", "Sega Dreamcast")
        val event = EsdeEvent.GameSelect("/roms/dc/game.chd", "Game", "dreamcast", "Sega Dreamcast")

        val result = AppStateReducer.reduce(playing, event)

        assertEquals(playing, result)
    }

    @Test
    fun `game-select for a different rom while playing transitions to BrowsingGame`() {
        val playing = AppState.PlayingGame("/roms/dc/game.chd", "Game", "dreamcast", "Sega Dreamcast")
        val event = EsdeEvent.GameSelect("/roms/dc/other.chd", "Other", "dreamcast", "Sega Dreamcast")

        val result = AppStateReducer.reduce(playing, event)

        assertEquals(
            AppState.BrowsingGame("/roms/dc/other.chd", "Other", "dreamcast", "Sega Dreamcast"),
            result,
        )
    }

    @Test
    fun `game-start transitions from BrowsingGame to PlayingGame`() {
        val browsing = AppState.BrowsingGame("/roms/dc/game.chd", "Game", "dreamcast", "Sega Dreamcast")
        val event = EsdeEvent.GameStart("/roms/dc/game.chd", "Game", "dreamcast", "Sega Dreamcast")

        val result = AppStateReducer.reduce(browsing, event)

        assertEquals(
            AppState.PlayingGame("/roms/dc/game.chd", "Game", "dreamcast", "Sega Dreamcast"),
            result,
        )
    }

    @Test
    fun `game-end returns to BrowsingGame`() {
        val playing = AppState.PlayingGame("/roms/dc/game.chd", "Game", "dreamcast", "Sega Dreamcast")
        val event = EsdeEvent.GameEnd("/roms/dc/game.chd", "Game", "dreamcast", "Sega Dreamcast")

        val result = AppStateReducer.reduce(playing, event)

        assertEquals(
            AppState.BrowsingGame("/roms/dc/game.chd", "Game", "dreamcast", "Sega Dreamcast"),
            result,
        )
    }

    @Test
    fun `screensaver-start captures the current state as previousState`() {
        val previous = AppState.BrowsingSystem("gc", "Nintendo GameCube", "/roms/gc")

        val result = AppStateReducer.reduce(previous, EsdeEvent.ScreensaverStart("manual"))

        assertEquals(
            AppState.Screensaver(mode = "manual", currentGame = null, previousState = previous),
            result,
        )
    }

    @Test
    fun `screensaver-game-select updates the current game and preserves mode and previousState`() {
        val previous = AppState.BrowsingSystem("gc", "Nintendo GameCube", "/roms/gc")
        val screensaver = AppState.Screensaver(mode = "manual", currentGame = null, previousState = previous)
        val event = EsdeEvent.ScreensaverGameSelect("/roms/arcade/tapper.zip", "Tapper", "arcade", "Arcade")

        val result = AppStateReducer.reduce(screensaver, event)

        assertEquals(
            AppState.Screensaver(
                mode = "manual",
                currentGame = ScreensaverGame("/roms/arcade/tapper.zip", "Tapper", "arcade", "Arcade"),
                previousState = previous,
            ),
            result,
        )
    }

    @Test
    fun `screensaver-game-select without a prior screensaver-start falls back to unknown mode and Idle previousState`() {
        val event = EsdeEvent.ScreensaverGameSelect("/roms/arcade/tapper.zip", "Tapper", "arcade", "Arcade")

        val result = AppStateReducer.reduce(AppState.Idle, event)

        assertEquals(
            AppState.Screensaver(
                mode = "unknown",
                currentGame = ScreensaverGame("/roms/arcade/tapper.zip", "Tapper", "arcade", "Arcade"),
                previousState = AppState.Idle,
            ),
            result,
        )
    }

    @Test
    fun `screensaver-end restores the state active before the screensaver started`() {
        val previous = AppState.BrowsingSystem("gc", "Nintendo GameCube", "/roms/gc")
        val screensaver = AppState.Screensaver(
            mode = "manual",
            currentGame = ScreensaverGame("/roms/arcade/xexex.zip", "Xexex", "arcade", "Arcade"),
            previousState = previous,
        )

        val result = AppStateReducer.reduce(screensaver, EsdeEvent.ScreensaverEnd("cancel"))

        assertEquals(previous, result)
    }

    @Test
    fun `screensaver-end with no prior screensaver-start falls back to Idle`() {
        val result = AppStateReducer.reduce(AppState.Idle, EsdeEvent.ScreensaverEnd("cancel"))

        assertEquals(AppState.Idle, result)
    }
}

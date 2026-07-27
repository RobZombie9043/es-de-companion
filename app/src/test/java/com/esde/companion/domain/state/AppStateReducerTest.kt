package com.esde.companion.domain.state

import com.esde.companion.domain.model.AppState
import com.esde.companion.domain.model.EsdeEvent
import com.esde.companion.domain.model.ScreensaverGame
import org.junit.Assert.assertEquals
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
    fun `screensaver-start from any prior state produces Screensaver with no current game`() {
        val previous = AppState.BrowsingSystem("dreamcast", "Sega Dreamcast", "/roms/dreamcast")

        val result = AppStateReducer.reduce(previous, EsdeEvent.ScreensaverStart("manual"))

        assertEquals(AppState.Screensaver(mode = "manual", currentGame = null), result)
    }

    @Test
    fun `screensaver-game-select updates the current game and preserves the existing mode`() {
        val screensaver = AppState.Screensaver(mode = "manual", currentGame = null)
        val event = EsdeEvent.ScreensaverGameSelect("/roms/arcade/tapper.zip", "Tapper", "arcade", "Arcade")

        val result = AppStateReducer.reduce(screensaver, event)

        assertEquals(
            AppState.Screensaver(
                mode = "manual",
                currentGame = ScreensaverGame("/roms/arcade/tapper.zip", "Tapper", "arcade", "Arcade"),
            ),
            result,
        )
    }

    @Test
    fun `screensaver-game-select without a prior screensaver-start falls back to an unknown mode`() {
        val event = EsdeEvent.ScreensaverGameSelect("/roms/arcade/tapper.zip", "Tapper", "arcade", "Arcade")

        val result = AppStateReducer.reduce(AppState.Idle, event)

        assertEquals(
            AppState.Screensaver(
                mode = "unknown",
                currentGame = ScreensaverGame("/roms/arcade/tapper.zip", "Tapper", "arcade", "Arcade"),
            ),
            result,
        )
    }

    @Test
    fun `screensaver-end returns to Idle`() {
        val screensaver = AppState.Screensaver(mode = "manual", currentGame = null)

        val result = AppStateReducer.reduce(screensaver, EsdeEvent.ScreensaverEnd("cancel"))

        assertEquals(AppState.Idle, result)
    }
}

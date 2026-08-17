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
        val screensaver =
            AppState.Screensaver(
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

    @Test
    fun `startup resets to Idle from a game in progress`() {
        val playing = AppState.PlayingGame("/roms/dc/game.chd", "Game", "dreamcast", "Sega Dreamcast")

        val result = AppStateReducer.reduce(playing, EsdeEvent.Startup)

        assertEquals(AppState.Idle, result)
    }

    @Test
    fun `startup resets to Idle from mid-screensaver`() {
        val previous = AppState.BrowsingSystem("gc", "Nintendo GameCube", "/roms/gc")
        val screensaver = AppState.Screensaver(mode = "manual", currentGame = null, previousState = previous)

        val result = AppStateReducer.reduce(screensaver, EsdeEvent.Startup)

        assertEquals(AppState.Idle, result)
    }

    @Test
    fun `quit resets to Idle from a game in progress`() {
        val playing = AppState.PlayingGame("/roms/dc/game.chd", "Game", "dreamcast", "Sega Dreamcast")

        val result = AppStateReducer.reduce(playing, EsdeEvent.Quit)

        assertEquals(AppState.Idle, result)
    }

    @Test
    fun `quit resets to Idle from mid-screensaver`() {
        val previous = AppState.BrowsingSystem("gc", "Nintendo GameCube", "/roms/gc")
        val screensaver = AppState.Screensaver(mode = "manual", currentGame = null, previousState = previous)

        val result = AppStateReducer.reduce(screensaver, EsdeEvent.Quit)

        assertEquals(AppState.Idle, result)
    }

    @Test
    fun `reload resets to Idle from a game in progress`() {
        val playing = AppState.PlayingGame("/roms/dc/game.chd", "Game", "dreamcast", "Sega Dreamcast")

        val result = AppStateReducer.reduce(playing, EsdeEvent.Reload)

        assertEquals(AppState.Idle, result)
    }

    @Test
    fun `reload resets to Idle from mid-screensaver`() {
        val previous = AppState.BrowsingSystem("gc", "Nintendo GameCube", "/roms/gc")
        val screensaver = AppState.Screensaver(mode = "manual", currentGame = null, previousState = previous)

        val result = AppStateReducer.reduce(screensaver, EsdeEvent.Reload)

        assertEquals(AppState.Idle, result)
    }

    // GameMediaPathResolver needs the system's real ROM directory (only ever reported via
    // system-select) to resolve media correctly when a system's ROM folder isn't named after
    // its shortname - see GameMediaPathResolver's kdoc. These verify the reducer actually
    // carries that systemPath forward onto BrowsingGame/PlayingGame/ScreensaverGame, since
    // game-select/game-start/game-end/screensaver-game-select never report it themselves.

    @Test
    fun `game-select carries systemPath forward from a matching BrowsingSystem`() {
        val browsingSystem = AppState.BrowsingSystem("nds", "Nintendo DS", "/storage/E2AB-E84A/ROMs/DS")
        val event = EsdeEvent.GameSelect("/storage/E2AB-E84A/ROMs/DS/dummy.zip", "Dummy", "nds", "Nintendo DS")

        val result = AppStateReducer.reduce(browsingSystem, event) as AppState.BrowsingGame

        assertEquals("/storage/E2AB-E84A/ROMs/DS", result.systemPath)
    }

    @Test
    fun `game-select does not carry systemPath forward from a different system`() {
        val browsingSystem = AppState.BrowsingSystem("nds", "Nintendo DS", "/storage/E2AB-E84A/ROMs/DS")
        val event = EsdeEvent.GameSelect("/roms/psx/game.chd", "Game", "psx", "Sony PlayStation")

        val result = AppStateReducer.reduce(browsingSystem, event) as AppState.BrowsingGame

        assertNull(result.systemPath)
    }

    @Test
    fun `game-start carries systemPath forward from BrowsingGame`() {
        val browsing =
            AppState.BrowsingGame(
                "/storage/E2AB-E84A/ROMs/DS/dummy.zip",
                "Dummy",
                "nds",
                "Nintendo DS",
                systemPath = "/storage/E2AB-E84A/ROMs/DS",
            )
        val event = EsdeEvent.GameStart("/storage/E2AB-E84A/ROMs/DS/dummy.zip", "Dummy", "nds", "Nintendo DS")

        val result = AppStateReducer.reduce(browsing, event) as AppState.PlayingGame

        assertEquals("/storage/E2AB-E84A/ROMs/DS", result.systemPath)
    }

    @Test
    fun `game-end carries systemPath forward from PlayingGame`() {
        val playing =
            AppState.PlayingGame(
                "/storage/E2AB-E84A/ROMs/DS/dummy.zip",
                "Dummy",
                "nds",
                "Nintendo DS",
                systemPath = "/storage/E2AB-E84A/ROMs/DS",
            )
        val event = EsdeEvent.GameEnd("/storage/E2AB-E84A/ROMs/DS/dummy.zip", "Dummy", "nds", "Nintendo DS")

        val result = AppStateReducer.reduce(playing, event) as AppState.BrowsingGame

        assertEquals("/storage/E2AB-E84A/ROMs/DS", result.systemPath)
    }

    @Test
    fun `screensaver-game-select carries systemPath forward from the state active before the screensaver started`() {
        val previous = AppState.BrowsingSystem("arcade", "Arcade", "/roms/arcade")
        val screensaver = AppState.Screensaver(mode = "manual", currentGame = null, previousState = previous)
        val event = EsdeEvent.ScreensaverGameSelect("/roms/arcade/tapper.zip", "Tapper", "arcade", "Arcade")

        val result = AppStateReducer.reduce(screensaver, event) as AppState.Screensaver

        assertEquals("/roms/arcade", result.currentGame?.systemPath)
    }
}

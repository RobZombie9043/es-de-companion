package com.esde.companion.domain.music

import com.esde.companion.domain.model.AppState
import com.esde.companion.domain.model.MusicPool
import com.esde.companion.domain.model.ScreensaverGame
import org.junit.Assert.assertEquals
import org.junit.Test

class MusicEligibilityResolverTest {

    private val allEnabled = MusicSettings(enabled = true, systems = true, games = true, screensaver = true)
    private val allDisabled = MusicSettings(enabled = false, systems = true, games = true, screensaver = true)

    @Test
    fun `Idle is always ineligible regardless of toggles`() {
        assertEquals(MusicEligibility.Ineligible, MusicEligibilityResolver.resolve(AppState.Idle, allEnabled))
    }

    @Test
    fun `BrowsingSystem is eligible for its own PerSystem pool when the systems toggle is on`() {
        val state = AppState.BrowsingSystem("dreamcast", "Sega Dreamcast", "/roms/dreamcast")

        assertEquals(
            MusicEligibility.Eligible(MusicPool.PerSystem("dreamcast")),
            MusicEligibilityResolver.resolve(state, allEnabled),
        )
    }

    @Test
    fun `BrowsingSystem is ineligible when the systems toggle is off`() {
        val state = AppState.BrowsingSystem("dreamcast", "Sega Dreamcast", "/roms/dreamcast")
        val settings = allEnabled.copy(systems = false)

        assertEquals(MusicEligibility.Ineligible, MusicEligibilityResolver.resolve(state, settings))
    }

    @Test
    fun `BrowsingGame is eligible for its own PerSystem pool when the games toggle is on`() {
        val state = AppState.BrowsingGame(
            romPath = "/roms/psx/Final Fantasy IX (USA).chd",
            gameName = "Final Fantasy IX",
            systemShortName = "psx",
            systemFullName = "Sony PlayStation",
        )

        assertEquals(
            MusicEligibility.Eligible(MusicPool.PerSystem("psx")),
            MusicEligibilityResolver.resolve(state, allEnabled),
        )
    }

    @Test
    fun `BrowsingGame is ineligible when the games toggle is off`() {
        val state = AppState.BrowsingGame(
            romPath = "/roms/psx/Final Fantasy IX (USA).chd",
            gameName = "Final Fantasy IX",
            systemShortName = "psx",
            systemFullName = "Sony PlayStation",
        )
        val settings = allEnabled.copy(games = false)

        assertEquals(MusicEligibility.Ineligible, MusicEligibilityResolver.resolve(state, settings))
    }

    @Test
    fun `PlayingGame is always ineligible regardless of toggles`() {
        val state = AppState.PlayingGame(
            romPath = "/roms/psx/Final Fantasy IX (USA).chd",
            gameName = "Final Fantasy IX",
            systemShortName = "psx",
            systemFullName = "Sony PlayStation",
        )

        assertEquals(MusicEligibility.Ineligible, MusicEligibilityResolver.resolve(state, allEnabled))
    }

    @Test
    fun `Screensaver is eligible for the General pool when the screensaver toggle is on`() {
        val state = AppState.Screensaver(mode = "video", currentGame = null, previousState = AppState.Idle)

        assertEquals(
            MusicEligibility.Eligible(MusicPool.General),
            MusicEligibilityResolver.resolve(state, allEnabled),
        )
    }

    @Test
    fun `Screensaver ignores currentGame and previousState tied to a specific system - always General`() {
        val previous = AppState.BrowsingSystem("dreamcast", "Sega Dreamcast", "/roms/dreamcast")
        val currentGame = ScreensaverGame(
            romPath = "/roms/psx/Final Fantasy IX (USA).chd",
            gameName = "Final Fantasy IX",
            systemShortName = "psx",
            systemFullName = "Sony PlayStation",
        )
        val state = AppState.Screensaver(mode = "video", currentGame = currentGame, previousState = previous)

        assertEquals(
            MusicEligibility.Eligible(MusicPool.General),
            MusicEligibilityResolver.resolve(state, allEnabled),
        )
    }

    @Test
    fun `Screensaver is ineligible when the screensaver toggle is off`() {
        val state = AppState.Screensaver(mode = "video", currentGame = null, previousState = AppState.Idle)
        val settings = allEnabled.copy(screensaver = false)

        assertEquals(MusicEligibility.Ineligible, MusicEligibilityResolver.resolve(state, settings))
    }

    @Test
    fun `master toggle off makes every state ineligible`() {
        val states = listOf(
            AppState.Idle,
            AppState.BrowsingSystem("dreamcast", "Sega Dreamcast", "/roms/dreamcast"),
            AppState.BrowsingGame("/roms/psx/game.chd", "Game", "psx", "Sony PlayStation"),
            AppState.PlayingGame("/roms/psx/game.chd", "Game", "psx", "Sony PlayStation"),
            AppState.Screensaver(mode = "video", currentGame = null, previousState = AppState.Idle),
        )

        states.forEach { state ->
            assertEquals(MusicEligibility.Ineligible, MusicEligibilityResolver.resolve(state, allDisabled))
        }
    }
}

package com.esde.companion.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RetroAchievementsScreensaverFreezeTest {
    private val browsingGame =
        AppState.BrowsingGame(
            romPath = "/roms/snes/game.sfc",
            gameName = "Game",
            systemShortName = "snes",
            systemFullName = "Super Nintendo",
        )
    private val browsingGameReference = GameReference("snes", "/roms/snes/game.sfc") to "Game"

    private val browsingSystem =
        AppState.BrowsingSystem(
            systemShortName = "snes",
            systemFullName = "Super Nintendo",
            systemPath = "/roms/snes",
        )

    private val screensaverGame =
        ScreensaverGame(
            romPath = "/roms/genesis/other.md",
            gameName = "Other Game",
            systemShortName = "genesis",
            systemFullName = "Sega Genesis",
        )

    private val screensaverWithOtherGame =
        AppState.Screensaver(
            mode = "video",
            currentGame = screensaverGame,
            previousState = browsingGame,
        )

    private val screensaverWithNoGame =
        AppState.Screensaver(mode = "video", currentGame = null, previousState = browsingSystem)

    // resolveAchievementsGame

    @Test
    fun `resolveAchievementsGame with toggle on switches to the screensaver's game`() {
        val result =
            resolveAchievementsGame(
                screensaverWithOtherGame,
                browsingGameReference,
                updateOnScreensaver = true,
            )

        assertEquals(GameReference("genesis", "/roms/genesis/other.md") to "Other Game", result)
    }

    @Test
    fun `resolveAchievementsGame with toggle on and no screensaver game resolves to null`() {
        val result = resolveAchievementsGame(screensaverWithNoGame, browsingGameReference, updateOnScreensaver = true)

        assertNull(result)
    }

    @Test
    fun `resolveAchievementsGame with toggle off holds the previous game during a screensaver`() {
        val result =
            resolveAchievementsGame(
                screensaverWithOtherGame,
                browsingGameReference,
                updateOnScreensaver = false,
            )

        assertEquals(browsingGameReference, result)
    }

    @Test
    fun `resolveAchievementsGame with toggle off but no previous game resolves to null`() {
        val result = resolveAchievementsGame(screensaverWithOtherGame, null, updateOnScreensaver = false)

        assertNull(result)
    }

    @Test
    fun `resolveAchievementsGame with toggle off passes through a non-screensaver state`() {
        val result = resolveAchievementsGame(browsingGame, null, updateOnScreensaver = false)

        assertEquals(browsingGameReference, result)
    }

    @Test
    fun `resolveAchievementsGame resolves to null when disconnected regardless of toggle`() {
        assertNull(resolveAchievementsGame(null, browsingGameReference, updateOnScreensaver = true))
        assertNull(resolveAchievementsGame(null, browsingGameReference, updateOnScreensaver = false))
    }

    // resolveAchievementsSystem

    @Test
    fun `resolveAchievementsSystem with toggle on drops to null during any screensaver`() {
        val result = resolveAchievementsSystem(screensaverWithOtherGame, browsingSystem, updateOnScreensaver = true)

        assertNull(result)
    }

    @Test
    fun `resolveAchievementsSystem with toggle off holds the previous system during a screensaver`() {
        val result = resolveAchievementsSystem(screensaverWithOtherGame, browsingSystem, updateOnScreensaver = false)

        assertEquals(browsingSystem, result)
    }

    @Test
    fun `resolveAchievementsSystem with toggle off but no previous system resolves to null`() {
        val result = resolveAchievementsSystem(screensaverWithNoGame, null, updateOnScreensaver = false)

        assertNull(result)
    }

    @Test
    fun `resolveAchievementsSystem with toggle off passes through a non-screensaver state`() {
        val result = resolveAchievementsSystem(browsingSystem, null, updateOnScreensaver = false)

        assertEquals(browsingSystem, result)
    }

    @Test
    fun `resolveAchievementsSystem resolves to null when disconnected regardless of toggle`() {
        assertNull(resolveAchievementsSystem(null, browsingSystem, updateOnScreensaver = true))
        assertNull(resolveAchievementsSystem(null, browsingSystem, updateOnScreensaver = false))
    }
}

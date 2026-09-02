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
    private val screensaverGameReference = GameReference("genesis", "/roms/genesis/other.md") to "Other Game"

    private val screensaverWithOtherGame =
        AppState.Screensaver(
            mode = "video",
            currentGame = screensaverGame,
            previousState = browsingGame,
        )

    private val screensaverWithNoGame =
        AppState.Screensaver(mode = "video", currentGame = null, previousState = browsingSystem)

    // resolveScreensaverAwareGame

    @Test
    fun `resolveScreensaverAwareGame with toggle on switches to the screensaver's game`() {
        val result =
            resolveScreensaverAwareGame(
                screensaverWithOtherGame,
                browsingGameReference,
                updateOnScreensaver = true,
                visible = true,
            )

        assertEquals(screensaverGameReference, result)
    }

    @Test
    fun `resolveScreensaverAwareGame with toggle on and no screensaver game resolves to null`() {
        val result =
            resolveScreensaverAwareGame(
                screensaverWithNoGame,
                browsingGameReference,
                updateOnScreensaver = true,
                visible = true,
            )

        assertNull(result)
    }

    @Test
    fun `resolveScreensaverAwareGame with toggle off and visible holds the previous game during a screensaver`() {
        val result =
            resolveScreensaverAwareGame(
                screensaverWithOtherGame,
                browsingGameReference,
                updateOnScreensaver = false,
                visible = true,
            )

        assertEquals(browsingGameReference, result)
    }

    @Test
    fun `resolveScreensaverAwareGame with no previous game falls back to the screensaver's game when visible`() {
        val result =
            resolveScreensaverAwareGame(screensaverWithOtherGame, null, updateOnScreensaver = false, visible = true)

        assertEquals(screensaverGameReference, result)
    }

    @Test
    fun `resolveScreensaverAwareGame with no previous game and no screensaver game resolves to null`() {
        val result =
            resolveScreensaverAwareGame(screensaverWithNoGame, null, updateOnScreensaver = false, visible = true)

        assertNull(result)
    }

    @Test
    fun `resolveScreensaverAwareGame not visible ignores a stale previous game and tracks the screensaver`() {
        // Simulates opening the achievements page while a screensaver is already playing: the
        // page was not visible before now, so whatever it holds from before the screensaver
        // started must not surface - it should show the game the screensaver is showing right now.
        val result =
            resolveScreensaverAwareGame(
                screensaverWithOtherGame,
                browsingGameReference,
                updateOnScreensaver = false,
                visible = false,
            )

        assertEquals(screensaverGameReference, result)
    }

    @Test
    fun `resolveScreensaverAwareGame with toggle off passes through a non-screensaver state regardless of visible`() {
        assertEquals(
            browsingGameReference,
            resolveScreensaverAwareGame(browsingGame, null, updateOnScreensaver = false, visible = true),
        )
        assertEquals(
            browsingGameReference,
            resolveScreensaverAwareGame(browsingGame, null, updateOnScreensaver = false, visible = false),
        )
    }

    @Test
    fun `resolveScreensaverAwareGame resolves to null when disconnected regardless of toggle or visibility`() {
        assertNull(
            resolveScreensaverAwareGame(null, browsingGameReference, updateOnScreensaver = true, visible = true),
        )
        assertNull(
            resolveScreensaverAwareGame(null, browsingGameReference, updateOnScreensaver = false, visible = true),
        )
        assertNull(
            resolveScreensaverAwareGame(null, browsingGameReference, updateOnScreensaver = false, visible = false),
        )
    }

    // resolveAchievementsSystem

    @Test
    fun `resolveAchievementsSystem with toggle on drops to null during any screensaver`() {
        val result =
            resolveAchievementsSystem(
                screensaverWithOtherGame,
                browsingSystem,
                updateOnScreensaver = true,
                visible = true,
            )

        assertNull(result)
    }

    @Test
    fun `resolveAchievementsSystem with toggle off and visible holds the previous system during a screensaver`() {
        val result =
            resolveAchievementsSystem(
                screensaverWithOtherGame,
                browsingSystem,
                updateOnScreensaver = false,
                visible = true,
            )

        assertEquals(browsingSystem, result)
    }

    @Test
    fun `resolveAchievementsSystem with toggle off but not visible does not hold the previous system`() {
        val result =
            resolveAchievementsSystem(
                screensaverWithOtherGame,
                browsingSystem,
                updateOnScreensaver = false,
                visible = false,
            )

        assertNull(result)
    }

    @Test
    fun `resolveAchievementsSystem with toggle off, visible, but no previous system resolves to null`() {
        val result =
            resolveAchievementsSystem(screensaverWithNoGame, null, updateOnScreensaver = false, visible = true)

        assertNull(result)
    }

    @Test
    fun `resolveAchievementsSystem with toggle off passes through a non-screensaver state`() {
        val result =
            resolveAchievementsSystem(browsingSystem, null, updateOnScreensaver = false, visible = true)

        assertEquals(browsingSystem, result)
    }

    @Test
    fun `resolveAchievementsSystem resolves to null when disconnected regardless of toggle or visibility`() {
        assertNull(resolveAchievementsSystem(null, browsingSystem, updateOnScreensaver = true, visible = true))
        assertNull(resolveAchievementsSystem(null, browsingSystem, updateOnScreensaver = false, visible = true))
        assertNull(resolveAchievementsSystem(null, browsingSystem, updateOnScreensaver = false, visible = false))
    }
}

package com.esde.companion.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameLaunchResolutionTest {
    @Test
    fun `falls back to the system default when no per-game override matches`() {
        val result =
            resolveGameLaunchPackage(
                systemShortName = "n64",
                romPath = "/roms/n64/Game.z64",
                systemDefaults = mapOf("n64" to "com.example.default"),
                gameOverrides = emptyList(),
            )

        assertEquals("com.example.default", result)
    }

    @Test
    fun `a per-game override with a package wins over the system default`() {
        val gameOverrides = listOf(GameLaunchOverride("n64", "./Game.z64", "com.example.override"))
        val result =
            resolveGameLaunchPackage(
                systemShortName = "n64",
                romPath = "/roms/n64/Game.z64",
                systemDefaults = mapOf("n64" to "com.example.default"),
                gameOverrides = gameOverrides,
            )

        assertEquals("com.example.override", result)
    }

    @Test
    fun `an explicit null per-game override suppresses a configured system default`() {
        val result =
            resolveGameLaunchPackage(
                systemShortName = "n64",
                romPath = "/roms/n64/Game.z64",
                systemDefaults = mapOf("n64" to "com.example.default"),
                gameOverrides = listOf(GameLaunchOverride("n64", "./Game.z64", packageName = null)),
            )

        assertNull(result)
    }

    @Test
    fun `returns null when neither a default nor an override is configured`() {
        val result =
            resolveGameLaunchPackage(
                systemShortName = "n64",
                romPath = "/roms/n64/Game.z64",
                systemDefaults = emptyMap(),
                gameOverrides = emptyList(),
            )

        assertNull(result)
    }

    @Test
    fun `matches a per-game override for a game in a subfolder via the suffix rule`() {
        val gameOverrides =
            listOf(
                GameLaunchOverride("psx", "./disc1/Game.chd", "com.example.disc1"),
                GameLaunchOverride("psx", "./disc2/Game.chd", "com.example.disc2"),
            )
        val result =
            resolveGameLaunchPackage(
                systemShortName = "psx",
                romPath = "/roms/psx/disc2/Game.chd",
                systemDefaults = emptyMap(),
                gameOverrides = gameOverrides,
            )

        assertEquals("com.example.disc2", result)
    }

    @Test
    fun `does not match an override for a different system with the same relative path`() {
        val result =
            resolveGameLaunchPackage(
                systemShortName = "n64",
                romPath = "/roms/n64/Game.z64",
                systemDefaults = emptyMap(),
                gameOverrides = listOf(GameLaunchOverride("psx", "./Game.z64", "com.example.wrong")),
            )

        assertNull(result)
    }

    @Test
    fun `does not match an override for a different system even with a configured default for the current system`() {
        val result =
            resolveGameLaunchPackage(
                systemShortName = "n64",
                romPath = "/roms/n64/Game.z64",
                systemDefaults = mapOf("n64" to "com.example.default"),
                gameOverrides = listOf(GameLaunchOverride("psx", "./Game.z64", packageName = null)),
            )

        assertEquals("com.example.default", result)
    }
}

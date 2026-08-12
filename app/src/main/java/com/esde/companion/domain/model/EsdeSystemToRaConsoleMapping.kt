package com.esde.companion.domain.model

/**
 * Maps ES-DE's `systemShortName` (verified against the real Android `es_systems.xml`, see
 * `docs/reference/` - fetched 2026-08-12, re-check there if ES-DE ever renames a system) to
 * the [RetroAchievementsConsole] it corresponds to. A `null` result means either the system
 * has no RetroAchievements support at all, or it does but hasn't been added to this table
 * yet - both cases are [RetroAchievementsGameMatch.UnsupportedSystem] to callers, since this
 * table can't distinguish "genuinely unsupported" from "not yet mapped" on its own.
 *
 * Several ES-DE regional variants of the same physical console (e.g. `megadrive` and
 * `megadrivejp`) map to the same [RetroAchievementsConsole] - RetroAchievements does not
 * distinguish regions at the console level, only at the game level.
 */
object EsdeSystemToRaConsoleMapping {
    private val mapping: Map<String, RetroAchievementsConsole> =
        mapOf(
            "genesis" to RetroAchievementsConsole.MegaDrive,
            "megadrive" to RetroAchievementsConsole.MegaDrive,
            "megadrivejp" to RetroAchievementsConsole.MegaDrive,
            "n64" to RetroAchievementsConsole.Nintendo64,
            "snes" to RetroAchievementsConsole.Snes,
            "sfc" to RetroAchievementsConsole.Snes,
            "snesna" to RetroAchievementsConsole.Snes,
            "gb" to RetroAchievementsConsole.GameBoy,
            "gba" to RetroAchievementsConsole.GameBoyAdvance,
            "gbc" to RetroAchievementsConsole.GameBoyColor,
            "nes" to RetroAchievementsConsole.Nes,
            "famicom" to RetroAchievementsConsole.Nes,
            "pcengine" to RetroAchievementsConsole.PcEngine,
            "tg16" to RetroAchievementsConsole.PcEngine,
            "segacd" to RetroAchievementsConsole.SegaCd,
            "megacd" to RetroAchievementsConsole.SegaCd,
            "megacdjp" to RetroAchievementsConsole.SegaCd,
            "sega32x" to RetroAchievementsConsole.Sega32X,
            "sega32xjp" to RetroAchievementsConsole.Sega32X,
            "sega32xna" to RetroAchievementsConsole.Sega32X,
            "mastersystem" to RetroAchievementsConsole.MasterSystem,
            "mark3" to RetroAchievementsConsole.MasterSystem,
            "gamegear" to RetroAchievementsConsole.GameGear,
            "psx" to RetroAchievementsConsole.PlayStation,
            "atarilynx" to RetroAchievementsConsole.AtariLynx,
            "ngp" to RetroAchievementsConsole.NeoGeoPocket,
            "ngpc" to RetroAchievementsConsole.NeoGeoPocket,
        )

    fun consoleFor(systemShortName: String): RetroAchievementsConsole? = mapping[systemShortName]
}

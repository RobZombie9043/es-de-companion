package com.esde.companion.domain.model

/**
 * Maps ES-DE's `systemShortName` (verified against the real Android `es_systems.xml`, see
 * `docs/reference/` - fetched 2026-08-12, re-check there if ES-DE ever renames a system) to
 * the [RetroAchievementsConsole] it corresponds to. A `null` result means either the system
 * has no RetroAchievements support at all, or it does but hasn't been added to this table
 * yet - both cases are [RetroAchievementsGameMatch.UnsupportedSystem] to callers, since this
 * table can't distinguish "genuinely unsupported" from "not yet mapped" on its own.
 *
 * Several ES-DE regional/hardware variants of the same physical console (e.g. `megadrive`
 * and `megadrivejp`, or `amiga`/`amiga1200`/`amigacd32`) map to the same
 * [RetroAchievementsConsole] - RetroAchievements does not distinguish regions or
 * sub-hardware revisions at the console level, only at the game level.
 *
 * The same "same console" reasoning extends to ROM-hack collections and hardware
 * peripherals/enhancements: `rcheevos`' `rc_consoles.h` (see [RetroAchievementsConsole]'s doc
 * comment) has no separate console ID for hacks, MSU-1-enhanced ROMs, or peripherals like the
 * SNES's Satellaview/Sufami Turbo or the N64DD - a hack is tracked under its base console's
 * normal ID exactly like a retail game (`snesh` is still console 3, same as `snes`), so
 * `snesh`/`gbh`/`gbch`/`gbah`/`genh`/`nesh`/`msu-md`/`snes-msu1`/`satellaview`/`sufami`/`n64dd`
 * all map to their base console below. Note that the six `*h` hack short names and the two
 * `msu-*`/`*-msu1` short names are **not** in ES-DE's stock `es_systems.xml` - they come from
 * the community "custom_systems" convention for ES-DE Android (see
 * github.com/GlazedBelmont/es-de-android-custom-systems), so don't "fix" them as typos by
 * cross-checking only against the stock reference file.
 *
 * Deliberately excludes ES-DE's various MAME/arcade-board systems (`arcade`, `mame`,
 * `fba`/`fbneo`, `cps`/`cps1`/`cps2`/`cps3`, `neogeo`, `model2`/`model3`, `naomi`/`naomi2`,
 * `atomiswave`, `stv`, `mugen`, `openbor`, `type-x`) even though RetroAchievements does
 * track an Arcade console - ES-DE's display names for these come from MAME ROM-set
 * metadata (already inconsistent set to set), and RA's Arcade achievements are matched by
 * exact ROM hash, not title, so title-matching against that console would mostly produce
 * noise rather than useful automatic matches. Also excludes systems RA doesn't
 * realistically support (PS3/PS4/PSVita/Xbox family/Switch/Wii U) and app/launcher
 * pseudo-systems (`android`, `windows`, `steam`, `ports`, etc.) - neither is "not yet
 * mapped" in the sense the class doc above means, just genuinely out of scope.
 */
object EsdeSystemToRaConsoleMapping {
    private val mapping: Map<String, RetroAchievementsConsole> =
        mapOf(
            "genesis" to RetroAchievementsConsole.MegaDrive,
            "megadrive" to RetroAchievementsConsole.MegaDrive,
            "megadrivejp" to RetroAchievementsConsole.MegaDrive,
            "genh" to RetroAchievementsConsole.MegaDrive,
            "msu-md" to RetroAchievementsConsole.MegaDrive,
            "n64" to RetroAchievementsConsole.Nintendo64,
            "n64dd" to RetroAchievementsConsole.Nintendo64,
            "snes" to RetroAchievementsConsole.Snes,
            "sfc" to RetroAchievementsConsole.Snes,
            "snesna" to RetroAchievementsConsole.Snes,
            "snesh" to RetroAchievementsConsole.Snes,
            "snes-msu1" to RetroAchievementsConsole.Snes,
            "satellaview" to RetroAchievementsConsole.Snes,
            "sufami" to RetroAchievementsConsole.Snes,
            "gb" to RetroAchievementsConsole.GameBoy,
            "sgb" to RetroAchievementsConsole.GameBoy,
            "gbh" to RetroAchievementsConsole.GameBoy,
            "gba" to RetroAchievementsConsole.GameBoyAdvance,
            "gbah" to RetroAchievementsConsole.GameBoyAdvance,
            "gbc" to RetroAchievementsConsole.GameBoyColor,
            "gbch" to RetroAchievementsConsole.GameBoyColor,
            "nes" to RetroAchievementsConsole.Nes,
            "famicom" to RetroAchievementsConsole.Nes,
            "nesh" to RetroAchievementsConsole.Nes,
            "fds" to RetroAchievementsConsole.FamicomDiskSystem,
            "pcengine" to RetroAchievementsConsole.PcEngine,
            "tg16" to RetroAchievementsConsole.PcEngine,
            "supergrafx" to RetroAchievementsConsole.PcEngine,
            "pcenginecd" to RetroAchievementsConsole.PcEngineCd,
            "tg-cd" to RetroAchievementsConsole.PcEngineCd,
            "pcfx" to RetroAchievementsConsole.PcFx,
            "segacd" to RetroAchievementsConsole.SegaCd,
            "megacd" to RetroAchievementsConsole.SegaCd,
            "megacdjp" to RetroAchievementsConsole.SegaCd,
            "sega32x" to RetroAchievementsConsole.Sega32X,
            "sega32xjp" to RetroAchievementsConsole.Sega32X,
            "sega32xna" to RetroAchievementsConsole.Sega32X,
            "mastersystem" to RetroAchievementsConsole.MasterSystem,
            "mark3" to RetroAchievementsConsole.MasterSystem,
            "gamegear" to RetroAchievementsConsole.GameGear,
            "sg-1000" to RetroAchievementsConsole.Sg1000,
            "psx" to RetroAchievementsConsole.PlayStation,
            "ps2" to RetroAchievementsConsole.PlayStation2,
            "psp" to RetroAchievementsConsole.Psp,
            "gc" to RetroAchievementsConsole.GameCube,
            "wii" to RetroAchievementsConsole.Wii,
            "nds" to RetroAchievementsConsole.NintendoDs,
            "n3ds" to RetroAchievementsConsole.Nintendo3ds,
            "atarilynx" to RetroAchievementsConsole.AtariLynx,
            "atari2600" to RetroAchievementsConsole.Atari2600,
            "atari5200" to RetroAchievementsConsole.Atari5200,
            "atari7800" to RetroAchievementsConsole.Atari7800,
            "atarijaguar" to RetroAchievementsConsole.AtariJaguar,
            "atarijaguarcd" to RetroAchievementsConsole.AtariJaguarCd,
            "atarist" to RetroAchievementsConsole.AtariSt,
            "ngp" to RetroAchievementsConsole.NeoGeoPocket,
            "ngpc" to RetroAchievementsConsole.NeoGeoPocket,
            "neogeocd" to RetroAchievementsConsole.NeoGeoCd,
            "neogeocdjp" to RetroAchievementsConsole.NeoGeoCd,
            "3do" to RetroAchievementsConsole.ThreeDo,
            "cdimono1" to RetroAchievementsConsole.Cdi,
            "dreamcast" to RetroAchievementsConsole.Dreamcast,
            "saturn" to RetroAchievementsConsole.Saturn,
            "saturnjp" to RetroAchievementsConsole.Saturn,
            "dos" to RetroAchievementsConsole.MsDos,
            "msx" to RetroAchievementsConsole.Msx,
            "msx1" to RetroAchievementsConsole.Msx,
            "msx2" to RetroAchievementsConsole.Msx,
            "msxturbor" to RetroAchievementsConsole.Msx,
            "c64" to RetroAchievementsConsole.Commodore64,
            "vic20" to RetroAchievementsConsole.Vic20,
            "zx81" to RetroAchievementsConsole.Zx81,
            "zxspectrum" to RetroAchievementsConsole.ZxSpectrum,
            "oric" to RetroAchievementsConsole.Oric,
            "amiga" to RetroAchievementsConsole.Amiga,
            "amiga1200" to RetroAchievementsConsole.Amiga,
            "amiga600" to RetroAchievementsConsole.Amiga,
            "amigacd32" to RetroAchievementsConsole.Amiga,
            "cdtv" to RetroAchievementsConsole.Amiga,
            "amstradcpc" to RetroAchievementsConsole.AmstradCpc,
            "apple2" to RetroAchievementsConsole.AppleII,
            "colecovision" to RetroAchievementsConsole.ColecoVision,
            "intellivision" to RetroAchievementsConsole.Intellivision,
            "vectrex" to RetroAchievementsConsole.Vectrex,
            "odyssey2" to RetroAchievementsConsole.MagnavoxOdyssey2,
            "videopac" to RetroAchievementsConsole.MagnavoxOdyssey2,
            "pokemini" to RetroAchievementsConsole.PokemonMini,
            "virtualboy" to RetroAchievementsConsole.VirtualBoy,
            "wonderswan" to RetroAchievementsConsole.WonderSwan,
            "wonderswancolor" to RetroAchievementsConsole.WonderSwan,
            "channelf" to RetroAchievementsConsole.FairchildChannelF,
            "gameandwatch" to RetroAchievementsConsole.GameAndWatch,
            "ngage" to RetroAchievementsConsole.NokiaNGage,
            "supervision" to RetroAchievementsConsole.Supervision,
            "x1" to RetroAchievementsConsole.SharpX1,
            "x68000" to RetroAchievementsConsole.X68000,
            "pc88" to RetroAchievementsConsole.Pc8800,
            "pc98" to RetroAchievementsConsole.Pc9800,
            "fmtowns" to RetroAchievementsConsole.FmTowns,
            "to8" to RetroAchievementsConsole.ThomsonTo8,
            "tic80" to RetroAchievementsConsole.Tic80,
            "megaduck" to RetroAchievementsConsole.Megaduck,
            "arduboy" to RetroAchievementsConsole.Arduboy,
            "wasm4" to RetroAchievementsConsole.Wasm4,
            "arcadia" to RetroAchievementsConsole.Arcadia2001,
            "scv" to RetroAchievementsConsole.SuperCassetteVision,
        )

    fun consoleFor(systemShortName: String): RetroAchievementsConsole? = mapping[systemShortName]
}

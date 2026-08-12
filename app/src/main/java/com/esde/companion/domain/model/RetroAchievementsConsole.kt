package com.esde.companion.domain.model

/**
 * A RetroAchievements-recognized console, carrying the numeric `consoleId` the Web API's
 * `c`/`i` parameters expect (e.g. `API_GetGameList.php`). This is a deliberate starter set
 * of well-established consoles sourced from RetroAchievements' public console ID table, not
 * an exhaustive one - see [EsdeSystemToRaConsoleMapping]. Growing this table over time is
 * expected, ongoing, low-effort maintenance (it's just a lookup table, no algorithmic
 * complexity attached to it). Before relying on any entry added later, spot-check its
 * `consoleId` against a live `API_GetConsoleIDs.php` response rather than trusting memory
 * or an unofficial source - these particular starter values were not verified against a
 * live API call.
 */
@Suppress("MagicNumber")
enum class RetroAchievementsConsole(val consoleId: Int) {
    MegaDrive(1),
    Nintendo64(2),
    Snes(3),
    GameBoy(4),
    GameBoyAdvance(5),
    GameBoyColor(6),
    Nes(7),
    PcEngine(8),
    SegaCd(9),
    Sega32X(10),
    MasterSystem(11),
    PlayStation(12),
    AtariLynx(13),
    NeoGeoPocket(14),
    GameGear(15),
}

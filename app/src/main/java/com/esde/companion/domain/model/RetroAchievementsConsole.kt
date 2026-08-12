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
enum class RetroAchievementsConsole(val consoleId: Long) {
    MegaDrive(1L),
    Nintendo64(2L),
    Snes(3L),
    GameBoy(4L),
    GameBoyAdvance(5L),
    GameBoyColor(6L),
    Nes(7L),
    PcEngine(8L),
    SegaCd(9L),
    Sega32X(10L),
    MasterSystem(11L),
    PlayStation(12L),
    AtariLynx(13L),
    NeoGeoPocket(14L),
    GameGear(15L),
}

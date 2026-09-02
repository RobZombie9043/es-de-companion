package com.esde.companion.domain.model

import com.esde.companion.domain.parser.GamelistXml.matchesGamelistPath

/**
 * Identifies a game by the same fields GameMediaPathResolver needs. [systemPath] is ES-DE's
 * own reported ROM directory for the system (carried forward from a prior system-select - see
 * AppStateReducer.systemPathFor), null when it hasn't been observed yet this session.
 */
data class GameReference(val systemShortName: String, val romPath: String, val systemPath: String? = null)

/**
 * True when [this] and [other] identify the same game - [GameReference.systemShortName] and
 * [GameReference.romPath] only, deliberately ignoring [GameReference.systemPath]. Use this
 * (never plain `==`/data-class equality) wherever a *stored* [GameReference] is being matched
 * against a *live* one - systemPath is session-transient (null until a `SystemSelect` event
 * has been seen this session) so it can easily differ between the session a game guide/manual
 * reference was saved in and a later session where the same game is played directly (e.g.
 * right after an app restart, before ever browsing that system's game list) - confirmed
 * on-device as every downloaded Game Guide appearing "deleted" (the FAB always fell back to a
 * fresh GameFAQs search, and the auto-open-on-launch feature never triggered) the next time
 * the app started fresh and a game was launched without first browsing its system.
 *
 * [romPath] itself is compared via [matchesGamelistPath] (tried in both directions, since
 * either side could be the absolute one here) rather than exact equality, for the same reason
 * [GameLaunchResolution] already needs it: Settings > Game Guides > Add Guide saves a guide
 * against a game picked from a gamelist.xml scan, which only ever has that game's ES-DE-
 * relative path ("./game.iso"), not the absolute path a live [AppState.PlayingGame] reports -
 * without this, a guide added that way would never be found once the game was actually played.
 */
fun GameReference.identifies(other: GameReference): Boolean {
    return systemShortName == other.systemShortName &&
        (romPath.matchesGamelistPath(other.romPath) || other.romPath.matchesGamelistPath(romPath))
}

/**
 * The game currently relevant to display, if any - BrowsingGame/PlayingGame directly, or
 * a Screensaver's currentGame. Null for Idle, BrowsingSystem, or a Screensaver with no
 * game selected yet.
 */
fun AppState.currentGameReference(): GameReference? =
    when (this) {
        is AppState.BrowsingGame -> GameReference(systemShortName, romPath, systemPath)
        is AppState.PlayingGame -> GameReference(systemShortName, romPath, systemPath)
        is AppState.Screensaver -> currentGame?.let { GameReference(it.systemShortName, it.romPath, it.systemPath) }
        is AppState.Idle, is AppState.BrowsingSystem -> null
    }

/** ES-DE's scraped/display title for [currentGameReference], the right thing to title-match against. */
fun AppState.currentGameName(): String? =
    when (this) {
        is AppState.BrowsingGame -> gameName
        is AppState.PlayingGame -> gameName
        is AppState.Screensaver -> currentGame?.gameName
        is AppState.Idle, is AppState.BrowsingSystem -> null
    }

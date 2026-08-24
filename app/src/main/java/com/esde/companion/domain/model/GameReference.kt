package com.esde.companion.domain.model

/**
 * Identifies a game by the same fields GameMediaPathResolver needs. [systemPath] is ES-DE's
 * own reported ROM directory for the system (carried forward from a prior system-select - see
 * AppStateReducer.systemPathFor), null when it hasn't been observed yet this session.
 */
data class GameReference(val systemShortName: String, val romPath: String, val systemPath: String? = null)

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

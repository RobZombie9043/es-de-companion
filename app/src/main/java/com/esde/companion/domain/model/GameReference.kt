package com.esde.companion.domain.model

/** Identifies a game by the same two fields GameMediaPathResolver needs. */
data class GameReference(val systemShortName: String, val romPath: String)

/**
 * The game currently relevant to display, if any - BrowsingGame/PlayingGame directly, or
 * a Screensaver's currentGame. Null for Idle, BrowsingSystem, or a Screensaver with no
 * game selected yet.
 */
fun AppState.currentGameReference(): GameReference? =
    when (this) {
        is AppState.BrowsingGame -> GameReference(systemShortName, romPath)
        is AppState.PlayingGame -> GameReference(systemShortName, romPath)
        is AppState.Screensaver -> currentGame?.let { GameReference(it.systemShortName, it.romPath) }
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

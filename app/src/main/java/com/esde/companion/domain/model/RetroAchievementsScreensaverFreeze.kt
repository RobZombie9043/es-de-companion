package com.esde.companion.domain.model

/**
 * Resolves the game the RetroAchievements per-game page should show. Behaves exactly like
 * [AppState.currentGameReference]/[AppState.currentGameName] when [updateOnScreensaver] is
 * true - the current default - but when it's false and [appState] is [AppState.Screensaver],
 * holds [previous] instead of switching to the screensaver's own game, so a user browsing
 * achievements isn't redirected to whatever game the screensaver happens to be showing.
 * A `null` [appState] (not connected to ES-DE) always resolves to `null`, matching today's
 * disconnect behavior regardless of the toggle.
 */
fun resolveAchievementsGame(
    appState: AppState?,
    previous: Pair<GameReference, String>?,
    updateOnScreensaver: Boolean,
): Pair<GameReference, String>? =
    when {
        appState == null -> null
        appState is AppState.Screensaver && !updateOnScreensaver -> previous
        else -> appState.toGameReferenceAndName()
    }

private fun AppState.toGameReferenceAndName(): Pair<GameReference, String>? =
    currentGameReference()?.let { reference -> currentGameName()?.let { reference to it } }

/**
 * Same idea as [resolveAchievementsGame], for the RetroAchievements system games browser -
 * holds [previous] rather than dropping to "no system" when a screensaver starts and
 * [updateOnScreensaver] is false.
 */
fun resolveAchievementsSystem(
    appState: AppState?,
    previous: AppState.BrowsingSystem?,
    updateOnScreensaver: Boolean,
): AppState.BrowsingSystem? =
    when {
        appState == null -> null
        appState is AppState.Screensaver && !updateOnScreensaver -> previous
        else -> appState as? AppState.BrowsingSystem
    }

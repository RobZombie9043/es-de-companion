package com.esde.companion.domain.model

/**
 * Resolves the game a per-game screen should show - shared by the RetroAchievements per-game
 * page ([com.esde.companion.ui.retroachievements.RetroAchievementsViewModel]) and the Game
 * Guides Library screen ([com.esde.companion.ui.gameguides.GameGuidesViewModel]), each with
 * its own "Update on Screensaver" toggle. Behaves exactly like
 * [AppState.currentGameReference]/[AppState.currentGameName] when [updateOnScreensaver] is
 * true, but when it's false, [appState] is [AppState.Screensaver], and [visible] is true (the
 * page is actually open on screen right now), holds [previous] instead of switching to the
 * screensaver's own game, so a user actively looking at the page isn't redirected to whatever
 * game the screensaver happens to be showing.
 *
 * The [visible] gate matters because this resolver keeps running in the background for the
 * whole app lifetime, not just while the page is on screen: without it, "hold" would freeze
 * [previous] at whatever game was active before a screensaver started even while nobody's
 * looking, so opening the page later - while the screensaver is still going - would surface
 * that stale held game instead of the one the screensaver is actually showing right now. While
 * [visible] is false this always tracks the live [appState] regardless of the toggle, so
 * [previous] is never stale by the time the page opens - see `ObserveScreensaverAwareContextUseCase`.
 * A `null` [appState] (not connected to ES-DE) always resolves to `null`, matching today's
 * disconnect behavior regardless of the toggle or [visible].
 */
fun resolveScreensaverAwareGame(
    appState: AppState?,
    previous: Pair<GameReference, String>?,
    updateOnScreensaver: Boolean,
    visible: Boolean,
): Pair<GameReference, String>? {
    val liveResolution = appState?.toGameReferenceAndName()
    return when {
        appState == null -> null
        visible && appState is AppState.Screensaver && !updateOnScreensaver -> previous ?: liveResolution
        else -> liveResolution
    }
}

private fun AppState.toGameReferenceAndName(): Pair<GameReference, String>? =
    currentGameReference()?.let { reference -> currentGameName()?.let { reference to it } }

/**
 * Same idea as [resolveScreensaverAwareGame], for the RetroAchievements system games browser -
 * holds [previous] rather than dropping to "no system" when a screensaver starts and
 * [updateOnScreensaver] is false, but (same [visible] reasoning as [resolveScreensaverAwareGame])
 * only while the page is actually open.
 */
fun resolveAchievementsSystem(
    appState: AppState?,
    previous: AppState.BrowsingSystem?,
    updateOnScreensaver: Boolean,
    visible: Boolean,
): AppState.BrowsingSystem? =
    when {
        appState == null -> null
        visible && appState is AppState.Screensaver && !updateOnScreensaver -> previous
        else -> appState as? AppState.BrowsingSystem
    }

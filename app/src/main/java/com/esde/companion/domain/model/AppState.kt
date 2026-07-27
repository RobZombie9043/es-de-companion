package com.esde.companion.domain.model

/**
 * The app's current understanding of what ES-DE is doing right now, derived entirely
 * from the sequence of [EsdeEvent]s parsed from es_log.txt.
 *
 * This is the single source of truth everything else in the app (media lookup, widgets,
 * whatever comes later) is built on top of. See CLAUDE.md: this pipeline is expected to
 * be the most rigorously tested part of the codebase, and this type should stay a plain,
 * closed set of variants that a `when` block can exhaustively handle.
 */
sealed class AppState {

    /** No events observed yet, or ES-DE's current activity is not known. */
    data object Idle : AppState()

    data class BrowsingSystem(
        val systemShortName: String,
        val systemFullName: String,
        val systemPath: String,
    ) : AppState()

    data class BrowsingGame(
        val romPath: String,
        val gameName: String,
        val systemShortName: String,
        val systemFullName: String,
    ) : AppState()

    data class PlayingGame(
        val romPath: String,
        val gameName: String,
        val systemShortName: String,
        val systemFullName: String,
    ) : AppState()

    /**
     * [previousState] is whatever [AppState] was active immediately before the
     * screensaver started - ES-DE does not reliably fire a fresh system-select/game-select
     * on screensaver-end, so the reducer restores this directly rather than falling back
     * to [Idle]. Deliberately excludes [Screensaver] itself (see [AppStateReducer]), so
     * this never nests.
     */
    data class Screensaver(
        val mode: String,
        val currentGame: ScreensaverGame?,
        val previousState: AppState,
    ) : AppState()
}

data class ScreensaverGame(
    val romPath: String,
    val gameName: String,
    val systemShortName: String,
    val systemFullName: String,
)
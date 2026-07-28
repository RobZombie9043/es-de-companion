package com.esde.companion.ui.main

/**
 * What backdrop image and overlay logo the main screen should show right now, derived
 * from AppState plus resolved media. Kept in the ui layer, not domain - same reasoning
 * as AppStateDisplay: this is presentation, and is expected to change independently of
 * the state model itself.
 */
sealed class MainScreenImageState {

    /** Idle, or the log file isn't currently readable - nothing to show. */
    data object None : MainScreenImageState()

    data class SystemBackdrop(
        val fanartPath: String?,
        val systemLogoAssetPath: String,
    ) : MainScreenImageState()

    data class GameBackdrop(
        val backdropPath: String?,
        val logoPath: String?,
    ) : MainScreenImageState()
}
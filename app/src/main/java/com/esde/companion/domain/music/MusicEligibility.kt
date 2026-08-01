package com.esde.companion.domain.music

import com.esde.companion.domain.model.AppState
import com.esde.companion.domain.model.MusicPool

data class MusicSettings(
    val enabled: Boolean,
    val systems: Boolean,
    val games: Boolean,
    val screensaver: Boolean,
)

sealed class MusicEligibility {
    data object Ineligible : MusicEligibility()

    data class Eligible(val pool: MusicPool) : MusicEligibility()
}

/**
 * Whether background music should be playing right now, and which pool it should come
 * from, based purely on ES-DE's current [AppState] and the user's toggles.
 *
 * [AppState.Screensaver] deliberately ignores [AppState.Screensaver.currentGame] and
 * [AppState.Screensaver.previousState] even though the latter genuinely carries whatever
 * was active before the screensaver started - the screensaver always uses the general
 * pool regardless of ES-DE's screensaver-tied game/system.
 */
object MusicEligibilityResolver {

    fun resolve(appState: AppState, settings: MusicSettings): MusicEligibility {
        if (!settings.enabled) return MusicEligibility.Ineligible

        return when (appState) {
            is AppState.Idle -> MusicEligibility.Ineligible

            is AppState.BrowsingSystem ->
                if (settings.systems) {
                    MusicEligibility.Eligible(MusicPool.PerSystem(appState.systemShortName))
                } else {
                    MusicEligibility.Ineligible
                }

            is AppState.BrowsingGame ->
                if (settings.games) {
                    MusicEligibility.Eligible(MusicPool.PerSystem(appState.systemShortName))
                } else {
                    MusicEligibility.Ineligible
                }

            is AppState.PlayingGame -> MusicEligibility.Ineligible

            is AppState.Screensaver ->
                if (settings.screensaver) {
                    MusicEligibility.Eligible(MusicPool.General)
                } else {
                    MusicEligibility.Ineligible
                }
        }
    }
}

package com.esde.companion.domain.model

/**
 * Direction of the controller navigation press that triggered a system/game selection -
 * see NavigationDirectionTracker for how this is derived from the raw log, and
 * AnimatedLogoImage for how the widget UI uses it (slides the incoming logo in from the
 * opposite side of travel).
 */
enum class NavigationDirection {
    Up,
    Down,
    Left,
    Right,
}

/**
 * The direction of the navigation press that produced the current state, if known - null
 * for Idle/PlayingGame/Screensaver, or for a BrowsingSystem/BrowsingGame reached by
 * anything other than a recognized directional press (e.g. a back/select button, or no
 * preceding controller input at all).
 */
fun AppState.navigationDirection(): NavigationDirection? = when (this) {
    is AppState.BrowsingSystem -> navigationDirection
    is AppState.BrowsingGame -> navigationDirection
    is AppState.Idle, is AppState.PlayingGame, is AppState.Screensaver -> null
}

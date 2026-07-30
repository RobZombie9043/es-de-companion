package com.esde.companion.domain.model

/**
 * Which widget canvas is relevant for a given AppState. Only two canvases exist -
 * System browsing and Playing (which also covers BrowsingGame and Screensaver, visually
 * closest to "a game is in view"). Idle intentionally has no StateGroup - see
 * AppState.stateGroup() - no widgets are shown for it.
 */
enum class StateGroup {
    System,
    Playing,
}

/**
 * Maps the live AppState to the widget canvas that should be shown for it, or null for
 * Idle. The widget overlay simply doesn't render when this is null.
 */
fun AppState.stateGroup(): StateGroup? = when (this) {
    is AppState.Idle -> null
    is AppState.BrowsingSystem -> StateGroup.System
    is AppState.BrowsingGame, is AppState.PlayingGame -> StateGroup.Playing
    is AppState.Screensaver -> StateGroup.Playing
}
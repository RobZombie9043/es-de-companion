package com.esde.companion.ui.main

import com.esde.companion.domain.model.AppState

/**
 * Turns an [AppState] into human-readable text for this initial text-only display.
 * Deliberately kept in the ui layer, not domain - how state is presented is a UI
 * concern and is expected to change often as real screens are built; the state
 * model itself should not need to change alongside it.
 */
fun AppState.toDisplayText(): String = when (this) {
    is AppState.Idle ->
        "Waiting for ES-DE activity…"

    is AppState.BrowsingSystem ->
        "Browsing system\n$systemFullName"

    is AppState.BrowsingGame ->
        "Browsing game\n$gameName\n($systemFullName)"

    is AppState.PlayingGame ->
        "Playing\n$gameName\n($systemFullName)"

    is AppState.Screensaver -> {
        val gameLine = currentGame?.let { "\n\nShowing: ${it.gameName} (${it.systemFullName})" } ?: ""
        "Screensaver active ($mode)$gameLine"
    }
}

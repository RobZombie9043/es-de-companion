package com.esde.companion.domain.model

/**
 * How the main screen should react while a particular AppState is active - see
 * Settings > UI Settings (Game Playing Behavior / Screensaver Behavior). Nothing leaves
 * the screen as-is. Dim overlays a translucent black scrim that lets touches pass
 * through. Black shows the same opaque full-black cover as the manual double-tap
 * gesture, blocking all touches except double-tap-to-restore.
 */
enum class ScreenBehavior {
    Nothing,
    Dim,
    Black,
}
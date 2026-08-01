package com.esde.companion.domain.model

/**
 * How the main screen should react while a particular AppState is active - see
 * Settings > UI Settings (Game Playing Behavior / Screensaver Behavior). Nothing leaves
 * the screen as-is. Dim overlays a translucent black scrim that lets touches pass
 * through. Black shows the same opaque full-black cover as the manual double-tap
 * gesture, blocking all touches except double-tap-to-restore. GameManual replaces the
 * screen with the game's manual PDF (see GameManualScreen) - only meaningful for Game
 * Playing Behavior, not Screensaver Behavior (ScreenBehaviorPicker's `options` param
 * excludes it from the Screensaver picker).
 */
enum class ScreenBehavior {
    Nothing,
    Dim,
    Black,
    GameManual,
}
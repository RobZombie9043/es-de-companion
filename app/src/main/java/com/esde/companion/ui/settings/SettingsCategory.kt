package com.esde.companion.ui.settings

/**
 * Top-level settings categories shown as a list on the long-press menu's home page - see
 * LongPressSettingsMenu. Selecting one shows its subpage in place of the list. Deliberately
 * a plain enum driving boolean-ish `selectedCategory` state rather than nav-compose
 * destinations, matching how the long-press popup itself is shown/hidden from MainActivity:
 * an extra NavHost graph here would hit the same predictive-back crash (b/384186542) that
 * keeps SETTINGS out of the main graph.
 */
enum class SettingsCategory(val title: String, val description: String) {
    Widgets(
        title = "Widgets",
        description = "Add, move, and resize widgets on the main screen",
    ),
    UI(
        title = "UI Settings",
        description = "Appearance and screen behavior",
    ),
    AppDrawer(
        title = "App Drawer and Dock",
        description = "Visible apps, grid columns, dock",
    ),
    VideoPlayback(
        title = "Video Playback",
        description = "Game video playback while browsing",
    ),
    Sound(
        title = "Background Music",
        description = "Background music and video-playback ducking",
    ),
    Other(
        title = "Other Settings",
        description = "Miscellaneous companion app behavior",
    ),
    Setup(
        title = "Setup",
        description = "ES-DE and media folder locations",
    ),
}
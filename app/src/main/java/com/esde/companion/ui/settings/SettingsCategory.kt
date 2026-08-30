package com.esde.companion.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Top-level settings categories shown as a list on the long-press menu's home page - see
 * LongPressSettingsMenu. Selecting one shows its subpage in place of the list. Deliberately
 * a plain enum driving boolean-ish `selectedCategory` state rather than nav-compose
 * destinations, matching how the long-press popup itself is shown/hidden from MainActivity:
 * an extra NavHost graph here would hit the same predictive-back crash (b/384186542) that
 * keeps SETTINGS out of the main graph.
 */
enum class SettingsCategory(val title: String, val description: String, val icon: ImageVector) {
    Widgets(
        title = "Edit Widgets",
        description = "Add, move, and resize widgets on the main screen",
        icon = Icons.Filled.Widgets,
    ),
    UI(
        title = "UI Settings",
        description = "Appearance and screen behavior",
        icon = Icons.Filled.Palette,
    ),
    AppDrawer(
        title = "App Drawer and Dock",
        description = "Visible apps, grid columns, dock",
        icon = Icons.Filled.Apps,
    ),
    Sound(
        title = "Background Music",
        description = "Background music and video-playback ducking",
        icon = Icons.Filled.MusicNote,
    ),
    Other(
        title = "Other Settings",
        description = "Miscellaneous companion app behavior",
        icon = Icons.Filled.Settings,
    ),
    Setup(
        title = "Setup",
        description = "ES-DE and media folder locations",
        icon = Icons.Filled.Folder,
    ),
    RetroAchievements(
        title = "RetroAchievements",
        description = "Sign in and manage the RetroAchievements connection",
        icon = Icons.Filled.EmojiEvents,
    ),
    GameGuides(
        title = "Game Guides",
        description = "Downloaded guide storage",
        icon = Icons.Filled.LibraryBooks,
    ),
    Thor(
        title = "Thor Settings",
        description = "Miscellaneous Thor utilities",
        icon = Icons.Filled.Devices,
    ),
}

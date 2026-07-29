package com.esde.companion.ui.settings

/**
 * Top-level settings categories shown as a list when Settings is first opened. Selecting
 * one shows its subpage in place of the list - see SettingsScreen. Deliberately a plain
 * enum driving boolean-ish `selectedCategory` state rather than nav-compose destinations,
 * matching how the Settings screen itself is shown/hidden from MainActivity: an extra
 * NavHost graph here would hit the same predictive-back crash (b/384186542) that keeps
 * SETTINGS out of the main graph.
 */
enum class SettingsCategory(val title: String, val description: String) {
    Setup(
        title = "Setup",
        description = "ES-DE and media folder locations",
    ),
    Other(
        title = "Other Settings",
        description = "Debug overlay and misc options",
    ),
}

package com.esde.companion.domain.model

/**
 * What a corner FAB slot (see [FabPosition]) does when tapped. [CustomApp] additionally
 * carries which app via [FabSlot.customAppPackageName] - the type alone isn't enough to
 * know what to launch. [RetroAchievements] is contextually aware of what ES-DE is currently
 * showing rather than being split into separate "current game" and "current system" FAB
 * types - see MainActivity's `retroAchievementsFabMode`.
 */
enum class FabType {
    Music,
    Settings,
    GameManual,
    AppDrawer,
    CustomApp,
    RetroAchievements,
    None,
}

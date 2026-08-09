package com.esde.companion.domain.model

/**
 * What a corner FAB slot (see [FabPosition]) does when tapped. [CustomApp] additionally
 * carries which app via [FabSlot.customAppPackageName] - the type alone isn't enough to
 * know what to launch.
 */
enum class FabType {
    Music,
    Settings,
    GameManual,
    AppDrawer,
    CustomApp,
    None,
}

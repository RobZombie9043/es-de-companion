package com.esde.companion.domain.model

/**
 * What a corner FAB slot (see [FabPosition]) does when tapped. [CustomApp] additionally
 * carries which app via [FabSlot.customAppPackageName] - the type alone isn't enough to
 * know what to launch. [Clock], [SystemStatus], and [ClockAndSystemStatus] are display-only
 * (no click behavior) and are offered only in the top corners - see UISettingsContent's
 * TOP_FAB_OPTIONS/BOTTOM_FAB_OPTIONS, same mechanism that already keeps [Music] top-only.
 */
enum class FabType {
    Music,
    Settings,
    GameManual,
    AppDrawer,
    CustomApp,
    Clock,
    SystemStatus,
    ClockAndSystemStatus,
    None,
}

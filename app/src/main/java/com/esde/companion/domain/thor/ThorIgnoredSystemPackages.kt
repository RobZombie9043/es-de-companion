package com.esde.companion.domain.thor

/**
 * System/launcher/OEM-overlay packages that must never be treated as a genuine foreground app
 * by any Thor Settings feature that reacts to or acts on the foreground app - shared by Auto
 * FPS Mode (never a trigger) and Task Killer (never force-stopped, and not exposed in its
 * exclude-apps picker since there's no user-reachable path to un-protect them) - ported from
 * Asgard's `AutoFpsForegroundReactor.IGNORED_PACKAGES`/`ProtectedPackages.SYSTEM_PACKAGES`,
 * which were already identical sets.
 */
val THOR_IGNORED_SYSTEM_PACKAGES: Set<String> =
    setOf(
        // System UI briefly becomes the foreground package when the notification shade is
        // pulled down, which would otherwise trigger a false Auto FPS revert / be force-stoppable.
        "com.android.systemui",
        "com.android.launcher3",
        // The bare system/framework package - covers system dialogs/toasts not attributable to
        // any real app.
        "android",
        // The OEM's game-assistant overlay - fires its own window-state-changed event on the top
        // screen (confirmed live) but never represents a genuine app the user navigates to.
        "com.odin.gameassistant",
    )

package com.esde.companion.domain.model

/**
 * A single tile in the App Drawer grid: either a plain launchable app, or a folder whose
 * members have already been resolved to their live [InstalledApp]s and filtered (hidden/
 * uninstalled members dropped) - see [buildDrawerItems], the only place this is produced.
 */
sealed interface DrawerItem {

    /** Display label, used to sort apps and folders into one alphabetical grid. */
    val label: String

    data class App(val app: InstalledApp) : DrawerItem {
        override val label: String get() = app.label
    }

    /**
     * [apps] is [folder]'s membership resolved against the currently visible installed
     * apps, sorted alphabetically - never persisted, always recomputed by
     * [buildDrawerItems]. Can be empty if every member was hidden/uninstalled; the folder
     * still renders as a tile in that case, just with an empty popup, rather than
     * vanishing - see [buildDrawerItems]'s kdoc for why this shouldn't be confused with
     * the explicit-removal-to-zero case, which does delete the folder.
     */
    data class Folder(val folder: AppFolder, val apps: List<InstalledApp>) : DrawerItem {
        override val label: String get() = folder.name
    }
}

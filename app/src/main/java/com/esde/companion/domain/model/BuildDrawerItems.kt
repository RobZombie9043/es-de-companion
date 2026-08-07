package com.esde.companion.domain.model

/**
 * Merges [installedApps], the hidden-package set, and persisted [folders] into the flat
 * list the App Drawer grid renders. This is the one place folder membership actually gets
 * resolved against live app state - see [AppFolder] and [DrawerItem.Folder]'s kdocs for
 * the storage/display split this relies on:
 *
 * - A hidden or uninstalled app is dropped from both the top-level list and any folder's
 *   [DrawerItem.Folder.apps] - display-time filtering only, [folders] itself is returned
 *   untouched by the caller (no write happens here or because of calling this).
 * - An app that belongs to a folder never also appears as an ungrouped [DrawerItem.App],
 *   even if it's listed in more than one folder's membership (tolerated, not validated -
 *   see [AppFolder]'s kdoc on the single-membership invariant living in the ViewModel).
 * - Folders sort alphabetically by name among themselves, and ungrouped apps sort
 *   alphabetically by label among themselves, matching
 *   [com.esde.companion.data.apps.PackageManagerAppsRepository]'s existing
 *   `sortedBy { it.label.lowercase() }` convention either way. [sortFoldersOnTop] only
 *   decides whether those two already-sorted groups are then concatenated (folders first)
 *   or merged into one fully interleaved alphabetical list - see AppDrawerSettingsRepository.
 */
fun buildDrawerItems(
    installedApps: List<InstalledApp>,
    hiddenPackages: Set<String>,
    folders: List<AppFolder>,
    sortFoldersOnTop: Boolean = true,
): List<DrawerItem> {
    val visibleByPackage = installedApps
        .filterNot { it.packageName in hiddenPackages }
        .associateBy { it.packageName }
    val groupedPackageNames = folders.flatMap { it.memberPackageNames }.toSet()

    val folderItems = folders
        .map { folder ->
            val members = folder.memberPackageNames
                .mapNotNull { visibleByPackage[it] }
                .sortedBy { it.label.lowercase() }
            DrawerItem.Folder(folder = folder, apps = members)
        }
        .sortedBy { it.label.lowercase() }
    val ungroupedItems = visibleByPackage.values
        .filterNot { it.packageName in groupedPackageNames }
        .map(DrawerItem::App)
        .sortedBy { it.label.lowercase() }

    return if (sortFoldersOnTop) {
        folderItems + ungroupedItems
    } else {
        (folderItems + ungroupedItems).sortedBy { it.label.lowercase() }
    }
}

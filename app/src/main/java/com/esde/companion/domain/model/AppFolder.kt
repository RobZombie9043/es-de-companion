package com.esde.companion.domain.model

/**
 * A named group of apps that collapses into a single tile in the App Drawer. [id] is a
 * stable, generated identity (not derived from [name]) so two folders can share a name,
 * same as real launchers allow.
 *
 * [memberPackageNames] is a membership *set*, not an ordered list - display order inside
 * an open folder is always alphabetical-by-label at render time (see [buildDrawerItems]),
 * never persisted, matching how [AppDrawerSettingsRepository.hiddenApps]/
 * [AppDrawerSettingsRepository.otherScreenLaunchApps] use `Set<String>` for the same
 * "membership, not order" reason - contrast with [DockSettingsRepository.dockApps], an
 * actual ordered `List<String>`.
 *
 * Single-folder membership (an app belonging to at most one folder) is an invariant
 * enforced by [com.esde.companion.ui.drawer.AppDrawerViewModel.addAppToFolder], not by
 * this model or [AppFolderRepository] - persisting a package name in two folders' sets is
 * tolerated gracefully by [buildDrawerItems] (the app renders in both), same as
 * [DockSettingsRepository] persisting whatever list it's handed without validating it.
 */
data class AppFolder(
    val id: String,
    val name: String,
    val memberPackageNames: Set<String>,
)

package com.esde.companion.domain.model

/** A launchable app, as shown in the App Drawer. Icon resolution deliberately isn't
 * part of this model - loading a Drawable is a real Android/PackageManager operation
 * with no meaningful domain shape, so it stays entirely in the data/ui layers. */
data class InstalledApp(
    val packageName: String,
    val label: String,
)

/**
 * Reserved, never-real packageName for the "Open App Drawer" dock shortcut - lets it be
 * pinned/persisted/reordered through the same List<String> dock storage as real apps
 * (AppDockViewModel/EditWidgetsViewModel's dockItems) rather than needing a parallel
 * dock-item type. Not a syntactically valid Android package name (contains ':'), so it
 * can never collide with a real installed app.
 */
const val APP_DRAWER_SHORTCUT_PACKAGE_NAME = "esde-companion:open-app-drawer"

/** The synthetic [InstalledApp] entry representing the App Drawer shortcut - see
 * [APP_DRAWER_SHORTCUT_PACKAGE_NAME]. */
val AppDrawerShortcut = InstalledApp(packageName = APP_DRAWER_SHORTCUT_PACKAGE_NAME, label = "App Drawer")
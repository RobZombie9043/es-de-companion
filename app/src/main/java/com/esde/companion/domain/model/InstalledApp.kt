package com.esde.companion.domain.model

/** A launchable app, as shown in the App Drawer. Icon resolution deliberately isn't
 * part of this model - loading a Drawable is a real Android/PackageManager operation
 * with no meaningful domain shape, so it stays entirely in the data/ui layers. */
data class InstalledApp(
    val packageName: String,
    val label: String,
)
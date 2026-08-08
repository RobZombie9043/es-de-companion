package com.esde.companion.domain.repository

import com.esde.companion.domain.model.InstalledApp
import kotlinx.coroutines.flow.Flow

/**
 * Source of truth for launchable apps on the device, as shown in the App Drawer.
 *
 * A Flow, not a one-shot suspend call, so the drawer reflects installs/uninstalls while
 * it's open without requiring the app to restart - see [PackageManagerAppsRepository]
 * for how that's kept live.
 */
interface InstalledAppsRepository {
    fun observeInstalledApps(): Flow<List<InstalledApp>>
}

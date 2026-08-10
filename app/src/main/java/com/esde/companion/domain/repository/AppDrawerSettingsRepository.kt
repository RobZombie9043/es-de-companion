package com.esde.companion.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Persisted App Drawer display/visibility preferences: which installed apps are hidden
 * from the drawer grid, its column count, and per-app launch location. Background opacity
 * is not here - it's the shared master Overlay Opacity setting, see
 * [OnboardingRepository.observeOverlayOpacityPercent]. Kept separate from
 * [OnboardingRepository] - these are App Drawer-specific display settings, not
 * onboarding/folder-location state, and grouping them here keeps each repository's scope
 * legible as the settings surface grows.
 */
interface AppDrawerSettingsRepository {
    /** Package names hidden from the App Drawer grid. Defaults to empty (nothing hidden). */
    suspend fun setHiddenApps(packageNames: Set<String>)

    fun observeHiddenApps(): Flow<Set<String>>

    /** Number of columns in the App Drawer grid, 3-6. Defaults to 4. */
    suspend fun setGridColumns(columns: Int)

    fun observeGridColumns(): Flow<Int>

    /**
     * Package names whose last-used launch location is the *other* connected display
     * rather than this screen. Absence from this set means "this screen" - that's the
     * default for every app until it's explicitly launched elsewhere, so nothing needs
     * seeding when a new app is installed. Same shape as [setHiddenApps]/[observeHiddenApps]
     * deliberately: this is a two-state per-app flag, not worth a full map encoding.
     */
    suspend fun setOtherScreenLaunchApps(packageNames: Set<String>)

    fun observeOtherScreenLaunchApps(): Flow<Set<String>>

    /** Whether folders are grouped ahead of ungrouped apps in the drawer grid, rather
     * than interleaved alphabetically with them - see buildDrawerItems. Defaults to true. */
    suspend fun setSortFoldersOnTop(sortOnTop: Boolean)

    fun observeSortFoldersOnTop(): Flow<Boolean>

    /** Whether the App Drawer shows its header row (search bar plus Android/companion
     * settings shortcuts) above the grid. Defaults to true. */
    suspend fun setShowSearchBar(show: Boolean)

    fun observeShowSearchBar(): Flow<Boolean>
}

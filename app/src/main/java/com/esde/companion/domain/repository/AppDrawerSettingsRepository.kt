package com.esde.companion.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Persisted App Drawer display/visibility preferences: which installed apps are hidden
 * from the drawer grid, the grid's background opacity, and its column count. Kept
 * separate from [OnboardingRepository] - these are App Drawer-specific display settings,
 * not onboarding/folder-location state, and grouping them here keeps each repository's
 * scope legible as the settings surface grows.
 */
interface AppDrawerSettingsRepository {

    /** Package names hidden from the App Drawer grid. Defaults to empty (nothing hidden). */
    suspend fun setHiddenApps(packageNames: Set<String>)
    fun observeHiddenApps(): Flow<Set<String>>

    /**
     * Background opacity of the App Drawer, 0-100, standard convention: 0 = fully
     * transparent, 100 = fully opaque. Defaults to 30.
     */
    suspend fun setDrawerOpacityPercent(percent: Int)
    fun observeDrawerOpacityPercent(): Flow<Int>

    /** Number of columns in the App Drawer grid, 3-6. Defaults to 4. */
    suspend fun setGridColumns(columns: Int)
    fun observeGridColumns(): Flow<Int>
}
package com.esde.companion.domain.repository

import com.esde.companion.domain.model.DockSize
import kotlinx.coroutines.flow.Flow

/**
 * Persisted App Dock settings: whether it's shown, how many apps it holds, its icon
 * size, its background opacity, and the ordered list of pinned apps. Kept separate from
 * [AppDrawerSettingsRepository] - the dock is a distinct surface with its own settings,
 * though it deliberately reuses that repository's `otherScreenLaunchApps` for launch
 * location so an app's this-screen/other-screen preference is shared between the Drawer
 * and the Dock rather than tracked twice.
 */
interface DockSettingsRepository {

    /** Whether the dock is shown on the main screen. Defaults to false. */
    suspend fun setDockEnabled(enabled: Boolean)
    fun observeDockEnabled(): Flow<Boolean>

    /** Number of slots in the dock, 2-5. Defaults to 5. */
    suspend fun setDockMaxApps(maxApps: Int)
    fun observeDockMaxApps(): Flow<Int>

    /** Icon/bar size. Defaults to [DockSize.Medium]. */
    suspend fun setDockSize(size: DockSize)
    fun observeDockSize(): Flow<DockSize>

    /**
     * Background opacity of the dock bar, 0-100, standard convention: 0 = fully
     * transparent, 100 = fully opaque. Defaults to 60.
     */
    suspend fun setDockOpacityPercent(percent: Int)
    fun observeDockOpacityPercent(): Flow<Int>

    /**
     * Package names pinned to the dock, in display order. Persists whatever list it's
     * given, independent of the current [setDockMaxApps] value - lowering the max
     * doesn't delete anything from storage, it just means fewer of these are shown.
     * Defaults to empty.
     */
    suspend fun setDockApps(packageNames: List<String>)
    fun observeDockApps(): Flow<List<String>>
}

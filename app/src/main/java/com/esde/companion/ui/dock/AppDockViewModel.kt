package com.esde.companion.ui.dock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esde.companion.domain.model.DockSize
import com.esde.companion.domain.model.InstalledApp
import com.esde.companion.domain.model.LaunchLocation
import com.esde.companion.domain.usecase.ObserveDockAppsUseCase
import com.esde.companion.domain.usecase.ObserveDockEnabledUseCase
import com.esde.companion.domain.usecase.ObserveDockMaxAppsUseCase
import com.esde.companion.domain.usecase.ObserveDockSizeUseCase
import com.esde.companion.domain.usecase.ObserveInstalledAppsUseCase
import com.esde.companion.domain.usecase.ObserveOtherScreenLaunchAppsUseCase
import com.esde.companion.domain.usecase.ObserveOverlayOpacityUseCase
import com.esde.companion.domain.usecase.SetDockAppsUseCase
import com.esde.companion.domain.usecase.SetOtherScreenLaunchAppsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Drives the live App Dock shown on the main screen. Item management (add/remove/
 * reorder) happens entirely through gestures on the live dock (long-press an item or an
 * empty slot - see AppDock), not a separate Settings screen, so this ViewModel owns both
 * the display state and the editing operations.
 *
 * Deliberately duplicates [recordLaunchLocation] rather than sharing a use case with
 * [com.esde.companion.ui.drawer.AppDrawerViewModel] - it's four lines used in exactly
 * two places, and CLAUDE.md favors duplication over a premature shared abstraction at
 * this size. The underlying preference set itself *is* shared (same
 * ObserveOtherScreenLaunchAppsUseCase/SetOtherScreenLaunchAppsUseCase as the Drawer), so
 * an app's this-screen/other-screen preference and indicator dot are identical on both
 * surfaces.
 */
class AppDockViewModel(
    observeDockEnabled: ObserveDockEnabledUseCase,
    private val observeDockApps: ObserveDockAppsUseCase,
    private val setDockApps: SetDockAppsUseCase,
    observeDockMaxApps: ObserveDockMaxAppsUseCase,
    observeDockSize: ObserveDockSizeUseCase,
    observeOverlayOpacity: ObserveOverlayOpacityUseCase,
    private val observeInstalledApps: ObserveInstalledAppsUseCase,
    private val observeOtherScreenLaunchApps: ObserveOtherScreenLaunchAppsUseCase,
    private val setOtherScreenLaunchApps: SetOtherScreenLaunchAppsUseCase,
) : ViewModel() {

    val dockEnabled: StateFlow<Boolean> = observeDockEnabled()
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000), initialValue = false)

    val maxApps: StateFlow<Int> = observeDockMaxApps()
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000), initialValue = 5)

    val dockSize: StateFlow<DockSize> = observeDockSize()
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000), initialValue = DockSize.Medium)

    val dockOpacityPercent: StateFlow<Int> = observeOverlayOpacity()
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000), initialValue = 80)

    /** The pinned apps actually shown, in order, resolved to their current
     * label/packageName and capped to [maxApps] - lowering the max never deletes
     * anything from storage, it just shows fewer slots (see DockSettingsRepository). An
     * entry for an app that's since been uninstalled is silently skipped rather than
     * shown as a broken row; it reappears in its slot if the app is reinstalled, since
     * nothing was removed from storage. */
    val dockItems: StateFlow<List<InstalledApp>> =
        combine(observeDockApps(), observeInstalledApps(), observeDockMaxApps()) { pinned, installed, max ->
            val byPackageName = installed.associateBy { it.packageName }
            pinned.take(max).mapNotNull { byPackageName[it] }
        }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000), initialValue = emptyList())

    /** Packages whose last-used launch location is the other screen - same set the App
     * Drawer reads/writes, see the class kdoc. */
    val otherScreenLaunchApps: StateFlow<Set<String>> = observeOtherScreenLaunchApps()
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000), initialValue = emptySet())

    /** Installed apps not already pinned - the candidate list for the "add app to
     * dock" picker opened by long-pressing an empty slot. */
    val availableApps: StateFlow<List<InstalledApp>> =
        combine(observeInstalledApps(), observeDockApps()) { installed, pinned ->
            installed.filterNot { it.packageName in pinned }
        }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000), initialValue = emptyList())

    fun recordLaunchLocation(packageName: String, location: LaunchLocation) {
        viewModelScope.launch {
            val current = observeOtherScreenLaunchApps().first()
            val updated = if (location == LaunchLocation.OtherScreen) current + packageName else current - packageName
            setOtherScreenLaunchApps(updated)
        }
    }

    /** No-op if [packageName] is already pinned or the dock is already at [maxApps]. */
    fun addToDock(packageName: String) {
        viewModelScope.launch {
            val current = observeDockApps().first()
            if (packageName in current || current.size >= maxApps.value) return@launch
            setDockApps(current + packageName)
        }
    }

    fun removeFromDock(packageName: String) {
        viewModelScope.launch {
            val current = observeDockApps().first()
            setDockApps(current - packageName)
        }
    }

    /** No-op if [packageName] is already first. */
    fun moveLeft(packageName: String) {
        viewModelScope.launch {
            val current = observeDockApps().first()
            val index = current.indexOf(packageName)
            if (index <= 0) return@launch
            setDockApps(current.swapped(index, index - 1))
        }
    }

    /** No-op if [packageName] is already last. */
    fun moveRight(packageName: String) {
        viewModelScope.launch {
            val current = observeDockApps().first()
            val index = current.indexOf(packageName)
            if (index == -1 || index == current.lastIndex) return@launch
            setDockApps(current.swapped(index, index + 1))
        }
    }

    private fun List<String>.swapped(a: Int, b: Int): List<String> =
        toMutableList().also { it[a] = this[b]; it[b] = this[a] }
}

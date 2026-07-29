package com.esde.companion.ui.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esde.companion.domain.model.InstalledApp
import com.esde.companion.domain.model.LaunchLocation
import com.esde.companion.domain.usecase.ObserveDrawerOpacityUseCase
import com.esde.companion.domain.usecase.ObserveGridColumnsUseCase
import com.esde.companion.domain.usecase.ObserveHiddenAppsUseCase
import com.esde.companion.domain.usecase.ObserveInstalledAppsUseCase
import com.esde.companion.domain.usecase.ObserveOtherScreenLaunchAppsUseCase
import com.esde.companion.domain.usecase.SetHiddenAppsUseCase
import com.esde.companion.domain.usecase.SetOtherScreenLaunchAppsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppDrawerViewModel(
    observeInstalledApps: ObserveInstalledAppsUseCase,
    private val observeHiddenApps: ObserveHiddenAppsUseCase,
    private val setHiddenApps: SetHiddenAppsUseCase,
    private val observeOtherScreenLaunchApps: ObserveOtherScreenLaunchAppsUseCase,
    private val setOtherScreenLaunchApps: SetOtherScreenLaunchAppsUseCase,
    observeDrawerOpacity: ObserveDrawerOpacityUseCase,
    observeGridColumns: ObserveGridColumnsUseCase,
) : ViewModel() {

    // Only the apps the user hasn't hidden - see ManageAppsViewModel for the unfiltered
    // list used by the Settings management screen.
    val installedApps: StateFlow<List<InstalledApp>> =
        combine(observeInstalledApps(), observeHiddenApps()) { apps, hidden ->
            apps.filterNot { hidden.contains(it.packageName) }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptyList(),
        )

    /** Packages whose last-used launch location is the other screen - drives both the
     * App Drawer's dot indicator and what a plain single-tap does for that app. */
    val otherScreenLaunchApps: StateFlow<Set<String>> = observeOtherScreenLaunchApps()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptySet(),
        )

    val drawerOpacityPercent: StateFlow<Int> = observeDrawerOpacity()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = 80,
        )

    val gridColumns: StateFlow<Int> = observeGridColumns()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = 4,
        )

    /** Persists [location] as the new default for [packageName]. Called for every
     * launch action that should update the remembered preference - the long-press
     * menu's explicit choices and the double-tap gesture - but deliberately not for a
     * plain single tap, which just replays the existing preference instead of writing
     * it again. */
    fun recordLaunchLocation(packageName: String, location: LaunchLocation) {
        viewModelScope.launch {
            val current = observeOtherScreenLaunchApps().first()
            val updated = if (location == LaunchLocation.OtherScreen) current + packageName else current - packageName
            setOtherScreenLaunchApps(updated)
        }
    }

    fun hideApp(packageName: String) {
        viewModelScope.launch {
            val current = observeHiddenApps().first()
            setHiddenApps(current + packageName)
        }
    }
}
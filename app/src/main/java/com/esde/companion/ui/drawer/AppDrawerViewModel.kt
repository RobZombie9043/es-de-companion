package com.esde.companion.ui.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esde.companion.domain.model.InstalledApp
import com.esde.companion.domain.usecase.ObserveDrawerOpacityUseCase
import com.esde.companion.domain.usecase.ObserveGridColumnsUseCase
import com.esde.companion.domain.usecase.ObserveHiddenAppsUseCase
import com.esde.companion.domain.usecase.ObserveInstalledAppsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class AppDrawerViewModel(
    observeInstalledApps: ObserveInstalledAppsUseCase,
    observeHiddenApps: ObserveHiddenAppsUseCase,
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
}
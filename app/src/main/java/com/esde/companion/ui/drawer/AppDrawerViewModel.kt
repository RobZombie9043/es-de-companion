package com.esde.companion.ui.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esde.companion.domain.model.InstalledApp
import com.esde.companion.domain.usecase.ObserveInstalledAppsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class AppDrawerViewModel(
    observeInstalledApps: ObserveInstalledAppsUseCase,
) : ViewModel() {

    val installedApps: StateFlow<List<InstalledApp>> = observeInstalledApps()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptyList(),
        )
}
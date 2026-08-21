package com.esde.companion.ui.thor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esde.companion.domain.model.InstalledApp
import com.esde.companion.domain.usecase.ObserveInstalledAppsUseCase
import com.esde.companion.domain.usecase.ObserveTaskKillerExcludedPackagesUseCase
import com.esde.companion.domain.usecase.SetTaskKillerExcludedPackagesUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A single row in the Task Killer excluded-apps list: an installed app plus whether it's
 * currently protected from being force-stopped. */
data class TaskKillerExcludedAppRow(
    val app: InstalledApp,
    val isExcluded: Boolean,
)

/**
 * Backs the Task Killer excluded-apps picker (Thor Settings > Task Killer). Deliberately its
 * own ViewModel, mirroring [com.esde.companion.ui.thor.AutoFpsTriggerAppsViewModel]'s split
 * from [com.esde.companion.ui.settings.SettingsViewModel] - same shape, inverted checkbox
 * semantics ("protected from force-stop" instead of "boost to 120Hz"). Toggles apply
 * immediately, matching the rest of the Settings screen.
 */
class TaskKillerExcludedAppsViewModel(
    observeInstalledApps: ObserveInstalledAppsUseCase,
    private val observeTaskKillerExcludedPackages: ObserveTaskKillerExcludedPackagesUseCase,
    private val setTaskKillerExcludedPackages: SetTaskKillerExcludedPackagesUseCase,
) : ViewModel() {
    val rows: StateFlow<List<TaskKillerExcludedAppRow>> =
        combine(observeInstalledApps(), observeTaskKillerExcludedPackages()) { apps, excluded ->
            apps.map { app -> TaskKillerExcludedAppRow(app = app, isExcluded = excluded.contains(app.packageName)) }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptyList(),
        )

    fun onExcludedToggled(
        packageName: String,
        isExcluded: Boolean,
    ) {
        viewModelScope.launch {
            val current = observeTaskKillerExcludedPackages().first()
            val updated = if (isExcluded) current + packageName else current - packageName
            setTaskKillerExcludedPackages(updated)
        }
    }
}

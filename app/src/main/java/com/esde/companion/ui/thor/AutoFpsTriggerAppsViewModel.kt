package com.esde.companion.ui.thor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esde.companion.domain.model.InstalledApp
import com.esde.companion.domain.usecase.ObserveAutoFpsTriggerPackagesUseCase
import com.esde.companion.domain.usecase.ObserveInstalledAppsUseCase
import com.esde.companion.domain.usecase.SetAutoFpsTriggerPackagesUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A single row in the Auto FPS trigger-app list: an installed app plus whether entering it
 * should boost the top screen to 120Hz. */
data class AutoFpsTriggerAppRow(
    val app: InstalledApp,
    val isTrigger: Boolean,
)

/**
 * Backs the Auto FPS Mode trigger-app picker (Thor Settings > Auto FPS Mode). Deliberately
 * its own ViewModel, mirroring [com.esde.companion.ui.settings.ManageAppsViewModel]'s split
 * from [com.esde.companion.ui.settings.SettingsViewModel] - it owns a different shape of
 * state (the full installed-app list plus per-app trigger flags) than the rest of Settings.
 * Toggles apply immediately, matching the rest of the Settings screen.
 */
class AutoFpsTriggerAppsViewModel(
    observeInstalledApps: ObserveInstalledAppsUseCase,
    private val observeAutoFpsTriggerPackages: ObserveAutoFpsTriggerPackagesUseCase,
    private val setAutoFpsTriggerPackages: SetAutoFpsTriggerPackagesUseCase,
) : ViewModel() {
    val rows: StateFlow<List<AutoFpsTriggerAppRow>> =
        combine(observeInstalledApps(), observeAutoFpsTriggerPackages()) { apps, triggers ->
            apps.map { app -> AutoFpsTriggerAppRow(app = app, isTrigger = triggers.contains(app.packageName)) }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptyList(),
        )

    fun onTriggerToggled(
        packageName: String,
        isTrigger: Boolean,
    ) {
        viewModelScope.launch {
            val current = observeAutoFpsTriggerPackages().first()
            val updated = if (isTrigger) current + packageName else current - packageName
            setAutoFpsTriggerPackages(updated)
        }
    }
}

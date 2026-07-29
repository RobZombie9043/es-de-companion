package com.esde.companion.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esde.companion.domain.model.InstalledApp
import com.esde.companion.domain.usecase.ObserveHiddenAppsUseCase
import com.esde.companion.domain.usecase.ObserveInstalledAppsUseCase
import com.esde.companion.domain.usecase.SetHiddenAppsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A single row in the Manage Apps list: an installed app plus whether it's currently
 * hidden from the App Drawer. */
data class AppVisibilityRow(
    val app: InstalledApp,
    val isHidden: Boolean,
)

/**
 * Backs the Manage Apps subpage of App Drawer settings. Deliberately its own ViewModel
 * rather than folded into SettingsViewModel - it owns a different shape of state (the
 * full installed-app list plus per-app hidden flags and icon loading) than the rest of
 * Settings. Toggles apply immediately, matching the rest of the Settings screen.
 */
class ManageAppsViewModel(
    observeInstalledApps: ObserveInstalledAppsUseCase,
    private val observeHiddenApps: ObserveHiddenAppsUseCase,
    private val setHiddenApps: SetHiddenAppsUseCase,
) : ViewModel() {

    val rows: StateFlow<List<AppVisibilityRow>> =
        combine(observeInstalledApps(), observeHiddenApps()) { apps, hidden ->
            apps.map { app -> AppVisibilityRow(app = app, isHidden = hidden.contains(app.packageName)) }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = emptyList(),
        )

    /** [hidden] is the new hidden state to apply for [packageName]. */
    fun onVisibilityToggled(packageName: String, hidden: Boolean) {
        viewModelScope.launch {
            val currentHidden = observeHiddenApps().first()
            val updated = if (hidden) currentHidden + packageName else currentHidden - packageName
            setHiddenApps(updated)
        }
    }
}
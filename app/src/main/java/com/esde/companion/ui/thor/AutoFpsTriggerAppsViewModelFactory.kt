package com.esde.companion.ui.thor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.esde.companion.AppContainer

class AutoFpsTriggerAppsViewModelFactory(
    private val appContainer: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(AutoFpsTriggerAppsViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return AutoFpsTriggerAppsViewModel(
            observeInstalledApps = appContainer.observeInstalledAppsUseCase,
            observeAutoFpsTriggerPackages = appContainer.observeAutoFpsTriggerPackagesUseCase,
            setAutoFpsTriggerPackages = appContainer.setAutoFpsTriggerPackagesUseCase,
        ) as T
    }
}

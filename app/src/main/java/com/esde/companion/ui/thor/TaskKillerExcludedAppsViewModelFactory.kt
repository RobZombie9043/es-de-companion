package com.esde.companion.ui.thor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.esde.companion.AppContainer

class TaskKillerExcludedAppsViewModelFactory(
    private val appContainer: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(TaskKillerExcludedAppsViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return TaskKillerExcludedAppsViewModel(
            observeInstalledApps = appContainer.observeInstalledAppsUseCase,
            observeTaskKillerExcludedPackages = appContainer.observeTaskKillerExcludedPackagesUseCase,
            setTaskKillerExcludedPackages = appContainer.setTaskKillerExcludedPackagesUseCase,
        ) as T
    }
}

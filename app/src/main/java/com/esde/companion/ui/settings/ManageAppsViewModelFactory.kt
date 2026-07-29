package com.esde.companion.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.esde.companion.AppContainer

class ManageAppsViewModelFactory(
    private val appContainer: AppContainer,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ManageAppsViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return ManageAppsViewModel(
            observeInstalledApps = appContainer.observeInstalledAppsUseCase,
            observeHiddenApps = appContainer.observeHiddenAppsUseCase,
            setHiddenApps = appContainer.setHiddenAppsUseCase,
        ) as T
    }
}
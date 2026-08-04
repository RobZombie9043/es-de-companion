package com.esde.companion.ui.drawer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.esde.companion.AppContainer

class AppDrawerViewModelFactory(
    private val appContainer: AppContainer,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(AppDrawerViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return AppDrawerViewModel(
            observeInstalledApps = appContainer.observeInstalledAppsUseCase,
            observeHiddenApps = appContainer.observeHiddenAppsUseCase,
            setHiddenApps = appContainer.setHiddenAppsUseCase,
            observeOtherScreenLaunchApps = appContainer.observeOtherScreenLaunchAppsUseCase,
            setOtherScreenLaunchApps = appContainer.setOtherScreenLaunchAppsUseCase,
            observeOverlayOpacity = appContainer.observeOverlayOpacityUseCase,
            observeGridColumns = appContainer.observeGridColumnsUseCase,
        ) as T
    }
}
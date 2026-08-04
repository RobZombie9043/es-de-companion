package com.esde.companion.ui.dock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.esde.companion.AppContainer

class AppDockViewModelFactory(
    private val appContainer: AppContainer,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(AppDockViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return AppDockViewModel(
            observeDockEnabled = appContainer.observeDockEnabledUseCase,
            observeDockApps = appContainer.observeDockAppsUseCase,
            setDockApps = appContainer.setDockAppsUseCase,
            observeDockMaxApps = appContainer.observeDockMaxAppsUseCase,
            observeDockSize = appContainer.observeDockSizeUseCase,
            observeOverlayOpacity = appContainer.observeOverlayOpacityUseCase,
            observeInstalledApps = appContainer.observeInstalledAppsUseCase,
            observeOtherScreenLaunchApps = appContainer.observeOtherScreenLaunchAppsUseCase,
            setOtherScreenLaunchApps = appContainer.setOtherScreenLaunchAppsUseCase,
        ) as T
    }
}

package com.esde.companion.ui.widgets.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.esde.companion.AppContainer

class EditWidgetsViewModelFactory(
    private val appContainer: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(EditWidgetsViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return EditWidgetsViewModel(
            observeWidgetCanvas = appContainer.observeWidgetCanvasUseCase,
            saveWidgetCanvas = appContainer.saveWidgetCanvasUseCase,
            resolveGameMedia = appContainer.resolveGameMediaUseCase,
            resolveRandomSystemMedia = appContainer.resolveRandomSystemMediaUseCase,
            resolveGameDescription = appContainer.resolveGameDescriptionUseCase,
            resolveGameRating = appContainer.resolveGameRatingUseCase,
            resolveCustomSystemImage = appContainer.resolveCustomSystemImageUseCase,
            resolveCustomSystemLogo = appContainer.resolveCustomSystemLogoUseCase,
            resolveBundledSystemLogo = appContainer.resolveBundledSystemLogoUseCase,
            observeLastSystemShortName = appContainer.observeLastSystemShortNameUseCase,
            observeLastGameReference = appContainer.observeLastGameReferenceUseCase,
            observeDockEnabled = appContainer.observeDockEnabledUseCase,
            observeDockSize = appContainer.observeDockSizeUseCase,
            observeOverlayOpacity = appContainer.observeOverlayOpacityUseCase,
            observeDockMaxApps = appContainer.observeDockMaxAppsUseCase,
            observeDockApps = appContainer.observeDockAppsUseCase,
            observeInstalledApps = appContainer.observeInstalledAppsUseCase,
        ) as T
    }
}

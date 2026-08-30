package com.esde.companion.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.esde.companion.AppContainer

class DownloadedGuidesViewModelFactory(
    private val appContainer: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(DownloadedGuidesViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return DownloadedGuidesViewModel(
            observeAllGameGuides = appContainer.observeAllGameGuidesUseCase,
            deleteGameGuide = appContainer.deleteGameGuideUseCase,
        ) as T
    }
}

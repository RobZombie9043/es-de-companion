package com.esde.companion.ui.widgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.esde.companion.AppContainer

class WidgetsViewModelFactory(
    private val appContainer: AppContainer,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(WidgetsViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return WidgetsViewModel(
            observeConnectionState = appContainer.observeConnectionStateUseCase,
            observeWidgetCanvas = appContainer.observeWidgetCanvasUseCase,
            resolveGameMedia = appContainer.resolveGameMediaUseCase,
            resolveRandomSystemMedia = appContainer.resolveRandomSystemMediaUseCase,
        ) as T
    }
}
package com.esde.companion.ui.manual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.esde.companion.AppContainer

class GameManualViewModelFactory(
    private val appContainer: AppContainer,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(GameManualViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return GameManualViewModel(
            observeConnectionState = appContainer.observeConnectionStateUseCase,
            resolveGameMedia = appContainer.resolveGameMediaUseCase,
        ) as T
    }
}
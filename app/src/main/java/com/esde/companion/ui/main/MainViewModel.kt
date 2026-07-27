package com.esde.companion.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esde.companion.domain.model.AppState
import com.esde.companion.domain.usecase.ObserveAppStateUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MainViewModel(
    observeAppState: ObserveAppStateUseCase,
) : ViewModel() {

    val appState: StateFlow<AppState> = observeAppState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = AppState.Idle,
        )
}

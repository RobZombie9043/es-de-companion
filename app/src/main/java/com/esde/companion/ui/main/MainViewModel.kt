package com.esde.companion.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esde.companion.domain.model.EsdeConnectionState
import com.esde.companion.domain.usecase.ObserveConnectionStateUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MainViewModel(
    observeConnectionState: ObserveConnectionStateUseCase,
) : ViewModel() {

    val connectionState: StateFlow<EsdeConnectionState> = observeConnectionState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = EsdeConnectionState.LogFileNotFound,
        )
}
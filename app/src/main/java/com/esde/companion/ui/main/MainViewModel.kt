package com.esde.companion.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esde.companion.domain.model.EsdeConnectionState
import com.esde.companion.domain.model.ScreenBehavior
import com.esde.companion.domain.usecase.ObserveConnectionStateUseCase
import com.esde.companion.domain.usecase.ObserveGamePlayingBehaviorUseCase
import com.esde.companion.domain.usecase.ObserveScreensaverBehaviorUseCase
import com.esde.companion.domain.usecase.ObserveVideoPlaybackEnabledUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Backdrop/logo rendering (previously mainScreenImageState/MainScreenImages) has moved
 * to WidgetsViewModel/WidgetOverlay - this class now only owns connection status, the
 * screen-behavior settings MainActivity combines with it to drive the automatic Dim/Black
 * cover, and whether video playback is enabled (used for the video-overlay condition in
 * MainActivity - the toggle itself lives in Settings > Video Playback, not here).
 */
class MainViewModel(
    observeConnectionState: ObserveConnectionStateUseCase,
    observeGamePlayingBehavior: ObserveGamePlayingBehaviorUseCase,
    observeScreensaverBehavior: ObserveScreensaverBehaviorUseCase,
    observeVideoPlaybackEnabled: ObserveVideoPlaybackEnabledUseCase,
) : ViewModel() {
    val connectionState: StateFlow<EsdeConnectionState> =
        observeConnectionState()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = EsdeConnectionState.LogFileNotFound,
            )

    val videoPlaybackEnabled: StateFlow<Boolean> =
        observeVideoPlaybackEnabled()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = false,
            )

    // Settings > UI Settings: how the main screen should react while a game is
    // playing / the screensaver is active - see MainActivity, which combines these
    // with connectionState to drive the automatic Dim/Black cover.
    val gamePlayingBehavior: StateFlow<ScreenBehavior> =
        observeGamePlayingBehavior()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = ScreenBehavior.Nothing,
            )

    val screensaverBehavior: StateFlow<ScreenBehavior> =
        observeScreensaverBehavior()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = ScreenBehavior.Nothing,
            )
}

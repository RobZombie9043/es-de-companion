package com.esde.companion.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esde.companion.domain.model.EsdeConnectionState
import com.esde.companion.domain.model.ScreenBehavior
import com.esde.companion.domain.usecase.ObserveConnectionStateUseCase
import com.esde.companion.domain.usecase.ObserveGamePlayingBehaviorUseCase
import com.esde.companion.domain.usecase.ObserveMusicEnabledUseCase
import com.esde.companion.domain.usecase.ObserveScreensaverBehaviorUseCase
import com.esde.companion.domain.usecase.ObserveVideoPlaybackEnabledUseCase
import com.esde.companion.domain.usecase.SetMusicEnabledUseCase
import com.esde.companion.domain.usecase.SetVideoPlaybackEnabledUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backdrop/logo rendering (previously mainScreenImageState/MainScreenImages) has moved
 * to WidgetsViewModel/WidgetOverlay - this class now only owns connection status and the
 * screen-behavior settings MainActivity combines with it to drive the automatic Dim/Black
 * cover, plus whether video playback/music is enabled and the master on/off toggles for
 * each (Settings > Video Playback / Background Music own every other knob - see
 * MainScreen's long-press menu for where these two toggles surface).
 */
class MainViewModel(
    observeConnectionState: ObserveConnectionStateUseCase,
    observeGamePlayingBehavior: ObserveGamePlayingBehaviorUseCase,
    observeScreensaverBehavior: ObserveScreensaverBehaviorUseCase,
    observeVideoPlaybackEnabled: ObserveVideoPlaybackEnabledUseCase,
    private val setVideoPlaybackEnabled: SetVideoPlaybackEnabledUseCase,
    observeMusicEnabled: ObserveMusicEnabledUseCase,
    private val setMusicEnabled: SetMusicEnabledUseCase,
) : ViewModel() {

    val connectionState: StateFlow<EsdeConnectionState> = observeConnectionState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = EsdeConnectionState.LogFileNotFound,
        )

    val videoPlaybackEnabled: StateFlow<Boolean> = observeVideoPlaybackEnabled()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = false,
        )

    val musicEnabled: StateFlow<Boolean> = observeMusicEnabled()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = true,
        )

    /** Master Settings > Video Playback toggle - every other video setting is untouched. */
    fun toggleVideoPlaybackEnabled() {
        viewModelScope.launch { setVideoPlaybackEnabled(!videoPlaybackEnabled.value) }
    }

    /** Master Settings > Background Music toggle - every other music setting is untouched. */
    fun toggleMusicEnabled() {
        viewModelScope.launch { setMusicEnabled(!musicEnabled.value) }
    }

    // Settings > UI Settings: how the main screen should react while a game is
    // playing / the screensaver is active - see MainActivity, which combines these
    // with connectionState to drive the automatic Dim/Black cover.
    val gamePlayingBehavior: StateFlow<ScreenBehavior> = observeGamePlayingBehavior()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = ScreenBehavior.Nothing,
        )

    val screensaverBehavior: StateFlow<ScreenBehavior> = observeScreensaverBehavior()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = ScreenBehavior.Nothing,
        )
}

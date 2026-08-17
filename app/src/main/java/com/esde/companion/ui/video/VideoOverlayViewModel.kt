package com.esde.companion.ui.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esde.companion.domain.model.AppState
import com.esde.companion.domain.model.EsdeConnectionState
import com.esde.companion.domain.model.GameReference
import com.esde.companion.domain.model.MediaType
import com.esde.companion.domain.usecase.ObserveConnectionStateUseCase
import com.esde.companion.domain.usecase.ObserveVideoAudioEnabledUseCase
import com.esde.companion.domain.usecase.ObserveVideoDelaySecondsUseCase
import com.esde.companion.domain.usecase.ObserveVideoPlaybackEnabledUseCase
import com.esde.companion.domain.usecase.ResolveGameMediaUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Resolves "what video, if any, is there for the game currently being browsed" -
 * structurally parallel to GameManualViewModel, with one important difference in the
 * source flow: [videoPath] is only ever non-null while AppState is exactly
 * AppState.BrowsingGame AND video playback (Settings > Video Playback) is enabled,
 * deliberately narrower than currentGameReference() (which also covers PlayingGame and
 * Screensaver). The AppState restriction is what keeps a video from resolving during
 * actual gameplay, on top of the visibility gating MainActivity applies separately (see
 * ActivityVisibilityRepository) - AppState alone is not trustworthy here, since ES-DE can
 * fire a spurious game-select while scrolling during real gameplay. The video-playback-
 * enabled restriction keeps this resolver (and therefore the opt-in debug log - see
 * LoggingGameMediaRepository) quiet while the feature is off, rather than resolving media
 * MainActivity's mainScreenActive/isActivityVisible gate would never actually display.
 */
class VideoOverlayViewModel(
    observeConnectionState: ObserveConnectionStateUseCase,
    private val resolveGameMedia: ResolveGameMediaUseCase,
    observeVideoDelaySeconds: ObserveVideoDelaySecondsUseCase,
    observeVideoAudioEnabled: ObserveVideoAudioEnabledUseCase,
    observeVideoPlaybackEnabled: ObserveVideoPlaybackEnabledUseCase,
) : ViewModel() {
    private val browsingGameReference: Flow<GameReference?> =
        combine(observeConnectionState(), observeVideoPlaybackEnabled()) { connection, enabled ->
            ((connection as? EsdeConnectionState.Connected)?.appState as? AppState.BrowsingGame)
                .takeIf { enabled }
                ?.let { GameReference(it.systemShortName, it.romPath, it.systemPath) }
        }.distinctUntilChanged()

    val videoPath: StateFlow<String?> =
        browsingGameReference
            .map { ref ->
                ref?.let {
                    resolveGameMedia(
                        systemShortName = it.systemShortName,
                        systemPath = it.systemPath,
                        romPath = it.romPath,
                        mediaTypes = setOf(MediaType.Videos),
                    ).path(MediaType.Videos)
                }
            }
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = null,
            )

    val delaySeconds: StateFlow<Int> =
        observeVideoDelaySeconds()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = 0,
            )

    val audioEnabled: StateFlow<Boolean> =
        observeVideoAudioEnabled()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = true,
            )
}

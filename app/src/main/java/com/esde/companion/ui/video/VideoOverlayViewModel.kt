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
import com.esde.companion.domain.usecase.ResolveGameMediaUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Resolves "what video, if any, is there for the game currently being browsed" -
 * structurally parallel to GameManualViewModel, with one important difference in the
 * source flow: [videoPath] is only ever non-null while AppState is exactly
 * AppState.BrowsingGame, deliberately narrower than currentGameReference() (which also
 * covers PlayingGame and Screensaver). That distinction is what keeps a video from
 * resolving during actual gameplay, on top of the visibility gating MainActivity applies
 * separately (see ActivityVisibilityRepository) - AppState alone is not trustworthy here,
 * since ES-DE can fire a spurious game-select while scrolling during real gameplay.
 *
 * Whether the feature is enabled at all (Settings > UI Settings > Video Playback) is
 * deliberately NOT checked here, same reasoning as GameManualViewModel not knowing about
 * gamePlayingBehavior - that decision belongs to MainActivity, alongside
 * mainScreenActive/isActivityVisible, not to the content resolver.
 */
class VideoOverlayViewModel(
    observeConnectionState: ObserveConnectionStateUseCase,
    private val resolveGameMedia: ResolveGameMediaUseCase,
    observeVideoDelaySeconds: ObserveVideoDelaySecondsUseCase,
    observeVideoAudioEnabled: ObserveVideoAudioEnabledUseCase,
) : ViewModel() {

    private val browsingGameReference: Flow<GameReference?> = observeConnectionState()
        .map { connection ->
            ((connection as? EsdeConnectionState.Connected)?.appState as? AppState.BrowsingGame)
                ?.let { GameReference(it.systemShortName, it.romPath) }
        }
        .distinctUntilChanged()

    val videoPath: StateFlow<String?> = browsingGameReference
        .map { ref -> ref?.let { resolveGameMedia(it.systemShortName, it.romPath).path(MediaType.Videos) } }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = null,
        )

    val delaySeconds: StateFlow<Int> = observeVideoDelaySeconds()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = 0,
        )

    val audioEnabled: StateFlow<Boolean> = observeVideoAudioEnabled()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = true,
        )
}
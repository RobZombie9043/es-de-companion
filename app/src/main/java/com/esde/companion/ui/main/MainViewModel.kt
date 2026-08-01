package com.esde.companion.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esde.companion.domain.model.EsdeConnectionState
import com.esde.companion.domain.model.GameReference
import com.esde.companion.domain.model.MediaType
import com.esde.companion.domain.model.ScreenBehavior
import com.esde.companion.domain.model.currentGameReference
import com.esde.companion.domain.repository.OnboardingRepository
import com.esde.companion.domain.usecase.ObserveConnectionStateUseCase
import com.esde.companion.domain.usecase.ObserveGamePlayingBehaviorUseCase
import com.esde.companion.domain.usecase.ObserveOverlayEnabledUseCase
import com.esde.companion.domain.usecase.ObserveScreensaverBehaviorUseCase
import com.esde.companion.domain.usecase.ResolveGameMediaUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Backdrop/logo rendering (previously mainScreenImageState/MainScreenImages) has moved
 * to WidgetsViewModel/WidgetOverlay - this class now only owns connection status,
 * the debug overlay toggle, and the cover-image-lookup status the debug overlay displays.
 */
class MainViewModel(
    observeConnectionState: ObserveConnectionStateUseCase,
    observeOverlayEnabled: ObserveOverlayEnabledUseCase,
    observeGamePlayingBehavior: ObserveGamePlayingBehaviorUseCase,
    observeScreensaverBehavior: ObserveScreensaverBehaviorUseCase,
    private val resolveGameMedia: ResolveGameMediaUseCase,
    private val onboardingRepository: OnboardingRepository,
) : ViewModel() {

    val connectionState: StateFlow<EsdeConnectionState> = observeConnectionState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = EsdeConnectionState.LogFileNotFound,
        )

    val overlayEnabled: StateFlow<Boolean> = observeOverlayEnabled()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = true,
        )

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

    private val currentGameReference: Flow<GameReference?> = connectionState
        .map { connection -> (connection as? EsdeConnectionState.Connected)?.appState?.currentGameReference() }
        .distinctUntilChanged()

    val coverImageStatus: StateFlow<CoverImageStatus?> =
        combine(currentGameReference, onboardingRepository.observeMediaFolderPath()) { gameRef, mediaFolder ->
            resolveCoverStatus(gameRef, mediaFolder)
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = null,
            )

    private suspend fun resolveCoverStatus(gameRef: GameReference?, mediaFolder: String?): CoverImageStatus? {
        if (gameRef == null || mediaFolder == null) return null

        val media = resolveGameMedia(gameRef.systemShortName, gameRef.romPath)
        val baseRelativePath = media.baseRelativePath ?: return null

        val exampleFilePath = "$mediaFolder/${gameRef.systemShortName}/${MediaType.Covers.folderName}/$baseRelativePath.png"
        return CoverImageStatus(exampleFilePath = exampleFilePath, found = media.path(MediaType.Covers) != null)
    }
}

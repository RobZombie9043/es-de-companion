package com.esde.companion.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esde.companion.domain.model.AppState
import com.esde.companion.domain.model.EsdeConnectionState
import com.esde.companion.domain.model.GameMedia
import com.esde.companion.domain.model.GameReference
import com.esde.companion.domain.model.MediaType
import com.esde.companion.domain.model.currentGameReference
import com.esde.companion.domain.repository.OnboardingRepository
import com.esde.companion.domain.usecase.ObserveConnectionStateUseCase
import com.esde.companion.domain.usecase.ObserveOverlayEnabledUseCase
import com.esde.companion.domain.usecase.ResolveGameMediaUseCase
import com.esde.companion.domain.usecase.ResolveRandomSystemFanartUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MainViewModel(
    observeConnectionState: ObserveConnectionStateUseCase,
    observeOverlayEnabled: ObserveOverlayEnabledUseCase,
    private val resolveGameMedia: ResolveGameMediaUseCase,
    private val resolveRandomSystemFanart: ResolveRandomSystemFanartUseCase,
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

    private val currentGameMedia: Flow<GameMedia?> = currentGameReference
        .map { gameRef -> gameRef?.let { resolveGameMedia(it.systemShortName, it.romPath) } }

    // Re-rolled only when the browsed system actually changes, not on every state
    // emission - otherwise the backdrop would flicker to a new random image on
    // unrelated events (e.g. re-selecting the same system).
    private val currentSystemFanart: Flow<String?> = connectionState
        .map { (it as? EsdeConnectionState.Connected)?.appState as? AppState.BrowsingSystem }
        .map { it?.systemShortName }
        .distinctUntilChanged()
        .map { systemShortName -> systemShortName?.let { resolveRandomSystemFanart(it) } }

    val mainScreenImageState: StateFlow<MainScreenImageState> =
        combine(connectionState, currentGameMedia, currentSystemFanart) { connection, gameMedia, systemFanart ->
            toImageState(connection, gameMedia, systemFanart)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = MainScreenImageState.None,
        )

    private fun toImageState(
        connection: EsdeConnectionState,
        gameMedia: GameMedia?,
        systemFanart: String?,
    ): MainScreenImageState {
        val appState = (connection as? EsdeConnectionState.Connected)?.appState ?: return MainScreenImageState.None
        return when (appState) {
            is AppState.BrowsingSystem -> MainScreenImageState.SystemBackdrop(
                fanartPath = systemFanart,
                systemLogoAssetPath = "file:///android_asset/system_logos/${systemLogoAssetName(appState.systemShortName)}.svg",
            )

            is AppState.BrowsingGame,
            is AppState.PlayingGame,
                -> gameMedia.toBackdropState()

            is AppState.Screensaver ->
                if (appState.currentGame != null) gameMedia.toBackdropState() else MainScreenImageState.None

            AppState.Idle -> MainScreenImageState.None
        }
    }

    private fun GameMedia?.toBackdropState(): MainScreenImageState {
        val media = this ?: return MainScreenImageState.None
        val backdrop = media.path(MediaType.FanArt) ?: media.path(MediaType.Screenshots)
        return MainScreenImageState.GameBackdrop(backdropPath = backdrop, logoPath = media.path(MediaType.Marquees))
    }

    private suspend fun resolveCoverStatus(gameRef: GameReference?, mediaFolder: String?): CoverImageStatus? {
        if (gameRef == null || mediaFolder == null) return null

        val media = resolveGameMedia(gameRef.systemShortName, gameRef.romPath)
        val baseRelativePath = media.baseRelativePath ?: return null

        val exampleFilePath = "$mediaFolder/${gameRef.systemShortName}/${MediaType.Covers.folderName}/$baseRelativePath.png"
        return CoverImageStatus(exampleFilePath = exampleFilePath, found = media.path(MediaType.Covers) != null)
    }
}
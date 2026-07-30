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
import com.esde.companion.domain.usecase.ResolveRandomSystemMediaUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MainViewModel(
    observeConnectionState: ObserveConnectionStateUseCase,
    observeOverlayEnabled: ObserveOverlayEnabledUseCase,
    private val resolveGameMedia: ResolveGameMediaUseCase,
    private val resolveRandomSystemMedia: ResolveRandomSystemMediaUseCase,
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

    // What the main screen's backdrop/logo should be driven by, distilled from
    // connectionState down to just the identity that actually matters for image
    // resolution. distinctUntilChanged() here is what stops an unrelated AppState field
    // changing (while still browsing the same system, or the same game) from re-running
    // the resolution below - same intent as the old per-branch distinctUntilChanged()s,
    // just unified across both the system and game cases.
    private val imageSource: Flow<ImageSource> = connectionState
        .map { connection -> toImageSource(connection) }
        .distinctUntilChanged()

    // Previously this was combine(connectionState, currentGameMedia, currentSystemFanart)
    // { ... }, three independently-collected flows. combine() re-emits as soon as ANY of
    // them ticks, using whatever stale value the other two last held - so on a
    // connectionState change, it fired immediately with the *old* gameMedia/fanart still
    // attached. For system<->system or game<->game that stale pairing happened to be
    // identical to the pre-transition frame (invisible), but crossing the system<->game
    // boundary (or entering/exiting Screensaver) it produced a real, visible
    // MainScreenImageState.None - a flash to the fallback background - before the actual
    // media resolved a moment later and it crossfaded again. Two crossfades instead of
    // one is exactly the "less smooth" stutter.
    //
    // flatMapLatest fixes this by resolving each ImageSource fully (including the
    // suspend gameMedia/fanart lookup) before emitting anything at all, so
    // mainScreenImageState only ever carries fully-resolved target states - every
    // transition is a single clean crossfade. It also cancels an in-flight resolution if
    // the source changes again before it finishes, which avoids a slow lookup for an
    // already-abandoned game/system landing late and clobbering a newer one.
    @OptIn(ExperimentalCoroutinesApi::class)
    val mainScreenImageState: StateFlow<MainScreenImageState> =
        imageSource
            .flatMapLatest { source -> flow { emit(resolveImageState(source)) } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = MainScreenImageState.None,
            )

    private fun toImageSource(connection: EsdeConnectionState): ImageSource {
        val appState = (connection as? EsdeConnectionState.Connected)?.appState ?: return ImageSource.None
        return when (appState) {
            is AppState.BrowsingSystem -> ImageSource.System(appState.systemShortName)

            // BrowsingGame, PlayingGame, Screensaver (with a currentGame), and Idle all
            // fall out of currentGameReference() correctly - it already encodes exactly
            // this mapping (see its use above for coverImageStatus), including
            // Screensaver-with-no-game and Idle both yielding null -> ImageSource.None.
            else -> appState.currentGameReference()?.let { ImageSource.Game(it) } ?: ImageSource.None
        }
    }

    private suspend fun resolveImageState(source: ImageSource): MainScreenImageState = when (source) {
        ImageSource.None -> MainScreenImageState.None

        is ImageSource.System -> MainScreenImageState.SystemBackdrop(
            fanartPath = resolveRandomSystemMedia(source.systemShortName, MediaType.FanArt),
            systemLogoAssetPath = "file:///android_asset/system_logos/${systemLogoAssetName(source.systemShortName)}.svg",
        )

        is ImageSource.Game ->
            resolveGameMedia(source.gameRef.systemShortName, source.gameRef.romPath).toBackdropState()
    }

    private fun GameMedia.toBackdropState(): MainScreenImageState {
        val backdrop = path(MediaType.FanArt) ?: path(MediaType.Screenshots)
        return MainScreenImageState.GameBackdrop(backdropPath = backdrop, logoPath = path(MediaType.Marquees))
    }

    private suspend fun resolveCoverStatus(gameRef: GameReference?, mediaFolder: String?): CoverImageStatus? {
        if (gameRef == null || mediaFolder == null) return null

        val media = resolveGameMedia(gameRef.systemShortName, gameRef.romPath)
        val baseRelativePath = media.baseRelativePath ?: return null

        val exampleFilePath = "$mediaFolder/${gameRef.systemShortName}/${MediaType.Covers.folderName}/$baseRelativePath.png"
        return CoverImageStatus(exampleFilePath = exampleFilePath, found = media.path(MediaType.Covers) != null)
    }

    /**
     * The identity that drives what backdrop/logo should be shown - a distillation of
     * AppState down to just what image resolution cares about, so [imageSource] can
     * distinctUntilChanged() on it directly instead of on raw AppState (which changes on
     * fields image resolution doesn't care about) or juggling separate media/fanart
     * flows (see the comment on [mainScreenImageState] for why that was the bug).
     */
    private sealed class ImageSource {
        data object None : ImageSource()
        data class System(val systemShortName: String) : ImageSource()
        data class Game(val gameRef: GameReference) : ImageSource()
    }
}
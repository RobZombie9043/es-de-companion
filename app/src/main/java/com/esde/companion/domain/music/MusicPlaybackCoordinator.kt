package com.esde.companion.domain.music

import com.esde.companion.domain.model.AppState
import com.esde.companion.domain.model.EsdeConnectionState
import com.esde.companion.domain.model.MusicDuckingMode
import com.esde.companion.domain.model.MusicPlaybackState
import com.esde.companion.domain.model.MusicPool
import com.esde.companion.domain.model.MusicTrack
import com.esde.companion.domain.repository.ActivityVisibilityRepository
import com.esde.companion.domain.repository.MusicLibraryRepository
import com.esde.companion.domain.repository.MusicPlayerController
import com.esde.companion.domain.repository.VideoPlaybackStateRepository
import com.esde.companion.domain.usecase.ObserveConnectionStateUseCase
import com.esde.companion.domain.usecase.ObserveMusicDuckingModeUseCase
import com.esde.companion.domain.usecase.ObserveMusicEnabledUseCase
import com.esde.companion.domain.usecase.ObserveMusicPlayDuringScreensaverUseCase
import com.esde.companion.domain.usecase.ObserveMusicPlayWhileBrowsingGamesUseCase
import com.esde.companion.domain.usecase.ObserveMusicPlayWhileBrowsingSystemsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

/**
 * Owns the entire background-music session: which pool is loaded, which track is current,
 * and whether it's audibly playing right now. Plain Kotlin, no Android imports - the only
 * impure edges it touches are [MusicLibraryRepository] and [MusicPlayerController].
 *
 * All mutable session state below is coroutine-confined - read and written only from
 * inside this class's own single collection loop (the merged snapshot/track-completion/
 * user-action flow in [init]). The public [togglePlayPause]/[next] entry points do not
 * mutate state directly - they [MutableSharedFlow.tryEmit] onto [userActions], a third arm
 * of the same [merge], so a UI-thread tap is turned into just another event processed by
 * the one collector rather than a second writer racing it. This is the same single-actor
 * discipline [com.esde.companion.domain.state.AppStateReducer] relies on.
 *
 * [init] used to launch two independent `applicationScope.launch` coroutines - one for the
 * settings/AppState snapshot loop, one collecting [MusicPlayerController.observeTrackCompletion]
 * - both mutating the same private vars with no synchronization. applicationScope runs on
 * Dispatchers.Default (multi-threaded), so those two coroutines could genuinely execute
 * concurrently on different threads: a real data race, not a theoretical one, that showed
 * up as spurious/flickery [MusicPlaybackState.Stopped] flips - e.g. the music FAB and
 * MusicControlsOverlay briefly appearing then disappearing as a torn read of
 * currentTrack/loadedPool got published as a state. [merge]-ing both event sources into
 * one flow collected by one coroutine fixed that structurally rather than papering over it
 * with a Mutex - but [togglePlayPause]/[next] were still calling straight into the shared
 * vars from whatever thread invoked them (the main thread, via Compose `onClick`), racing
 * the same vars against the Default-dispatcher collector. Routing them through [userActions]
 * closes that gap the same way: there is now genuinely only one place these vars are ever
 * touched.
 */
class MusicPlaybackCoordinator(
    observeConnectionState: ObserveConnectionStateUseCase,
    observeMusicEnabled: ObserveMusicEnabledUseCase,
    observeMusicPlayWhileBrowsingSystems: ObserveMusicPlayWhileBrowsingSystemsUseCase,
    observeMusicPlayWhileBrowsingGames: ObserveMusicPlayWhileBrowsingGamesUseCase,
    observeMusicPlayDuringScreensaver: ObserveMusicPlayDuringScreensaverUseCase,
    observeMusicDuckingMode: ObserveMusicDuckingModeUseCase,
    private val activityVisibilityRepository: ActivityVisibilityRepository,
    private val videoPlaybackStateRepository: VideoPlaybackStateRepository,
    private val musicLibraryRepository: MusicLibraryRepository,
    private val musicPlayerController: MusicPlayerController,
    applicationScope: CoroutineScope,
) {

    private val _playbackState = MutableStateFlow<MusicPlaybackState>(MusicPlaybackState.Stopped)
    val playbackState: StateFlow<MusicPlaybackState> = _playbackState.asStateFlow()

    private var loadedPool: MusicPool? = null
    private var loadedTracks: List<MusicTrack> = emptyList()
    private var currentTrack: MusicTrack? = null
    private var lastRequestedPool: MusicPool? = null
    private var userPaused: Boolean = false

    private var lastEligible: Boolean = false
    private var lastIsDuckedByVideo: Boolean = false
    private var lastDuckingMode: MusicDuckingMode = MusicDuckingMode.LowerVolume

    /**
     * Where [togglePlayPause]/[next] feed in - merged into [init]'s single collector so a
     * UI-thread tap becomes just another event on the one coroutine that owns the mutable
     * state above, rather than a second concurrent writer. Sized generously for user-tap
     * frequency; the collector is already running by the time either entry point is
     * reachable (the coordinator must be constructed, starting [init], before anything can
     * hold a reference to call them), so [MutableSharedFlow.tryEmit] should never actually
     * see a full buffer.
     */
    private val userActions = MutableSharedFlow<CoordinatorEvent>(extraBufferCapacity = 16)

    init {
        val appStateFlow =
            observeConnectionState().map { connection ->
                (connection as? EsdeConnectionState.Connected)?.appState ?: AppState.Idle
            }

        val settingsFlow =
            combine(
                observeMusicEnabled(),
                observeMusicPlayWhileBrowsingSystems(),
                observeMusicPlayWhileBrowsingGames(),
                observeMusicPlayDuringScreensaver(),
            ) { enabled, systems, games, screensaver ->
                MusicSettings(enabled = enabled, systems = systems, games = games, screensaver = screensaver)
            }

        val snapshotFlow =
            combine(
                appStateFlow,
                settingsFlow,
                activityVisibilityRepository.observeIsVisible(),
                videoPlaybackStateRepository.observeIsPlaying(),
                observeMusicDuckingMode(),
            ) { appState, settings, isVisible, isPlayingVideo, duckingMode ->
                CoordinatorEvent.SnapshotUpdated(Snapshot(appState, settings, isVisible, isPlayingVideo, duckingMode))
            }

        val trackCompletionFlow =
            musicPlayerController.observeTrackCompletion().map { CoordinatorEvent.TrackCompleted }

        applicationScope.launch {
            merge(snapshotFlow, trackCompletionFlow, userActions).collect { event ->
                when (event) {
                    is CoordinatorEvent.SnapshotUpdated -> handleSnapshot(event.snapshot)
                    CoordinatorEvent.TrackCompleted -> handleTrackCompletion()
                    CoordinatorEvent.PlayPauseToggled -> {
                        userPaused = !userPaused
                        applyCachedAction()
                    }
                }
            }
        }
    }

    fun togglePlayPause() {
        userActions.tryEmit(CoordinatorEvent.PlayPauseToggled)
    }

    fun next() {
        userActions.tryEmit(CoordinatorEvent.TrackCompleted)
    }

    private fun handleTrackCompletion() {
        advanceToNextTrack()
        currentTrack?.let(musicPlayerController::playTrack)
        applyCachedAction()
    }

    private suspend fun handleSnapshot(snapshot: Snapshot) {
        val eligibility = MusicEligibilityResolver.resolve(snapshot.appState, snapshot.settings)

        if (eligibility is MusicEligibility.Eligible && eligibility.pool != lastRequestedPool) {
            lastRequestedPool = eligibility.pool
            loadPoolAndMaybeSwitchTrack(eligibility.pool)
        }
        // Ineligible: leave loadedPool/loadedTracks/currentTrack untouched - eligibility
        // loss is handled purely by action resolution below, pausing in place.

        lastEligible = eligibility is MusicEligibility.Eligible && snapshot.isVisible
        lastIsDuckedByVideo = snapshot.isPlayingVideo && snapshot.duckingMode != MusicDuckingMode.Unchanged
        lastDuckingMode = snapshot.duckingMode
        applyCachedAction()
    }

    private suspend fun loadPoolAndMaybeSwitchTrack(desiredPool: MusicPool) {
        val contents = musicLibraryRepository.loadPool(desiredPool)
        if (contents.pool == loadedPool && currentTrack != null) {
            // Resolved pool unchanged from what's already loaded - same-pool-transition
            // guard, don't restart playback (e.g. two systems both falling back to General).
            return
        }
        val track = pickRandomTrack(contents.tracks, excluding = currentTrack)
        track?.let(musicPlayerController::playTrack)
        loadedPool = contents.pool
        loadedTracks = contents.tracks
        currentTrack = track
    }

    private fun advanceToNextTrack() {
        currentTrack = pickRandomTrack(loadedTracks, excluding = currentTrack)
    }

    private fun pickRandomTrack(tracks: List<MusicTrack>, excluding: MusicTrack?): MusicTrack? {
        if (tracks.isEmpty()) return null
        val candidates = tracks.filter { it != excluding }
        return candidates.ifEmpty { tracks }.random()
    }

    private fun applyCachedAction() {
        val track = currentTrack
        val pool = loadedPool
        val action =
            MusicPlayerActionResolver.resolve(
                hasCurrentTrack = track != null,
                eligible = lastEligible,
                userPaused = userPaused,
                isDuckedByVideo = lastIsDuckedByVideo,
                duckingMode = lastDuckingMode,
            )

        when (action) {
            MusicPlayerAction.NoTrackLoaded -> Unit
            MusicPlayerAction.Paused -> musicPlayerController.pause()
            is MusicPlayerAction.Playing -> {
                musicPlayerController.setVolume(action.volumeFraction)
                musicPlayerController.resume()
            }
        }

        _playbackState.value =
            when {
                track == null || pool == null -> MusicPlaybackState.Stopped
                action is MusicPlayerAction.Playing -> MusicPlaybackState.Playing(track, pool)
                else -> MusicPlaybackState.Paused(track, pool)
            }
    }

    private data class Snapshot(
        val appState: AppState,
        val settings: MusicSettings,
        val isVisible: Boolean,
        val isPlayingVideo: Boolean,
        val duckingMode: MusicDuckingMode,
    )

    /** The event sources merged into [init]'s single collector - see the class kdoc for why
     * [togglePlayPause]/[next] emit [PlayPauseToggled]/[TrackCompleted] here instead of
     * mutating state directly. */
    private sealed class CoordinatorEvent {
        data class SnapshotUpdated(val snapshot: Snapshot) : CoordinatorEvent()
        data object TrackCompleted : CoordinatorEvent()
        data object PlayPauseToggled : CoordinatorEvent()
    }
}

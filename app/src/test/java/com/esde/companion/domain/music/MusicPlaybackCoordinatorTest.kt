package com.esde.companion.domain.music

import app.cash.turbine.test
import com.esde.companion.domain.model.EsdeEvent
import com.esde.companion.domain.model.FabAssignments
import com.esde.companion.domain.model.LogFolderValidation
import com.esde.companion.domain.model.MediaFolderValidation
import com.esde.companion.domain.model.MusicDuckingMode
import com.esde.companion.domain.model.MusicPlaybackState
import com.esde.companion.domain.model.MusicPool
import com.esde.companion.domain.model.MusicTrack
import com.esde.companion.domain.model.ScreenBehavior
import com.esde.companion.domain.model.ThemePreference
import com.esde.companion.domain.repository.ActivityVisibilityRepository
import com.esde.companion.domain.repository.EsdeLogRepository
import com.esde.companion.domain.repository.MusicLibraryRepository
import com.esde.companion.domain.repository.MusicPlayerController
import com.esde.companion.domain.repository.MusicPoolContents
import com.esde.companion.domain.repository.OnboardingRepository
import com.esde.companion.domain.repository.VideoPlaybackStateRepository
import com.esde.companion.domain.usecase.ObserveAppStateUseCase
import com.esde.companion.domain.usecase.ObserveConnectionStateUseCase
import com.esde.companion.domain.usecase.ObserveMusicDuckingModeUseCase
import com.esde.companion.domain.usecase.ObserveMusicEnabledUseCase
import com.esde.companion.domain.usecase.ObserveMusicPlayDuringScreensaverUseCase
import com.esde.companion.domain.usecase.ObserveMusicPlayWhileBrowsingGamesUseCase
import com.esde.companion.domain.usecase.ObserveMusicPlayWhileBrowsingSystemsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicPlaybackCoordinatorTest {
    private class FakeEsdeLogRepository(
        private val events: Flow<EsdeEvent>,
    ) : EsdeLogRepository {
        override fun observeEvents(): Flow<EsdeEvent> = events

        override fun observeLogFileExists(): Flow<Boolean> = flowOf(true)
    }

    private class FakeOnboardingRepository : OnboardingRepository {
        val musicEnabled = MutableStateFlow(true)
        val musicPlayWhileBrowsingSystems = MutableStateFlow(true)
        val musicPlayWhileBrowsingGames = MutableStateFlow(true)
        val musicPlayDuringScreensaver = MutableStateFlow(true)
        val musicDuckingMode = MutableStateFlow(MusicDuckingMode.LowerVolume)

        override fun defaultLogFolderPath() = ""

        override fun defaultMediaFolderPath() = ""

        override suspend fun validateLogFolder(path: String) = LogFolderValidation.FolderNotFound

        override suspend fun validateMediaFolder(path: String) = MediaFolderValidation.FolderNotFound

        override suspend fun saveLogFolderPath(path: String) {}

        override suspend fun saveMediaFolderPath(path: String) {}

        override fun observeLogFolderPath(): Flow<String?> = flowOf(null)

        override fun observeMediaFolderPath(): Flow<String?> = flowOf(null)

        override suspend fun saveCustomSystemImagesFolderPath(path: String) {}

        override fun observeCustomSystemImagesFolderPath(): Flow<String?> = flowOf(null)

        override suspend fun clearCustomSystemImagesFolderPath() {}

        override suspend fun saveCustomLogosFolderPath(path: String) {}

        override fun observeCustomLogosFolderPath(): Flow<String?> = flowOf(null)

        override suspend fun clearCustomLogosFolderPath() {}

        override suspend fun markOnboardingComplete() {}

        override fun observeOnboardingComplete(): Flow<Boolean> = flowOf(true)

        override suspend fun setThemePreference(preference: ThemePreference) {}

        override fun observeThemePreference(): Flow<ThemePreference> = flowOf(ThemePreference.Auto)

        override suspend fun setGamePlayingBehavior(behavior: ScreenBehavior) {}

        override fun observeGamePlayingBehavior(): Flow<ScreenBehavior> = flowOf(ScreenBehavior.Nothing)

        override suspend fun setGamePlayingDimPercent(percent: Int) {}

        override fun observeGamePlayingDimPercent(): Flow<Int> = flowOf(50)

        override suspend fun setScreensaverBehavior(behavior: ScreenBehavior) {}

        override fun observeScreensaverBehavior(): Flow<ScreenBehavior> = flowOf(ScreenBehavior.Nothing)

        override suspend fun setScreensaverDimPercent(percent: Int) {}

        override fun observeScreensaverDimPercent(): Flow<Int> = flowOf(50)

        override suspend fun setVideoPlaybackEnabled(enabled: Boolean) {}

        override fun observeVideoPlaybackEnabled(): Flow<Boolean> = flowOf(false)

        override suspend fun setVideoDelaySeconds(seconds: Int) {}

        override fun observeVideoDelaySeconds(): Flow<Int> = flowOf(0)

        override suspend fun setVideoAudioEnabled(enabled: Boolean) {}

        override fun observeVideoAudioEnabled(): Flow<Boolean> = flowOf(true)

        override suspend fun setMusicEnabled(enabled: Boolean) {
            musicEnabled.value = enabled
        }

        override fun observeMusicEnabled(): Flow<Boolean> = musicEnabled

        override suspend fun setMusicPlayWhileBrowsingSystems(enabled: Boolean) {
            musicPlayWhileBrowsingSystems.value = enabled
        }

        override fun observeMusicPlayWhileBrowsingSystems(): Flow<Boolean> = musicPlayWhileBrowsingSystems

        override suspend fun setMusicPlayWhileBrowsingGames(enabled: Boolean) {
            musicPlayWhileBrowsingGames.value = enabled
        }

        override fun observeMusicPlayWhileBrowsingGames(): Flow<Boolean> = musicPlayWhileBrowsingGames

        override suspend fun setMusicPlayDuringScreensaver(enabled: Boolean) {
            musicPlayDuringScreensaver.value = enabled
        }

        override fun observeMusicPlayDuringScreensaver(): Flow<Boolean> = musicPlayDuringScreensaver

        override suspend fun setMusicDuckingMode(mode: MusicDuckingMode) {
            musicDuckingMode.value = mode
        }

        override fun observeMusicDuckingMode(): Flow<MusicDuckingMode> = musicDuckingMode

        override suspend fun setOverlayOpacityPercent(percent: Int) {}

        override fun observeOverlayOpacityPercent(): Flow<Int> = flowOf(80)

        override suspend fun saveCustomMusicFolderPath(path: String) {}

        override fun observeCustomMusicFolderPath(): Flow<String?> = flowOf(null)

        override suspend fun clearCustomMusicFolderPath() {}

        override suspend fun setCloseCompanionOnQuitEnabled(enabled: Boolean) {}

        override fun observeCloseCompanionOnQuitEnabled(): Flow<Boolean> = flowOf(false)

        override suspend fun setLaunchEsdeOnStartEnabled(enabled: Boolean) {}

        override fun observeLaunchEsdeOnStartEnabled(): Flow<Boolean> = flowOf(false)

        override suspend fun setDebugLoggingEnabled(enabled: Boolean) {}

        override fun observeDebugLoggingEnabled(): Flow<Boolean> = flowOf(false)

        override suspend fun setUpdateAchievementsOnScreensaverEnabled(enabled: Boolean) {}

        override fun observeUpdateAchievementsOnScreensaverEnabled(): Flow<Boolean> = flowOf(true)

        override suspend fun setFabAssignments(assignments: FabAssignments) {}

        override fun observeFabAssignments(): Flow<FabAssignments> = flowOf(FabAssignments.Default)
    }

    private class FakeMusicLibraryRepository(
        private val poolsByRequestedSystem: Map<String, MusicPoolContents>,
        private val generalPool: MusicPoolContents,
    ) : MusicLibraryRepository {
        var loadCallCount = 0

        override suspend fun loadPool(desired: MusicPool): MusicPoolContents {
            loadCallCount++
            return when (desired) {
                is MusicPool.General -> generalPool
                is MusicPool.PerSystem -> poolsByRequestedSystem[desired.systemShortName] ?: generalPool
            }
        }
    }

    private class FakeMusicPlayerController : MusicPlayerController {
        val playedTracks = mutableListOf<MusicTrack>()
        val volumeCalls = mutableListOf<Float>()
        var pauseCallCount = 0
        var resumeCallCount = 0
        private val completions = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        private val errors = MutableSharedFlow<String>(extraBufferCapacity = 1)

        override fun playTrack(track: MusicTrack) {
            playedTracks += track
        }

        override fun pause() {
            pauseCallCount++
        }

        override fun resume() {
            resumeCallCount++
        }

        override fun setVolume(fraction: Float) {
            volumeCalls += fraction
        }

        override fun observeTrackCompletion(): Flow<Unit> = completions

        override fun observePlaybackError(): Flow<String> = errors

        suspend fun completeCurrentTrack() = completions.emit(Unit)

        suspend fun emitError(message: String) = errors.emit(message)
    }

    private class FakeActivityVisibilityRepository : ActivityVisibilityRepository {
        val isVisible = MutableStateFlow(true)

        override fun observeIsVisible(): Flow<Boolean> = isVisible

        override fun setVisible(visible: Boolean) {
            isVisible.value = visible
        }
    }

    private class FakeVideoPlaybackStateRepository : VideoPlaybackStateRepository {
        val isPlaying = MutableStateFlow(false)

        override fun observeIsPlaying(): Flow<Boolean> = isPlaying

        override fun setIsPlaying(playing: Boolean) {
            isPlaying.value = playing
        }
    }

    private fun buildCoordinator(
        events: MutableSharedFlow<EsdeEvent>,
        musicLibraryRepository: MusicLibraryRepository,
        musicPlayerController: FakeMusicPlayerController,
        onboardingRepository: FakeOnboardingRepository = FakeOnboardingRepository(),
        activityVisibilityRepository: FakeActivityVisibilityRepository = FakeActivityVisibilityRepository(),
        videoPlaybackStateRepository: FakeVideoPlaybackStateRepository = FakeVideoPlaybackStateRepository(),
        scope: CoroutineScope,
        onTrackStarted: (MusicTrack) -> Unit = {},
        onPlaybackError: (MusicTrack?, String) -> Unit = { _, _ -> },
    ): MusicPlaybackCoordinator {
        val logRepository = FakeEsdeLogRepository(events)
        val observeConnectionState = ObserveConnectionStateUseCase(logRepository, ObserveAppStateUseCase(logRepository, scope))
        return MusicPlaybackCoordinator(
            observeConnectionState = observeConnectionState,
            observeMusicEnabled = ObserveMusicEnabledUseCase(onboardingRepository),
            observeMusicPlayWhileBrowsingSystems = ObserveMusicPlayWhileBrowsingSystemsUseCase(onboardingRepository),
            observeMusicPlayWhileBrowsingGames = ObserveMusicPlayWhileBrowsingGamesUseCase(onboardingRepository),
            observeMusicPlayDuringScreensaver = ObserveMusicPlayDuringScreensaverUseCase(onboardingRepository),
            observeMusicDuckingMode = ObserveMusicDuckingModeUseCase(onboardingRepository),
            activityVisibilityRepository = activityVisibilityRepository,
            videoPlaybackStateRepository = videoPlaybackStateRepository,
            musicLibraryRepository = musicLibraryRepository,
            musicPlayerController = musicPlayerController,
            applicationScope = scope,
            onTrackStarted = onTrackStarted,
            onPlaybackError = onPlaybackError,
        )
    }

    @Test
    fun `same resolved pool across a context change does not restart playback`() =
        runTest(UnconfinedTestDispatcher()) {
            val trackA1 = MusicTrack("/music/systems/a/1.mp3", "1")
            val trackGeneral = MusicTrack("/music/general.mp3", "general")
            val library =
                FakeMusicLibraryRepository(
                    poolsByRequestedSystem = mapOf("a" to MusicPoolContents(MusicPool.PerSystem("a"), listOf(trackA1))),
                    generalPool = MusicPoolContents(MusicPool.General, listOf(trackGeneral)),
                )
            val controller = FakeMusicPlayerController()
            val events = MutableSharedFlow<EsdeEvent>()
            val coordinator = buildCoordinator(events, library, controller, scope = backgroundScope)

            coordinator.playbackState.test {
                assertEquals(MusicPlaybackState.Stopped, awaitItem())

                events.emit(EsdeEvent.SystemSelect("a", "System A", "/roms/a"))
                assertEquals(MusicPlaybackState.Playing(trackA1, MusicPool.PerSystem("a")), awaitItem())

                // System "b" has no dedicated folder - falls back to General.
                events.emit(EsdeEvent.SystemSelect("b", "System B", "/roms/b"))
                assertEquals(MusicPlaybackState.Playing(trackGeneral, MusicPool.General), awaitItem())

                // System "c" also falls back to General - same resolved pool, no restart,
                // so no further playbackState emission for this transition.
                events.emit(EsdeEvent.SystemSelect("c", "System C", "/roms/c"))

                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(listOf(trackA1, trackGeneral), controller.playedTracks)
        }

    @Test
    fun `eligibility loss pauses in place and regaining eligibility resumes the same track`() =
        runTest(UnconfinedTestDispatcher()) {
            val trackA1 = MusicTrack("/music/systems/a/1.mp3", "1")
            val library =
                FakeMusicLibraryRepository(
                    poolsByRequestedSystem = mapOf("a" to MusicPoolContents(MusicPool.PerSystem("a"), listOf(trackA1))),
                    generalPool = MusicPoolContents(MusicPool.General, emptyList()),
                )
            val controller = FakeMusicPlayerController()
            val events = MutableSharedFlow<EsdeEvent>()
            val coordinator = buildCoordinator(events, library, controller, scope = backgroundScope)

            coordinator.playbackState.test {
                awaitItem() // Stopped

                events.emit(EsdeEvent.SystemSelect("a", "System A", "/roms/a"))
                assertEquals(MusicPlaybackState.Playing(trackA1, MusicPool.PerSystem("a")), awaitItem())

                events.emit(
                    EsdeEvent.GameStart(
                        romPath = "/roms/a/game.chd",
                        gameName = "Game",
                        systemShortName = "a",
                        systemFullName = "System A",
                    ),
                )
                assertEquals(MusicPlaybackState.Paused(trackA1, MusicPool.PerSystem("a")), awaitItem())

                events.emit(
                    EsdeEvent.GameEnd(
                        romPath = "/roms/a/game.chd",
                        gameName = "Game",
                        systemShortName = "a",
                        systemFullName = "System A",
                    ),
                )
                assertEquals(MusicPlaybackState.Playing(trackA1, MusicPool.PerSystem("a")), awaitItem())

                cancelAndIgnoreRemainingEvents()
            }

            // playTrack only ever called once - regaining eligibility resumed rather than
            // reloading/restarting the track.
            assertEquals(listOf(trackA1), controller.playedTracks)
            assertTrue(controller.pauseCallCount >= 1)
            assertTrue(controller.resumeCallCount >= 1)
        }

    @Test
    fun `a per-system pool with no tracks resolves to General, not an empty PerSystem pool`() =
        runTest(UnconfinedTestDispatcher()) {
            val trackGeneral = MusicTrack("/music/general.mp3", "general")
            val library =
                FakeMusicLibraryRepository(
                    poolsByRequestedSystem = emptyMap(),
                    generalPool = MusicPoolContents(MusicPool.General, listOf(trackGeneral)),
                )
            val controller = FakeMusicPlayerController()
            val events = MutableSharedFlow<EsdeEvent>()
            val coordinator = buildCoordinator(events, library, controller, scope = backgroundScope)

            coordinator.playbackState.test {
                awaitItem() // Stopped

                events.emit(EsdeEvent.SystemSelect("empty-system", "Empty System", "/roms/empty"))

                val playing = awaitItem()
                check(playing is MusicPlaybackState.Playing)
                assertEquals(MusicPool.General, playing.pool)
                assertEquals(trackGeneral, playing.track)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `ducking toggles volume without changing the current track`() =
        runTest(UnconfinedTestDispatcher()) {
            val trackA1 = MusicTrack("/music/systems/a/1.mp3", "1")
            val library =
                FakeMusicLibraryRepository(
                    poolsByRequestedSystem = mapOf("a" to MusicPoolContents(MusicPool.PerSystem("a"), listOf(trackA1))),
                    generalPool = MusicPoolContents(MusicPool.General, emptyList()),
                )
            val controller = FakeMusicPlayerController()
            val events = MutableSharedFlow<EsdeEvent>()
            val videoPlaybackStateRepository = FakeVideoPlaybackStateRepository()
            val coordinator =
                buildCoordinator(
                    events,
                    library,
                    controller,
                    videoPlaybackStateRepository = videoPlaybackStateRepository,
                    scope = backgroundScope,
                )

            coordinator.playbackState.test {
                awaitItem() // Stopped
                events.emit(EsdeEvent.SystemSelect("a", "System A", "/roms/a"))
                assertEquals(MusicPlaybackState.Playing(trackA1, MusicPool.PerSystem("a")), awaitItem())

                videoPlaybackStateRepository.setIsPlaying(true)
                // Still Playing (LowerVolume is the default ducking mode, not Pause) -
                // no new playbackState variant, but volume was lowered.
                expectNoEvents()

                videoPlaybackStateRepository.setIsPlaying(false)
                expectNoEvents()

                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(listOf(trackA1), controller.playedTracks)
            assertTrue(controller.volumeCalls.contains(0.2f))
            assertTrue(controller.volumeCalls.contains(1f))
        }

    @Test
    fun `togglePlayPause pauses in place and resumes the same track`() =
        runTest(UnconfinedTestDispatcher()) {
            val trackA1 = MusicTrack("/music/systems/a/1.mp3", "1")
            val library =
                FakeMusicLibraryRepository(
                    poolsByRequestedSystem = mapOf("a" to MusicPoolContents(MusicPool.PerSystem("a"), listOf(trackA1))),
                    generalPool = MusicPoolContents(MusicPool.General, emptyList()),
                )
            val controller = FakeMusicPlayerController()
            val events = MutableSharedFlow<EsdeEvent>()
            val coordinator = buildCoordinator(events, library, controller, scope = backgroundScope)

            coordinator.playbackState.test {
                awaitItem() // Stopped

                events.emit(EsdeEvent.SystemSelect("a", "System A", "/roms/a"))
                assertEquals(MusicPlaybackState.Playing(trackA1, MusicPool.PerSystem("a")), awaitItem())

                coordinator.togglePlayPause()
                assertEquals(MusicPlaybackState.Paused(trackA1, MusicPool.PerSystem("a")), awaitItem())

                coordinator.togglePlayPause()
                assertEquals(MusicPlaybackState.Playing(trackA1, MusicPool.PerSystem("a")), awaitItem())

                cancelAndIgnoreRemainingEvents()
            }

            // Toggling play/pause never reloads or re-selects a track.
            assertEquals(listOf(trackA1), controller.playedTracks)
        }

    @Test
    fun `next skips to a different track in a multi-track pool`() =
        runTest(UnconfinedTestDispatcher()) {
            val trackA1 = MusicTrack("/music/systems/a/1.mp3", "1")
            val trackA2 = MusicTrack("/music/systems/a/2.mp3", "2")
            val library =
                FakeMusicLibraryRepository(
                    poolsByRequestedSystem = mapOf("a" to MusicPoolContents(MusicPool.PerSystem("a"), listOf(trackA1, trackA2))),
                    generalPool = MusicPoolContents(MusicPool.General, emptyList()),
                )
            val controller = FakeMusicPlayerController()
            val events = MutableSharedFlow<EsdeEvent>()
            val coordinator = buildCoordinator(events, library, controller, scope = backgroundScope)

            coordinator.playbackState.test {
                awaitItem() // Stopped
                events.emit(EsdeEvent.SystemSelect("a", "System A", "/roms/a"))
                awaitItem() // Playing the first track

                coordinator.next()
                val afterSkip = awaitItem()
                check(afterSkip is MusicPlaybackState.Playing)

                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(2, controller.playedTracks.size)
            assertNotEquals(controller.playedTracks[0], controller.playedTracks[1])
        }

    @Test
    fun `track completion advances to a different track in a multi-track pool`() =
        runTest(UnconfinedTestDispatcher()) {
            val trackA1 = MusicTrack("/music/systems/a/1.mp3", "1")
            val trackA2 = MusicTrack("/music/systems/a/2.mp3", "2")
            val library =
                FakeMusicLibraryRepository(
                    poolsByRequestedSystem = mapOf("a" to MusicPoolContents(MusicPool.PerSystem("a"), listOf(trackA1, trackA2))),
                    generalPool = MusicPoolContents(MusicPool.General, emptyList()),
                )
            val controller = FakeMusicPlayerController()
            val events = MutableSharedFlow<EsdeEvent>()
            val coordinator = buildCoordinator(events, library, controller, scope = backgroundScope)

            coordinator.playbackState.test {
                awaitItem() // Stopped
                events.emit(EsdeEvent.SystemSelect("a", "System A", "/roms/a"))
                awaitItem() // Playing the first track

                controller.completeCurrentTrack()
                val afterCompletion = awaitItem()
                check(afterCompletion is MusicPlaybackState.Playing)

                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(2, controller.playedTracks.size)
            assertNotEquals(controller.playedTracks[0], controller.playedTracks[1])
        }

    @Test
    fun `onTrackStarted fires every time a track is handed to the player`() =
        runTest(UnconfinedTestDispatcher()) {
            val trackA1 = MusicTrack("/music/systems/a/1.mp3", "1")
            val trackA2 = MusicTrack("/music/systems/a/2.mp3", "2")
            val library =
                FakeMusicLibraryRepository(
                    poolsByRequestedSystem = mapOf("a" to MusicPoolContents(MusicPool.PerSystem("a"), listOf(trackA1, trackA2))),
                    generalPool = MusicPoolContents(MusicPool.General, emptyList()),
                )
            val controller = FakeMusicPlayerController()
            val events = MutableSharedFlow<EsdeEvent>()
            val startedTracks = mutableListOf<MusicTrack>()
            val coordinator =
                buildCoordinator(
                    events,
                    library,
                    controller,
                    scope = backgroundScope,
                    onTrackStarted = { startedTracks += it },
                )

            coordinator.playbackState.test {
                awaitItem() // Stopped
                events.emit(EsdeEvent.SystemSelect("a", "System A", "/roms/a"))
                awaitItem()

                controller.completeCurrentTrack()
                awaitItem()

                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(controller.playedTracks, startedTracks)
        }

    @Test
    fun `onPlaybackError fires with the current track when the player reports an error`() =
        runTest(UnconfinedTestDispatcher()) {
            val trackA1 = MusicTrack("/music/systems/a/1.mp3", "1")
            val library =
                FakeMusicLibraryRepository(
                    poolsByRequestedSystem = mapOf("a" to MusicPoolContents(MusicPool.PerSystem("a"), listOf(trackA1))),
                    generalPool = MusicPoolContents(MusicPool.General, emptyList()),
                )
            val controller = FakeMusicPlayerController()
            val events = MutableSharedFlow<EsdeEvent>()
            val errors = mutableListOf<Pair<MusicTrack?, String>>()
            val coordinator =
                buildCoordinator(
                    events,
                    library,
                    controller,
                    scope = backgroundScope,
                    onPlaybackError = { track, message -> errors += track to message },
                )

            coordinator.playbackState.test {
                awaitItem() // Stopped
                events.emit(EsdeEvent.SystemSelect("a", "System A", "/roms/a"))
                awaitItem()

                controller.emitError("unreadable file")

                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(listOf(trackA1 to "unreadable file"), errors)
        }
}

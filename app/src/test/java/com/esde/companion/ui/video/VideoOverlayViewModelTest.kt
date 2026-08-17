package com.esde.companion.ui.video

import com.esde.companion.domain.model.EsdeEvent
import com.esde.companion.domain.model.FabAssignments
import com.esde.companion.domain.model.GameMedia
import com.esde.companion.domain.model.LogFolderValidation
import com.esde.companion.domain.model.MediaFolderValidation
import com.esde.companion.domain.model.MediaType
import com.esde.companion.domain.model.MusicDuckingMode
import com.esde.companion.domain.model.ScreenBehavior
import com.esde.companion.domain.model.ThemePreference
import com.esde.companion.domain.repository.EsdeLogRepository
import com.esde.companion.domain.repository.GameMediaRepository
import com.esde.companion.domain.repository.OnboardingRepository
import com.esde.companion.domain.usecase.ObserveAppStateUseCase
import com.esde.companion.domain.usecase.ObserveConnectionStateUseCase
import com.esde.companion.domain.usecase.ObserveVideoAudioEnabledUseCase
import com.esde.companion.domain.usecase.ObserveVideoDelaySecondsUseCase
import com.esde.companion.domain.usecase.ObserveVideoPlaybackEnabledUseCase
import com.esde.companion.domain.usecase.ResolveGameMediaUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class VideoOverlayViewModelTest {
    // --- Fakes -------------------------------------------------------------------------

    private class FakeEsdeLogRepository : EsdeLogRepository {
        val events = MutableSharedFlow<EsdeEvent>()

        override fun observeEvents(): Flow<EsdeEvent> = events

        override fun observeLogFileExists(): Flow<Boolean> = flowOf(true)
    }

    private class RecordingGameMediaRepository(
        private val videoPathForRom: (String) -> String? = { "$it.mp4" },
    ) : GameMediaRepository {
        var callCount = 0
            private set

        override suspend fun resolveMedia(
            systemShortName: String,
            systemPath: String?,
            romPath: String,
            mediaTypes: Set<MediaType>,
        ): GameMedia {
            callCount++
            val path = videoPathForRom(romPath)
            val filesByType = path?.let { mapOf(MediaType.Videos to it) } ?: emptyMap()
            return GameMedia(baseRelativePath = null, filesByType = filesByType)
        }
    }

    /** Only the video-related settings are meaningfully backed; every other member of this
     * large interface is a fixed stub, same convention as SetFabAssignmentUseCaseTest's fake. */
    @Suppress("EmptyFunctionBlock", "TooManyFunctions")
    private class FakeOnboardingRepository(
        initialPlaybackEnabled: Boolean = true,
        initialDelaySeconds: Int = 0,
        initialAudioEnabled: Boolean = true,
    ) : OnboardingRepository {
        val playbackEnabled = MutableStateFlow(initialPlaybackEnabled)
        val delaySeconds = MutableStateFlow(initialDelaySeconds)
        val audioEnabled = MutableStateFlow(initialAudioEnabled)

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

        override fun observeOnboardingComplete(): Flow<Boolean> = flowOf(false)

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

        override suspend fun setVideoPlaybackEnabled(enabled: Boolean) {
            playbackEnabled.value = enabled
        }

        override fun observeVideoPlaybackEnabled(): Flow<Boolean> = playbackEnabled

        override suspend fun setVideoDelaySeconds(seconds: Int) {
            delaySeconds.value = seconds
        }

        override fun observeVideoDelaySeconds(): Flow<Int> = delaySeconds

        override suspend fun setVideoAudioEnabled(enabled: Boolean) {
            audioEnabled.value = enabled
        }

        override fun observeVideoAudioEnabled(): Flow<Boolean> = audioEnabled

        override suspend fun setMusicEnabled(enabled: Boolean) {}

        override fun observeMusicEnabled(): Flow<Boolean> = flowOf(true)

        override suspend fun setMusicPlayWhileBrowsingSystems(enabled: Boolean) {}

        override fun observeMusicPlayWhileBrowsingSystems(): Flow<Boolean> = flowOf(true)

        override suspend fun setMusicPlayWhileBrowsingGames(enabled: Boolean) {}

        override fun observeMusicPlayWhileBrowsingGames(): Flow<Boolean> = flowOf(true)

        override suspend fun setMusicPlayDuringScreensaver(enabled: Boolean) {}

        override fun observeMusicPlayDuringScreensaver(): Flow<Boolean> = flowOf(true)

        override suspend fun setMusicDuckingMode(mode: MusicDuckingMode) {}

        override fun observeMusicDuckingMode(): Flow<MusicDuckingMode> = flowOf(MusicDuckingMode.LowerVolume)

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

        override suspend fun setFabAssignments(assignments: FabAssignments) {}

        override fun observeFabAssignments(): Flow<FabAssignments> = flowOf(FabAssignments.Default)
    }

    // --- Setup ---------------------------------------------------------------------------

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.buildViewModel(
        esdeLogRepository: FakeEsdeLogRepository = FakeEsdeLogRepository(),
        gameMediaRepository: GameMediaRepository = RecordingGameMediaRepository(),
        onboardingRepository: FakeOnboardingRepository = FakeOnboardingRepository(),
    ): VideoOverlayViewModel {
        val observeAppState = ObserveAppStateUseCase(esdeLogRepository, backgroundScope)
        val observeConnectionState = ObserveConnectionStateUseCase(esdeLogRepository, observeAppState)
        val viewModel =
            VideoOverlayViewModel(
                observeConnectionState = observeConnectionState,
                resolveGameMedia = ResolveGameMediaUseCase(gameMediaRepository),
                observeVideoDelaySeconds = ObserveVideoDelaySecondsUseCase(onboardingRepository),
                observeVideoAudioEnabled = ObserveVideoAudioEnabledUseCase(onboardingRepository),
                observeVideoPlaybackEnabled = ObserveVideoPlaybackEnabledUseCase(onboardingRepository),
            )
        // videoPath/delaySeconds/audioEnabled are all WhileSubscribed - simulate the
        // always-on UI collector for each, same reasoning as WidgetsViewModelTest's
        // canvasState fix.
        backgroundScope.launch { viewModel.videoPath.collect {} }
        backgroundScope.launch { viewModel.delaySeconds.collect {} }
        backgroundScope.launch { viewModel.audioEnabled.collect {} }
        advanceUntilIdle()
        return viewModel
    }

    // --- gating on exact BrowsingGame state ------------------------------------------------

    @Test
    fun `videoPath is null for every state other than exactly BrowsingGame, even with playback enabled`() =
        runTest(testDispatcher) {
            val esdeLogRepository = FakeEsdeLogRepository()
            val viewModel = buildViewModel(esdeLogRepository)

            // Idle (initial state) - no event needed.
            assertNull(viewModel.videoPath.value)

            esdeLogRepository.events.emit(EsdeEvent.SystemSelect("snes", "SNES", "/roms/snes"))
            advanceUntilIdle()
            assertNull(viewModel.videoPath.value)

            esdeLogRepository.events.emit(EsdeEvent.GameStart("/roms/snes/a.sfc", "A", "snes", "SNES"))
            advanceUntilIdle()
            assertNull(viewModel.videoPath.value)

            esdeLogRepository.events.emit(EsdeEvent.ScreensaverStart("timer"))
            esdeLogRepository.events.emit(EsdeEvent.ScreensaverGameSelect("/roms/snes/a.sfc", "A", "snes", "SNES"))
            advanceUntilIdle()
            assertNull(viewModel.videoPath.value)
        }

    @Test
    fun `videoPath is null while BrowsingGame if playback is disabled, even when a video file exists`() =
        runTest(testDispatcher) {
            val esdeLogRepository = FakeEsdeLogRepository()
            val onboardingRepository = FakeOnboardingRepository(initialPlaybackEnabled = false)
            val viewModel = buildViewModel(esdeLogRepository, onboardingRepository = onboardingRepository)

            esdeLogRepository.events.emit(EsdeEvent.GameSelect("/roms/snes/a.sfc", "A", "snes", "SNES"))
            advanceUntilIdle()

            assertNull(viewModel.videoPath.value)
        }

    @Test
    fun `videoPath resolves when BrowsingGame and playback are both true`() =
        runTest(testDispatcher) {
            val esdeLogRepository = FakeEsdeLogRepository()
            val viewModel = buildViewModel(esdeLogRepository)

            esdeLogRepository.events.emit(EsdeEvent.GameSelect("/roms/snes/a.sfc", "A", "snes", "SNES"))
            advanceUntilIdle()

            assertEquals("/roms/snes/a.sfc.mp4", viewModel.videoPath.value)
        }

    // --- distinctUntilChanged on the game reference ----------------------------------------

    @Test
    fun `a repeated identical BrowsingGame transition does not re-trigger media resolution`() =
        runTest(testDispatcher) {
            val esdeLogRepository = FakeEsdeLogRepository()
            val gameMediaRepository = RecordingGameMediaRepository()
            val viewModel = buildViewModel(esdeLogRepository, gameMediaRepository = gameMediaRepository)

            val gameSelect = EsdeEvent.GameSelect("/roms/snes/a.sfc", "A", "snes", "SNES")
            esdeLogRepository.events.emit(gameSelect)
            advanceUntilIdle()
            assertEquals(1, gameMediaRepository.callCount)

            esdeLogRepository.events.emit(gameSelect)
            advanceUntilIdle()

            assertEquals(1, gameMediaRepository.callCount)
        }

    // --- toggling playback-enabled independent of AppState ---------------------------------

    @Test
    fun `disabling playback while a video is showing flips videoPath to null without an AppState change`() =
        runTest(testDispatcher) {
            val esdeLogRepository = FakeEsdeLogRepository()
            val onboardingRepository = FakeOnboardingRepository(initialPlaybackEnabled = true)
            val viewModel = buildViewModel(esdeLogRepository, onboardingRepository = onboardingRepository)

            esdeLogRepository.events.emit(EsdeEvent.GameSelect("/roms/snes/a.sfc", "A", "snes", "SNES"))
            advanceUntilIdle()
            assertEquals("/roms/snes/a.sfc.mp4", viewModel.videoPath.value)

            onboardingRepository.playbackEnabled.value = false
            advanceUntilIdle()

            assertNull(viewModel.videoPath.value)
        }

    // --- delaySeconds/audioEnabled pass-throughs --------------------------------------------

    @Test
    fun `delaySeconds and audioEnabled reflect the underlying settings`() =
        runTest(testDispatcher) {
            val onboardingRepository = FakeOnboardingRepository(initialDelaySeconds = 7, initialAudioEnabled = false)
            val viewModel = buildViewModel(onboardingRepository = onboardingRepository)

            assertEquals(7, viewModel.delaySeconds.value)
            assertEquals(false, viewModel.audioEnabled.value)
        }
}

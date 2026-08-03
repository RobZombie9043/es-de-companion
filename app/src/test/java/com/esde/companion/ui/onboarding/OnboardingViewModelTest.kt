package com.esde.companion.ui.onboarding

import com.esde.companion.domain.model.EsdeEvent
import com.esde.companion.domain.model.EsdeEventScriptSettings
import com.esde.companion.domain.model.ImageTransitionMode
import com.esde.companion.domain.model.LogFolderValidation
import com.esde.companion.domain.model.LogoTransitionMode
import com.esde.companion.domain.model.MediaFolderValidation
import com.esde.companion.domain.model.MusicDuckingMode
import com.esde.companion.domain.model.ScreenBehavior
import com.esde.companion.domain.model.ThemePreference
import com.esde.companion.domain.repository.EsdeInstallationRepository
import com.esde.companion.domain.repository.EsdeLogRepository
import com.esde.companion.domain.repository.OnboardingRepository
import com.esde.companion.domain.usecase.CompleteOnboardingUseCase
import com.esde.companion.domain.usecase.DeleteLegacyScriptFilesUseCase
import com.esde.companion.domain.usecase.FindLegacyScriptFilesUseCase
import com.esde.companion.domain.usecase.ObserveAppStateUseCase
import com.esde.companion.domain.usecase.ObserveConnectionStateUseCase
import com.esde.companion.domain.usecase.ObserveEsdeEventScriptSettingsUseCase
import com.esde.companion.domain.usecase.ReadEsdeEventScriptSettingsUseCase
import com.esde.companion.domain.usecase.ReadEsdeMediaDirectoryUseCase
import com.esde.companion.domain.usecase.ValidateEsdeLogFolderUseCase
import com.esde.companion.domain.usecase.ValidateEsdeMediaFolderUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Note: AllFilesAccessPermission.isGranted() (called from OnboardingViewModel's init
 * block) always evaluates true in a plain JVM unit test - Build.VERSION.SDK_INT defaults
 * to 0, which is always < Build.VERSION_CODES.R. So every ViewModel built here starts
 * straight on EsdeFolder, same as a real device that already granted the permission -
 * the Permission step itself isn't exercised by these tests.
 */
class OnboardingViewModelTest {

    private class FakeOnboardingRepository : OnboardingRepository {
        val savedLogPaths = mutableListOf<String>()
        val savedMediaPaths = mutableListOf<String>()
        var markedComplete = false
        var logFolderValidationResult: LogFolderValidation = LogFolderValidation.FolderFound(settingsFileFound = true)
        var mediaFolderValidationResult: MediaFolderValidation = MediaFolderValidation.FolderFound

        override fun defaultLogFolderPath() = "/storage/emulated/0/ES-DE"
        override fun defaultMediaFolderPath() = "/storage/emulated/0/ES-DE/downloaded_media"
        override suspend fun validateLogFolder(path: String) = logFolderValidationResult
        override suspend fun validateMediaFolder(path: String) = mediaFolderValidationResult
        override suspend fun saveLogFolderPath(path: String) { savedLogPaths += path }
        override suspend fun saveMediaFolderPath(path: String) { savedMediaPaths += path }
        override fun observeLogFolderPath(): Flow<String?> = flowOf(null)
        override fun observeMediaFolderPath(): Flow<String?> = flowOf(null)
        override suspend fun saveCustomSystemImagesFolderPath(path: String) {}
        override fun observeCustomSystemImagesFolderPath(): Flow<String?> = flowOf(null)
        override suspend fun clearCustomSystemImagesFolderPath() {}
        override suspend fun saveCustomLogosFolderPath(path: String) {}
        override fun observeCustomLogosFolderPath(): Flow<String?> = flowOf(null)
        override suspend fun clearCustomLogosFolderPath() {}
        override suspend fun markOnboardingComplete() { markedComplete = true }
        override fun observeOnboardingComplete(): Flow<Boolean> = flowOf(markedComplete)
        override suspend fun setThemePreference(preference: ThemePreference) {}
        override fun observeThemePreference(): Flow<ThemePreference> = flowOf(ThemePreference.Auto)
        override suspend fun setGamePlayingBehavior(behavior: ScreenBehavior) {}
        override fun observeGamePlayingBehavior(): Flow<ScreenBehavior> = flowOf(ScreenBehavior.Nothing)
        override suspend fun setScreensaverBehavior(behavior: ScreenBehavior) {}
        override fun observeScreensaverBehavior(): Flow<ScreenBehavior> = flowOf(ScreenBehavior.Nothing)
        override suspend fun setVideoPlaybackEnabled(enabled: Boolean) {}
        override fun observeVideoPlaybackEnabled(): Flow<Boolean> = flowOf(false)
        override suspend fun setVideoDelaySeconds(seconds: Int) {}
        override fun observeVideoDelaySeconds(): Flow<Int> = flowOf(0)
        override suspend fun setVideoAudioEnabled(enabled: Boolean) {}
        override fun observeVideoAudioEnabled(): Flow<Boolean> = flowOf(true)
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
        override suspend fun setMusicOverlayOpacityPercent(percent: Int) {}
        override fun observeMusicOverlayOpacityPercent(): Flow<Int> = flowOf(100)
        override suspend fun saveCustomMusicFolderPath(path: String) {}
        override fun observeCustomMusicFolderPath(): Flow<String?> = flowOf(null)
        override suspend fun clearCustomMusicFolderPath() {}
        override suspend fun setImageTransitionMode(mode: ImageTransitionMode) {}
        override fun observeImageTransitionMode(): Flow<ImageTransitionMode> = flowOf(ImageTransitionMode.None)
        override suspend fun setLogoTransitionMode(mode: LogoTransitionMode) {}
        override fun observeLogoTransitionMode(): Flow<LogoTransitionMode> = flowOf(LogoTransitionMode.None)
    }

    private class FakeEsdeInstallationRepository : EsdeInstallationRepository {
        var mediaDirectory: String? = null
        var eventScriptSettings: EsdeEventScriptSettings? = EsdeEventScriptSettings(
            customEventScripts = true,
            customEventScriptsBrowsing = true,
            debugMode = true,
        )
        var legacyScriptFiles: List<String> = emptyList()
        var deleteCalled = false

        // Emits eventScriptSettings' value at subscription time, then replays whatever's
        // pushed via pushEventScriptSettingsUpdate - mirrors the real repository re-reading
        // the file on subscribe, then again each time it changes on disk.
        val eventScriptSettingsUpdates = MutableSharedFlow<EsdeEventScriptSettings?>(extraBufferCapacity = 1)

        fun pushEventScriptSettingsUpdate(settings: EsdeEventScriptSettings?) {
            eventScriptSettings = settings
            eventScriptSettingsUpdates.tryEmit(settings)
        }

        override suspend fun readMediaDirectory(esdeRootPath: String): String? = mediaDirectory
        override suspend fun readEventScriptSettings(esdeRootPath: String): EsdeEventScriptSettings? = eventScriptSettings
        override fun observeEventScriptSettings(esdeRootPath: String): Flow<EsdeEventScriptSettings?> = flow {
            emit(eventScriptSettings)
            emitAll(eventScriptSettingsUpdates)
        }
        override suspend fun findLegacyScriptFiles(esdeRootPath: String): List<String> = legacyScriptFiles
        override suspend fun deleteLegacyScriptFiles(esdeRootPath: String) {
            deleteCalled = true
            legacyScriptFiles = emptyList()
        }
    }

    private class FakeEsdeLogRepository : EsdeLogRepository {
        // extraBufferCapacity avoids a fragile suspend-until-rendezvous emit() - the
        // ViewModel's own baseline-diffing logic is what's under test here, not exact
        // collector-subscription timing.
        val events = MutableSharedFlow<EsdeEvent>(extraBufferCapacity = 1)
        val logFileExists = MutableStateFlow(true)
        override fun observeEvents(): Flow<EsdeEvent> = events
        override fun observeLogFileExists(): Flow<Boolean> = logFileExists
    }

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
        onboardingRepository: FakeOnboardingRepository = FakeOnboardingRepository(),
        installationRepository: FakeEsdeInstallationRepository = FakeEsdeInstallationRepository(),
        logRepository: FakeEsdeLogRepository = FakeEsdeLogRepository(),
    ): OnboardingViewModel {
        val observeAppState = ObserveAppStateUseCase(logRepository, backgroundScope)
        val observeConnectionState = ObserveConnectionStateUseCase(logRepository, observeAppState)
        return OnboardingViewModel(
            onboardingRepository = onboardingRepository,
            validateLogFolderUseCase = ValidateEsdeLogFolderUseCase(onboardingRepository),
            validateMediaFolderUseCase = ValidateEsdeMediaFolderUseCase(onboardingRepository),
            completeOnboardingUseCase = CompleteOnboardingUseCase(onboardingRepository),
            readEsdeMediaDirectoryUseCase = ReadEsdeMediaDirectoryUseCase(installationRepository),
            readEsdeEventScriptSettingsUseCase = ReadEsdeEventScriptSettingsUseCase(installationRepository),
            observeEsdeEventScriptSettingsUseCase = ObserveEsdeEventScriptSettingsUseCase(installationRepository),
            findLegacyScriptFilesUseCase = FindLegacyScriptFilesUseCase(installationRepository),
            deleteLegacyScriptFilesUseCase = DeleteLegacyScriptFilesUseCase(installationRepository),
            observeConnectionStateUseCase = observeConnectionState,
        )
    }

    @Test
    fun `starts on EsdeFolder and confirming it saves the path`() = runTest(testDispatcher) {
        val onboardingRepo = FakeOnboardingRepository()
        val viewModel = buildViewModel(onboardingRepository = onboardingRepo)
        advanceUntilIdle()

        assertEquals(OnboardingStep.EsdeFolder, viewModel.uiState.value.step)

        viewModel.onEsdeFolderConfirmed()
        advanceUntilIdle()

        assertEquals(OnboardingStep.MediaFolder, viewModel.uiState.value.step)
        assertEquals(listOf(onboardingRepo.defaultLogFolderPath()), onboardingRepo.savedLogPaths)
    }

    @Test
    fun `media folder confirmed by ES-DE's own settings skips the MediaFolder step entirely`() = runTest(testDispatcher) {
        val onboardingRepo = FakeOnboardingRepository().apply {
            mediaFolderValidationResult = MediaFolderValidation.FolderFound
        }
        val installationRepo = FakeEsdeInstallationRepository().apply {
            mediaDirectory = "/storage/emulated/0/ES-DE/downloaded_media"
        }
        val viewModel = buildViewModel(onboardingRepository = onboardingRepo, installationRepository = installationRepo)
        advanceUntilIdle()

        viewModel.onEsdeFolderConfirmed()
        advanceUntilIdle()

        assertEquals(OnboardingStep.LiveLogCheck, viewModel.uiState.value.step)
        assertEquals(listOf(installationRepo.mediaDirectory), onboardingRepo.savedMediaPaths)
        assertTrue(viewModel.uiState.value.mediaFolderAutoDetected)
    }

    @Test
    fun `media folder named in settings but missing on disk still shows MediaFolder for manual pick`() = runTest(testDispatcher) {
        val onboardingRepo = FakeOnboardingRepository().apply {
            mediaFolderValidationResult = MediaFolderValidation.FolderNotFound
        }
        val installationRepo = FakeEsdeInstallationRepository().apply {
            mediaDirectory = "/storage/emulated/0/ES-DE/downloaded_media"
        }
        val viewModel = buildViewModel(onboardingRepository = onboardingRepo, installationRepository = installationRepo)
        advanceUntilIdle()

        viewModel.onEsdeFolderConfirmed()
        advanceUntilIdle()

        assertEquals(OnboardingStep.MediaFolder, viewModel.uiState.value.step)
        assertTrue(onboardingRepo.savedMediaPaths.isEmpty())
        assertEquals(MediaFolderValidation.FolderNotFound, viewModel.uiState.value.mediaFolderValidation)
    }

    @Test
    fun `when nothing needs fixing, MediaFolder confirm skips straight to LiveLogCheck`() = runTest(testDispatcher) {
        val installationRepo = FakeEsdeInstallationRepository()
        val viewModel = buildViewModel(installationRepository = installationRepo)
        advanceUntilIdle()
        viewModel.onEsdeFolderConfirmed()
        advanceUntilIdle()

        viewModel.onMediaFolderConfirmed()
        advanceUntilIdle()

        assertEquals(OnboardingStep.LiveLogCheck, viewModel.uiState.value.step)
    }

    @Test
    fun `when legacy scripts are found, MediaFolder confirm shows LegacyScripts, and deleting clears them`() = runTest(testDispatcher) {
        val installationRepo = FakeEsdeInstallationRepository().apply {
            legacyScriptFiles = listOf("/roms/ES-DE/scripts/game-end/esdecompanion-game-end.sh")
        }
        val viewModel = buildViewModel(installationRepository = installationRepo)
        advanceUntilIdle()
        viewModel.onEsdeFolderConfirmed()
        advanceUntilIdle()
        viewModel.onMediaFolderConfirmed()
        advanceUntilIdle()

        assertEquals(OnboardingStep.LegacyScripts, viewModel.uiState.value.step)
        assertEquals(1, viewModel.uiState.value.legacyScriptFiles.size)

        viewModel.onDeleteLegacyScriptFiles()
        advanceUntilIdle()

        assertTrue(installationRepo.deleteCalled)
        assertTrue(viewModel.uiState.value.legacyScriptFiles.isEmpty())

        viewModel.onLegacyScriptsConfirmed()
        advanceUntilIdle()

        assertEquals(OnboardingStep.LiveLogCheck, viewModel.uiState.value.step)
    }

    @Test
    fun `when an ES-DE setting is disabled, MediaFolder confirm shows EventScriptSettings`() = runTest(testDispatcher) {
        val installationRepo = FakeEsdeInstallationRepository().apply {
            eventScriptSettings = EsdeEventScriptSettings(
                customEventScripts = false,
                customEventScriptsBrowsing = true,
                debugMode = true,
            )
        }
        val viewModel = buildViewModel(installationRepository = installationRepo)
        advanceUntilIdle()
        viewModel.onEsdeFolderConfirmed()
        advanceUntilIdle()

        viewModel.onMediaFolderConfirmed()
        advanceUntilIdle()

        assertEquals(OnboardingStep.EventScriptSettings, viewModel.uiState.value.step)

        viewModel.onEventScriptSettingsConfirmed()
        advanceUntilIdle()

        assertEquals(OnboardingStep.LiveLogCheck, viewModel.uiState.value.step)
    }

    @Test
    fun `EventScriptSettings updates as es_settings xml changes are detected, without needing a manual re-check`() =
        runTest(testDispatcher) {
            val installationRepo = FakeEsdeInstallationRepository().apply {
                eventScriptSettings = EsdeEventScriptSettings(
                    customEventScripts = false,
                    customEventScriptsBrowsing = true,
                    debugMode = true,
                )
            }
            val viewModel = buildViewModel(installationRepository = installationRepo)
            advanceUntilIdle()
            viewModel.onEsdeFolderConfirmed()
            advanceUntilIdle()
            viewModel.onMediaFolderConfirmed()
            advanceUntilIdle()

            assertEquals(OnboardingStep.EventScriptSettings, viewModel.uiState.value.step)
            assertFalse(viewModel.uiState.value.eventScriptSettings?.allEnabled == true)

            // Simulates ES-DE rewriting es_settings.xml once the user navigates back out of
            // its settings menu, with the fix applied - no explicit re-check call from the
            // ViewModel is involved, only the file-change signal.
            installationRepo.pushEventScriptSettingsUpdate(
                EsdeEventScriptSettings(
                    customEventScripts = true,
                    customEventScriptsBrowsing = true,
                    debugMode = true,
                ),
            )
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.eventScriptSettings?.allEnabled == true)
            assertEquals(OnboardingStep.EventScriptSettings, viewModel.uiState.value.step)

            viewModel.onEventScriptSettingsConfirmed()
            advanceUntilIdle()

            assertEquals(OnboardingStep.LiveLogCheck, viewModel.uiState.value.step)
        }

    @Test
    fun `back navigation skips over steps that were skipped going forward`() = runTest(testDispatcher) {
        val installationRepo = FakeEsdeInstallationRepository()
        val viewModel = buildViewModel(installationRepository = installationRepo)
        advanceUntilIdle()
        viewModel.onEsdeFolderConfirmed()
        advanceUntilIdle()
        viewModel.onMediaFolderConfirmed()
        advanceUntilIdle()
        assertEquals(OnboardingStep.LiveLogCheck, viewModel.uiState.value.step)

        viewModel.onBack()

        assertEquals(OnboardingStep.MediaFolder, viewModel.uiState.value.step)
    }

    @Test
    fun `Finish stays disabled until a connection state emission differs from the entry baseline`() {
        // Unconfined (not the class-level StandardTestDispatcher) - matches
        // ObserveConnectionStateUseCaseTest's established pattern for this exact
        // combine()+SharedFlow-emit shape, where a queued/deferred dispatcher can
        // conflate the baseline and post-emit combine() outputs before either is
        // separately observable. Set for both Main (viewModelScope) and runTest's own
        // scope so everything shares one scheduler.
        val dispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(dispatcher)
        runTest(dispatcher) {
            val onboardingRepo = FakeOnboardingRepository()
            val logRepo = FakeEsdeLogRepository()
            val viewModel = buildViewModel(onboardingRepository = onboardingRepo, logRepository = logRepo)
            advanceUntilIdle()
            viewModel.onEsdeFolderConfirmed()
            advanceUntilIdle()
            viewModel.onMediaFolderConfirmed()
            advanceUntilIdle()
            assertEquals(OnboardingStep.LiveLogCheck, viewModel.uiState.value.step)
            assertFalse(viewModel.uiState.value.liveCheckPassed)

            logRepo.events.emit(EsdeEvent.SystemSelect("dreamcast", "Sega Dreamcast", "/roms/dreamcast"))
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.liveCheckPassed)

            viewModel.onFinishSetup()
            advanceUntilIdle()

            assertTrue(onboardingRepo.markedComplete)
        }
    }
}


package com.esde.companion.domain.usecase

import app.cash.turbine.test
import com.esde.companion.domain.model.AppState
import com.esde.companion.domain.model.EsdeEvent
import com.esde.companion.domain.model.FabAssignments
import com.esde.companion.domain.model.LogFolderValidation
import com.esde.companion.domain.model.MediaFolderValidation
import com.esde.companion.domain.model.MusicDuckingMode
import com.esde.companion.domain.model.ScreenBehavior
import com.esde.companion.domain.model.ThemePreference
import com.esde.companion.domain.repository.EsdeLogRepository
import com.esde.companion.domain.repository.OnboardingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveScreensaverAwareContextUseCaseTest {
    private class FakeEsdeLogRepository(
        private val events: Flow<EsdeEvent>,
    ) : EsdeLogRepository {
        override fun observeEvents(): Flow<EsdeEvent> = events

        override fun observeLogFileExists(): Flow<Boolean> = flowOf(true)
    }

    @Suppress("EmptyFunctionBlock")
    private class FakeOnboardingRepository : OnboardingRepository {
        val updateOnScreensaver = MutableStateFlow(true)

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

        override suspend fun setUpdateAchievementsOnScreensaverEnabled(enabled: Boolean) {
            updateOnScreensaver.value = enabled
        }

        override fun observeUpdateAchievementsOnScreensaverEnabled(): Flow<Boolean> = updateOnScreensaver
    }

    private fun buildUseCase(
        events: Flow<EsdeEvent>,
        onboardingRepository: FakeOnboardingRepository,
        scope: CoroutineScope,
        resolve: (AppState?, String?, Boolean) -> String?,
    ): ObserveScreensaverAwareContextUseCase<String> {
        val logRepository = FakeEsdeLogRepository(events)
        val observeAppState = ObserveAppStateUseCase(logRepository, scope)
        val observeConnectionState = ObserveConnectionStateUseCase(logRepository, observeAppState)
        val observeUpdateOnScreensaver = ObserveUpdateAchievementsOnScreensaverEnabledUseCase(onboardingRepository)
        return ObserveScreensaverAwareContextUseCase(observeConnectionState, observeUpdateOnScreensaver, resolve)
    }

    /** Trivial resolver independent of resolveAchievementsGame/resolveAchievementsSystem's own logic (see
     * RetroAchievementsScreensaverFreezeTest) - just enough to exercise this class's own combine/scan wiring. */
    private val holdPreviousDuringScreensaverWhenDisabled: (AppState?, String?, Boolean) -> String? =
        { appState, previous, updateOnScreensaver ->
            when {
                appState == null -> null
                appState is AppState.Screensaver && !updateOnScreensaver -> previous
                else -> appState.toString()
            }
        }

    @Test
    fun `resolves the current AppState by default`() =
        runTest(UnconfinedTestDispatcher()) {
            val events = MutableSharedFlow<EsdeEvent>()
            val onboardingRepository = FakeOnboardingRepository()
            val useCase =
                buildUseCase(events, onboardingRepository, backgroundScope, holdPreviousDuringScreensaverWhenDisabled)

            useCase().test {
                assertEquals(null, awaitItem())
                assertEquals(AppState.Idle.toString(), awaitItem())

                events.emit(EsdeEvent.SystemSelect("snes", "Super Nintendo", "/roms/snes"))
                assertEquals(AppState.BrowsingSystem("snes", "Super Nintendo", "/roms/snes").toString(), awaitItem())
            }
        }

    @Test
    fun `holds the previous value during a screensaver when the toggle is off`() =
        runTest(UnconfinedTestDispatcher()) {
            val events = MutableSharedFlow<EsdeEvent>()
            val onboardingRepository = FakeOnboardingRepository().apply { updateOnScreensaver.value = false }
            val useCase =
                buildUseCase(events, onboardingRepository, backgroundScope, holdPreviousDuringScreensaverWhenDisabled)

            useCase().test {
                assertEquals(null, awaitItem())
                assertEquals(AppState.Idle.toString(), awaitItem())

                events.emit(EsdeEvent.SystemSelect("snes", "Super Nintendo", "/roms/snes"))
                val browsing = awaitItem()
                assertEquals(AppState.BrowsingSystem("snes", "Super Nintendo", "/roms/snes").toString(), browsing)

                events.emit(EsdeEvent.ScreensaverStart("timer"))
                assertEquals(browsing, awaitItem())
            }
        }

    @Test
    fun `switches to the screensaver's own state when the toggle is on`() =
        runTest(UnconfinedTestDispatcher()) {
            val events = MutableSharedFlow<EsdeEvent>()
            val onboardingRepository = FakeOnboardingRepository()
            val useCase =
                buildUseCase(events, onboardingRepository, backgroundScope, holdPreviousDuringScreensaverWhenDisabled)

            useCase().test {
                assertEquals(null, awaitItem())
                awaitItem() // Idle, before any event is emitted

                events.emit(EsdeEvent.SystemSelect("snes", "Super Nintendo", "/roms/snes"))
                awaitItem()

                events.emit(EsdeEvent.ScreensaverStart("timer"))
                val screensaverState = awaitItem()
                assertEquals(true, screensaverState?.contains("Screensaver"))
            }
        }
}

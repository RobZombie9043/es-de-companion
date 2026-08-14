package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.FabAssignments
import com.esde.companion.domain.model.FabPosition
import com.esde.companion.domain.model.FabSlot
import com.esde.companion.domain.model.FabType
import com.esde.companion.domain.model.LogFolderValidation
import com.esde.companion.domain.model.MediaFolderValidation
import com.esde.companion.domain.model.MusicDuckingMode
import com.esde.companion.domain.model.ScreenBehavior
import com.esde.companion.domain.model.ThemePreference
import com.esde.companion.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SetFabAssignmentUseCaseTest {
    // Only fabAssignments-related members matter here - the rest of this large interface
    // is stubbed with empty/fixed implementations, same shape as CompleteOnboardingUseCaseTest's fake.
    @Suppress("EmptyFunctionBlock", "TooManyFunctions")
    private class FakeOnboardingRepository(
        initial: FabAssignments = FabAssignments.Default,
    ) : OnboardingRepository {
        private val fabAssignments = MutableStateFlow(initial)
        val persisted = mutableListOf<FabAssignments>()

        override fun defaultLogFolderPath() = "/storage/emulated/0/ES-DE"

        override fun defaultMediaFolderPath() = "/storage/emulated/0/ES-DE/downloaded_media"

        override suspend fun validateLogFolder(path: String) = LogFolderValidation.FolderFound(true)

        override suspend fun validateMediaFolder(path: String) = MediaFolderValidation.FolderFound

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

        override suspend fun setVideoPlaybackEnabled(enabled: Boolean) {}

        override fun observeVideoPlaybackEnabled(): Flow<Boolean> = flowOf(false)

        override suspend fun setVideoDelaySeconds(seconds: Int) {}

        override fun observeVideoDelaySeconds(): Flow<Int> = flowOf(0)

        override suspend fun setVideoAudioEnabled(enabled: Boolean) {}

        override fun observeVideoAudioEnabled(): Flow<Boolean> = flowOf(true)

        override suspend fun setGamePlayingBehavior(behavior: ScreenBehavior) {}

        override fun observeGamePlayingBehavior(): Flow<ScreenBehavior> = flowOf(ScreenBehavior.Nothing)

        override suspend fun setGamePlayingDimPercent(percent: Int) {}

        override fun observeGamePlayingDimPercent(): Flow<Int> = flowOf(50)

        override suspend fun setScreensaverBehavior(behavior: ScreenBehavior) {}

        override fun observeScreensaverBehavior(): Flow<ScreenBehavior> = flowOf(ScreenBehavior.Nothing)

        override suspend fun setScreensaverDimPercent(percent: Int) {}

        override fun observeScreensaverDimPercent(): Flow<Int> = flowOf(50)

        override suspend fun setThemePreference(preference: ThemePreference) {}

        override fun observeThemePreference(): Flow<ThemePreference> = flowOf(ThemePreference.Auto)

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

        override suspend fun setUpdateAchievementsOnScreensaverEnabled(enabled: Boolean) {}

        override fun observeUpdateAchievementsOnScreensaverEnabled(): Flow<Boolean> = flowOf(true)

        override suspend fun setFabAssignments(assignments: FabAssignments) {
            persisted += assignments
            fabAssignments.value = assignments
        }

        override fun observeFabAssignments(): Flow<FabAssignments> = fabAssignments
    }

    @Test
    fun `assigning an empty corner updates only that corner`() =
        runTest {
            val repository = FakeOnboardingRepository()
            val useCase = SetFabAssignmentUseCase(repository)

            val result = useCase(FabPosition.BottomStart, FabSlot(FabType.GameManual))

            assertEquals(FabAssignments.Default.copy(bottomStart = FabSlot(FabType.GameManual)), result)
        }

    @Test
    fun `assigning Music to a top corner already holding Music swaps the displaced slot into the vacated corner`() =
        runTest {
            // Defaults: topStart = Music, topEnd = Settings.
            val repository = FakeOnboardingRepository()
            val useCase = SetFabAssignmentUseCase(repository)

            val result = useCase(FabPosition.TopEnd, FabSlot(FabType.Music))

            assertEquals(
                FabAssignments.Default.copy(topStart = FabSlot(FabType.Settings), topEnd = FabSlot(FabType.Music)),
                result,
            )
        }

    @Test
    fun `the returned value is exactly what gets persisted`() =
        runTest {
            val repository = FakeOnboardingRepository()
            val useCase = SetFabAssignmentUseCase(repository)

            val result = useCase(FabPosition.TopEnd, FabSlot(FabType.Music))

            assertEquals(listOf(result), repository.persisted)
        }
}

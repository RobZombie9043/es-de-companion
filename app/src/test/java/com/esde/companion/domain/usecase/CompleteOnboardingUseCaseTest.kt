package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.ImageTransitionMode
import com.esde.companion.domain.model.LogFolderValidation
import com.esde.companion.domain.model.LogoTransitionMode
import com.esde.companion.domain.model.MediaFolderValidation
import com.esde.companion.domain.model.MusicDuckingMode
import com.esde.companion.domain.model.ScreenBehavior
import com.esde.companion.domain.model.ThemePreference
import com.esde.companion.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CompleteOnboardingUseCaseTest {

    private class FakeOnboardingRepository : OnboardingRepository {
        val savedLogPaths = mutableListOf<String>()
        val savedMediaPaths = mutableListOf<String>()
        val savedCustomSystemImagesPaths = mutableListOf<String>()
        val savedCustomLogosPaths = mutableListOf<String>()
        var markedComplete = false
        var gamePlayingBehavior = ScreenBehavior.Nothing
        var videoPlaybackEnabled = false
        var videoDelaySeconds = 0
        var videoAudioEnabled = true
        var screensaverBehavior = ScreenBehavior.Nothing
        var themePreference = ThemePreference.Auto
        var musicEnabled = true
        var musicPlayWhileBrowsingSystems = true
        var musicPlayWhileBrowsingGames = true
        var musicPlayDuringScreensaver = true
        var musicDuckingMode = MusicDuckingMode.LowerVolume

        override fun defaultLogFolderPath() = "/storage/emulated/0/ES-DE"
        override fun defaultMediaFolderPath() = "/storage/emulated/0/ES-DE/downloaded_media"
        override suspend fun validateLogFolder(path: String) = LogFolderValidation.FolderFound(true)
        override suspend fun validateMediaFolder(path: String) = MediaFolderValidation.FolderFound
        override suspend fun saveLogFolderPath(path: String) { savedLogPaths += path }
        override suspend fun saveMediaFolderPath(path: String) { savedMediaPaths += path }
        override fun observeLogFolderPath(): Flow<String?> = flowOf(null)
        override fun observeMediaFolderPath(): Flow<String?> = flowOf(null)
        override suspend fun saveCustomSystemImagesFolderPath(path: String) { savedCustomSystemImagesPaths += path }
        override fun observeCustomSystemImagesFolderPath(): Flow<String?> = flowOf(null)
        override suspend fun clearCustomSystemImagesFolderPath() {}
        override suspend fun saveCustomLogosFolderPath(path: String) { savedCustomLogosPaths += path }
        override fun observeCustomLogosFolderPath(): Flow<String?> = flowOf(null)
        override suspend fun clearCustomLogosFolderPath() {}
        override suspend fun markOnboardingComplete() { markedComplete = true }
        override fun observeOnboardingComplete(): Flow<Boolean> = flowOf(markedComplete)
        override suspend fun setVideoPlaybackEnabled(enabled: Boolean) { videoPlaybackEnabled = enabled }
        override fun observeVideoPlaybackEnabled(): Flow<Boolean> = flowOf(videoPlaybackEnabled)
        override suspend fun setVideoDelaySeconds(seconds: Int) { videoDelaySeconds = seconds }
        override fun observeVideoDelaySeconds(): Flow<Int> = flowOf(videoDelaySeconds)
        override suspend fun setVideoAudioEnabled(enabled: Boolean) { videoAudioEnabled = enabled }
        override fun observeVideoAudioEnabled(): Flow<Boolean> = flowOf(videoAudioEnabled)
        override suspend fun setGamePlayingBehavior(behavior: ScreenBehavior) { gamePlayingBehavior = behavior }
        override fun observeGamePlayingBehavior(): Flow<ScreenBehavior> = flowOf(gamePlayingBehavior)
        override suspend fun setScreensaverBehavior(behavior: ScreenBehavior) { screensaverBehavior = behavior }
        override fun observeScreensaverBehavior(): Flow<ScreenBehavior> = flowOf(screensaverBehavior)
        override suspend fun setThemePreference(preference: ThemePreference) { themePreference = preference }
        override fun observeThemePreference(): Flow<ThemePreference> = flowOf(themePreference)
        override suspend fun setMusicEnabled(enabled: Boolean) { musicEnabled = enabled }
        override fun observeMusicEnabled(): Flow<Boolean> = flowOf(musicEnabled)
        override suspend fun setMusicPlayWhileBrowsingSystems(enabled: Boolean) { musicPlayWhileBrowsingSystems = enabled }
        override fun observeMusicPlayWhileBrowsingSystems(): Flow<Boolean> = flowOf(musicPlayWhileBrowsingSystems)
        override suspend fun setMusicPlayWhileBrowsingGames(enabled: Boolean) { musicPlayWhileBrowsingGames = enabled }
        override fun observeMusicPlayWhileBrowsingGames(): Flow<Boolean> = flowOf(musicPlayWhileBrowsingGames)
        override suspend fun setMusicPlayDuringScreensaver(enabled: Boolean) { musicPlayDuringScreensaver = enabled }
        override fun observeMusicPlayDuringScreensaver(): Flow<Boolean> = flowOf(musicPlayDuringScreensaver)
        override suspend fun setMusicDuckingMode(mode: MusicDuckingMode) { musicDuckingMode = mode }
        override fun observeMusicDuckingMode(): Flow<MusicDuckingMode> = flowOf(musicDuckingMode)
        override suspend fun setOverlayOpacityPercent(percent: Int) {}
        override fun observeOverlayOpacityPercent(): Flow<Int> = flowOf(80)
        override suspend fun saveCustomMusicFolderPath(path: String) {}
        override fun observeCustomMusicFolderPath(): Flow<String?> = flowOf(null)
        override suspend fun clearCustomMusicFolderPath() {}
        override suspend fun setImageTransitionMode(mode: ImageTransitionMode) {}
        override fun observeImageTransitionMode(): Flow<ImageTransitionMode> = flowOf(ImageTransitionMode.None)
        override suspend fun setLogoTransitionMode(mode: LogoTransitionMode) {}
        override fun observeLogoTransitionMode(): Flow<LogoTransitionMode> = flowOf(LogoTransitionMode.None)
        override suspend fun setGlintEnabled(enabled: Boolean) {}
        override fun observeGlintEnabled(): Flow<Boolean> = flowOf(false)
    }

    @Test
    fun `saves both paths and marks onboarding complete`() = runTest {
        val repository = FakeOnboardingRepository()
        val useCase = CompleteOnboardingUseCase(repository)

        useCase("/roms-parent/ES-DE", "/sdcard/media")

        assertEquals(listOf("/roms-parent/ES-DE"), repository.savedLogPaths)
        assertEquals(listOf("/sdcard/media"), repository.savedMediaPaths)
        assertEquals(true, repository.markedComplete)
    }
}
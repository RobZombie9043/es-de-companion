package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.LogFolderValidation
import com.esde.companion.domain.model.MediaFolderValidation
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
        var markedComplete = false
        var overlayEnabled = true
        var blankScreenEnabled = false
        var themePreference = ThemePreference.Auto

        override fun defaultLogFolderPath() = "/storage/emulated/0/ES-DE"
        override fun defaultMediaFolderPath() = "/storage/emulated/0/ES-DE/downloaded_media"
        override suspend fun validateLogFolder(path: String) = LogFolderValidation.FolderFound(true)
        override suspend fun validateMediaFolder(path: String) = MediaFolderValidation.FolderFound
        override suspend fun saveLogFolderPath(path: String) { savedLogPaths += path }
        override suspend fun saveMediaFolderPath(path: String) { savedMediaPaths += path }
        override fun observeLogFolderPath(): Flow<String?> = flowOf(null)
        override fun observeMediaFolderPath(): Flow<String?> = flowOf(null)
        override suspend fun markOnboardingComplete() { markedComplete = true }
        override fun observeOnboardingComplete(): Flow<Boolean> = flowOf(markedComplete)
        override suspend fun setOverlayEnabled(enabled: Boolean) { overlayEnabled = enabled }
        override fun observeOverlayEnabled(): Flow<Boolean> = flowOf(overlayEnabled)
        override suspend fun setBlankScreenEnabled(enabled: Boolean) { blankScreenEnabled = enabled }
        override fun observeBlankScreenEnabled(): Flow<Boolean> = flowOf(blankScreenEnabled)
        override suspend fun setThemePreference(preference: ThemePreference) { themePreference = preference }
        override fun observeThemePreference(): Flow<ThemePreference> = flowOf(themePreference)
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
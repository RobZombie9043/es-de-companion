package com.esde.companion.ui.settings

import com.esde.companion.domain.model.LogFolderValidation
import com.esde.companion.domain.model.MediaFolderValidation
import com.esde.companion.domain.model.ThemePreference
import com.esde.companion.domain.repository.AppDrawerSettingsRepository
import com.esde.companion.domain.repository.OnboardingRepository
import com.esde.companion.domain.usecase.ObserveDrawerOpacityUseCase
import com.esde.companion.domain.usecase.ObserveGridColumnsUseCase
import com.esde.companion.domain.usecase.ObserveOverlayEnabledUseCase
import com.esde.companion.domain.usecase.ObserveThemePreferenceUseCase
import com.esde.companion.domain.usecase.SetDrawerOpacityUseCase
import com.esde.companion.domain.usecase.SetGridColumnsUseCase
import com.esde.companion.domain.usecase.SetOverlayEnabledUseCase
import com.esde.companion.domain.usecase.SetThemePreferenceUseCase
import com.esde.companion.domain.usecase.ValidateEsdeLogFolderUseCase
import com.esde.companion.domain.usecase.ValidateEsdeMediaFolderUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SettingsViewModelTest {

    private class FakeOnboardingRepository : OnboardingRepository {
        var overlayEnabled = true
        var themePreference = ThemePreference.Auto

        override fun defaultLogFolderPath() = "/storage/emulated/0/ES-DE"
        override fun defaultMediaFolderPath() = "/storage/emulated/0/ES-DE/downloaded_media"
        override suspend fun validateLogFolder(path: String) = LogFolderValidation.FolderFound(true)
        override suspend fun validateMediaFolder(path: String) = MediaFolderValidation.FolderFound
        override suspend fun saveLogFolderPath(path: String) {}
        override suspend fun saveMediaFolderPath(path: String) {}
        override fun observeLogFolderPath(): Flow<String?> = flowOf(null)
        override fun observeMediaFolderPath(): Flow<String?> = flowOf(null)
        override suspend fun markOnboardingComplete() {}
        override fun observeOnboardingComplete(): Flow<Boolean> = flowOf(false)
        override suspend fun setOverlayEnabled(enabled: Boolean) { overlayEnabled = enabled }
        override fun observeOverlayEnabled(): Flow<Boolean> = flowOf(overlayEnabled)
        override suspend fun setThemePreference(preference: ThemePreference) { themePreference = preference }
        override fun observeThemePreference(): Flow<ThemePreference> = flowOf(themePreference)
    }

    private class FakeAppDrawerSettingsRepository(
        initialOpacity: Int = 80,
        initialColumns: Int = 4,
    ) : AppDrawerSettingsRepository {
        val opacity = MutableStateFlow(initialOpacity)
        val columns = MutableStateFlow(initialColumns)

        override suspend fun setHiddenApps(packageNames: Set<String>) {}
        override fun observeHiddenApps(): Flow<Set<String>> = flowOf(emptySet())
        override suspend fun setDrawerOpacityPercent(percent: Int) { opacity.value = percent }
        override fun observeDrawerOpacityPercent(): Flow<Int> = opacity
        override suspend fun setGridColumns(columns: Int) { this.columns.value = columns }
        override fun observeGridColumns(): Flow<Int> = columns
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

    private fun buildViewModel(
        onboardingRepository: FakeOnboardingRepository = FakeOnboardingRepository(),
        appDrawerSettingsRepository: FakeAppDrawerSettingsRepository = FakeAppDrawerSettingsRepository(),
    ): Pair<SettingsViewModel, FakeAppDrawerSettingsRepository> {
        val viewModel = SettingsViewModel(
            onboardingRepository = onboardingRepository,
            validateLogFolderUseCase = ValidateEsdeLogFolderUseCase(onboardingRepository),
            validateMediaFolderUseCase = ValidateEsdeMediaFolderUseCase(onboardingRepository),
            observeOverlayEnabledUseCase = ObserveOverlayEnabledUseCase(onboardingRepository),
            setOverlayEnabledUseCase = SetOverlayEnabledUseCase(onboardingRepository),
            observeThemePreferenceUseCase = ObserveThemePreferenceUseCase(onboardingRepository),
            setThemePreferenceUseCase = SetThemePreferenceUseCase(onboardingRepository),
            observeDrawerOpacityUseCase = ObserveDrawerOpacityUseCase(appDrawerSettingsRepository),
            setDrawerOpacityUseCase = SetDrawerOpacityUseCase(appDrawerSettingsRepository),
            observeGridColumnsUseCase = ObserveGridColumnsUseCase(appDrawerSettingsRepository),
            setGridColumnsUseCase = SetGridColumnsUseCase(appDrawerSettingsRepository),
        )
        return viewModel to appDrawerSettingsRepository
    }

    @Test
    fun `initial state loads drawer opacity and grid columns from the repository`() = runTest(testDispatcher) {
        val (viewModel, _) = buildViewModel(
            appDrawerSettingsRepository = FakeAppDrawerSettingsRepository(initialOpacity = 70, initialColumns = 5),
        )

        advanceUntilIdle()

        assertEquals(70, viewModel.uiState.value.drawerOpacityPercent)
        assertEquals(5, viewModel.uiState.value.gridColumns)
    }

    @Test
    fun `onDrawerOpacityChanged updates ui state immediately and persists`() = runTest(testDispatcher) {
        val (viewModel, appDrawerSettingsRepository) = buildViewModel()
        advanceUntilIdle()

        viewModel.onDrawerOpacityChanged(85)

        // ui state updates synchronously, before the persistence coroutine runs.
        assertEquals(85, viewModel.uiState.value.drawerOpacityPercent)

        advanceUntilIdle()
        assertEquals(85, appDrawerSettingsRepository.opacity.value)
    }

    @Test
    fun `onGridColumnsChanged updates ui state immediately and persists`() = runTest(testDispatcher) {
        val (viewModel, appDrawerSettingsRepository) = buildViewModel()
        advanceUntilIdle()

        viewModel.onGridColumnsChanged(6)

        assertEquals(6, viewModel.uiState.value.gridColumns)

        advanceUntilIdle()
        assertEquals(6, appDrawerSettingsRepository.columns.value)
    }
}
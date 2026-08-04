package com.esde.companion.ui.settings

import com.esde.companion.domain.model.InstalledApp
import com.esde.companion.domain.repository.AppDrawerSettingsRepository
import com.esde.companion.domain.repository.InstalledAppsRepository
import com.esde.companion.domain.usecase.ObserveHiddenAppsUseCase
import com.esde.companion.domain.usecase.ObserveInstalledAppsUseCase
import com.esde.companion.domain.usecase.SetHiddenAppsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ManageAppsViewModelTest {

    private class FakeInstalledAppsRepository(private val apps: Flow<List<InstalledApp>>) : InstalledAppsRepository {
        override fun observeInstalledApps(): Flow<List<InstalledApp>> = apps
    }

    private class FakeAppDrawerSettingsRepository(
        initialHiddenApps: Set<String> = emptySet(),
    ) : AppDrawerSettingsRepository {
        val hiddenApps = MutableStateFlow(initialHiddenApps)

        override suspend fun setHiddenApps(packageNames: Set<String>) { hiddenApps.value = packageNames }
        override fun observeHiddenApps(): Flow<Set<String>> = hiddenApps
        override suspend fun setGridColumns(columns: Int) { /* not under test */ }
        override fun observeGridColumns(): Flow<Int> = MutableStateFlow(4)
        override suspend fun setOtherScreenLaunchApps(packageNames: Set<String>) { /* not under test */ }
        override fun observeOtherScreenLaunchApps(): Flow<Set<String>> = MutableStateFlow(emptySet())
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

    private val allApps = listOf(
        InstalledApp(packageName = "com.example.a", label = "App A"),
        InstalledApp(packageName = "com.example.b", label = "App B"),
    )

    private fun buildViewModel(
        apps: List<InstalledApp> = allApps,
        settingsRepository: FakeAppDrawerSettingsRepository = FakeAppDrawerSettingsRepository(),
    ): Pair<ManageAppsViewModel, FakeAppDrawerSettingsRepository> {
        val installedAppsRepository = FakeInstalledAppsRepository(flowOf(apps))
        val viewModel = ManageAppsViewModel(
            observeInstalledApps = ObserveInstalledAppsUseCase(installedAppsRepository),
            observeHiddenApps = ObserveHiddenAppsUseCase(settingsRepository),
            setHiddenApps = SetHiddenAppsUseCase(settingsRepository),
        )
        return viewModel to settingsRepository
    }

    @Test
    fun `rows reflect every installed app with its current hidden state`() = runTest(testDispatcher) {
        val (viewModel, _) = buildViewModel(
            settingsRepository = FakeAppDrawerSettingsRepository(initialHiddenApps = setOf("com.example.b")),
        )

        val collectJob = launch { viewModel.rows.collect {} }
        advanceUntilIdle()

        assertEquals(
            listOf(
                AppVisibilityRow(allApps[0], isHidden = false),
                AppVisibilityRow(allApps[1], isHidden = true),
            ),
            viewModel.rows.value,
        )
        collectJob.cancel()
    }

    @Test
    fun `toggling an app to hidden adds it to the hidden set`() = runTest(testDispatcher) {
        val (viewModel, settingsRepository) = buildViewModel()

        val collectJob = launch { viewModel.rows.collect {} }
        advanceUntilIdle()

        viewModel.onVisibilityToggled("com.example.a", hidden = true)
        advanceUntilIdle()

        assertEquals(setOf("com.example.a"), settingsRepository.hiddenApps.value)
        assertEquals(
            listOf(
                AppVisibilityRow(allApps[0], isHidden = true),
                AppVisibilityRow(allApps[1], isHidden = false),
            ),
            viewModel.rows.value,
        )
        collectJob.cancel()
    }

    @Test
    fun `toggling an already-hidden app to visible removes it from the hidden set`() = runTest(testDispatcher) {
        val (viewModel, settingsRepository) = buildViewModel(
            settingsRepository = FakeAppDrawerSettingsRepository(
                initialHiddenApps = setOf("com.example.a", "com.example.b"),
            ),
        )

        val collectJob = launch { viewModel.rows.collect {} }
        advanceUntilIdle()

        viewModel.onVisibilityToggled("com.example.a", hidden = false)
        advanceUntilIdle()

        assertEquals(setOf("com.example.b"), settingsRepository.hiddenApps.value)
        collectJob.cancel()
    }

    @Test
    fun `toggling one app does not affect other hidden apps`() = runTest(testDispatcher) {
        val (viewModel, settingsRepository) = buildViewModel(
            settingsRepository = FakeAppDrawerSettingsRepository(initialHiddenApps = setOf("com.example.b")),
        )

        val collectJob = launch { viewModel.rows.collect {} }
        advanceUntilIdle()

        viewModel.onVisibilityToggled("com.example.a", hidden = true)
        advanceUntilIdle()

        assertEquals(setOf("com.example.a", "com.example.b"), settingsRepository.hiddenApps.value)
        collectJob.cancel()
    }
}
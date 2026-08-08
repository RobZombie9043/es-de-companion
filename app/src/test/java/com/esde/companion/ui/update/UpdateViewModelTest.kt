package com.esde.companion.ui.update

import com.esde.companion.domain.model.ApkAsset
import com.esde.companion.domain.model.DownloadState
import com.esde.companion.domain.model.ReleaseFetchResult
import com.esde.companion.domain.model.ReleaseInfo
import com.esde.companion.domain.model.UpdateCheckResult
import com.esde.companion.domain.repository.UpdateRepository
import com.esde.companion.domain.repository.UpdateStateRepository
import com.esde.companion.domain.usecase.CheckForUpdateUseCase
import com.esde.companion.domain.usecase.DownloadApkUseCase
import com.esde.companion.domain.usecase.FetchReleaseNotesForVersionUseCase
import com.esde.companion.domain.usecase.ObserveLastSeenChangelogVersionCodeUseCase
import com.esde.companion.domain.usecase.SetLastSeenChangelogVersionCodeUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UpdateViewModelTest {
    private class FakeUpdateRepository(
        var latestReleaseResult: ReleaseFetchResult = ReleaseFetchResult.NotFound,
        var releaseForVersionResult: ReleaseFetchResult = ReleaseFetchResult.NotFound,
        var downloadStates: List<DownloadState> = emptyList(),
    ) : UpdateRepository {
        override suspend fun fetchLatestRelease(): ReleaseFetchResult = latestReleaseResult

        override suspend fun fetchReleaseForVersion(versionName: String): ReleaseFetchResult = releaseForVersionResult

        override fun downloadApk(asset: ApkAsset): Flow<DownloadState> = downloadStates.asFlow()
    }

    private class FakeUpdateStateRepository(
        initialLastSeen: Int? = null,
    ) : UpdateStateRepository {
        private val lastSeen = MutableStateFlow(initialLastSeen)
        val setCalls = mutableListOf<Int>()

        override fun observeLastSeenChangelogVersionCode(): Flow<Int?> = lastSeen

        override suspend fun setLastSeenChangelogVersionCode(versionCode: Int) {
            setCalls += versionCode
            lastSeen.value = versionCode
        }
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

    private val release =
        ReleaseInfo(
            versionName = "0.8.0",
            tagName = "v0.8.0",
            releaseNotes = "Notes",
            apkAsset = ApkAsset("https://example.com/app.apk", "app.apk", 100L),
            htmlUrl = "https://example.com/release",
        )

    private fun buildViewModel(
        updateRepository: FakeUpdateRepository = FakeUpdateRepository(),
        updateStateRepository: FakeUpdateStateRepository = FakeUpdateStateRepository(),
        currentVersionName: String = "0.7.0-RC1",
        currentVersionCode: Int = 25,
    ) = UpdateViewModel(
        checkForUpdateUseCase = CheckForUpdateUseCase(updateRepository),
        fetchReleaseNotesForVersionUseCase = FetchReleaseNotesForVersionUseCase(updateRepository),
        downloadApkUseCase = DownloadApkUseCase(updateRepository),
        observeLastSeenChangelogVersionCodeUseCase = ObserveLastSeenChangelogVersionCodeUseCase(updateStateRepository),
        setLastSeenChangelogVersionCodeUseCase = SetLastSeenChangelogVersionCodeUseCase(updateStateRepository),
        runningAppVersion = RunningAppVersion(name = currentVersionName, code = currentVersionCode),
    )

    @Test
    fun `first-ever run seeds the baseline without showing what's new`() =
        runTest(testDispatcher) {
            val updateStateRepository = FakeUpdateStateRepository(initialLastSeen = null)
            val viewModel =
                buildViewModel(
                    updateRepository = FakeUpdateRepository(latestReleaseResult = ReleaseFetchResult.NotFound),
                    updateStateRepository = updateStateRepository,
                    currentVersionCode = 25,
                )

            viewModel.runStartupChecks()
            advanceUntilIdle()

            assertEquals(listOf(25), updateStateRepository.setCalls)
            assertEquals(false, viewModel.uiState.value.showWhatsNewDialog)
        }

    @Test
    fun `versionCode increase since last recorded shows what's new with that version's own notes`() =
        runTest(testDispatcher) {
            val updateStateRepository = FakeUpdateStateRepository(initialLastSeen = 24)
            val updateRepository =
                FakeUpdateRepository(
                    latestReleaseResult = ReleaseFetchResult.NotFound,
                    releaseForVersionResult = ReleaseFetchResult.Success(release),
                )
            val viewModel = buildViewModel(updateRepository, updateStateRepository, currentVersionCode = 25)

            viewModel.runStartupChecks()
            advanceUntilIdle()

            assertEquals(true, viewModel.uiState.value.showWhatsNewDialog)
            assertEquals(release, viewModel.uiState.value.whatsNewRelease)
            assertEquals(listOf(25), updateStateRepository.setCalls)
        }

    @Test
    fun `what's new notes fetch failure still records the new baseline so it doesn't nag every start`() =
        runTest(testDispatcher) {
            val updateStateRepository = FakeUpdateStateRepository(initialLastSeen = 24)
            val updateRepository =
                FakeUpdateRepository(
                    releaseForVersionResult = ReleaseFetchResult.NetworkError("offline"),
                )
            val viewModel = buildViewModel(updateRepository, updateStateRepository, currentVersionCode = 25)

            viewModel.runStartupChecks()
            advanceUntilIdle()

            assertEquals(false, viewModel.uiState.value.showWhatsNewDialog)
            assertEquals(listOf(25), updateStateRepository.setCalls)
        }

    @Test
    fun `no versionCode change does not show what's new or touch the baseline`() =
        runTest(testDispatcher) {
            val updateStateRepository = FakeUpdateStateRepository(initialLastSeen = 25)
            val viewModel = buildViewModel(updateStateRepository = updateStateRepository, currentVersionCode = 25)

            viewModel.runStartupChecks()
            advanceUntilIdle()

            assertEquals(false, viewModel.uiState.value.showWhatsNewDialog)
            assertTrue(updateStateRepository.setCalls.isEmpty())
        }

    @Test
    fun `manual check showing an update sets lastManualCheckResult and shows the update dialog`() =
        runTest(testDispatcher) {
            val updateRepository = FakeUpdateRepository(latestReleaseResult = ReleaseFetchResult.Success(release))
            val viewModel = buildViewModel(updateRepository = updateRepository, currentVersionName = "0.7.0-RC1")

            viewModel.checkForUpdatesManually()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(false, state.isCheckingForUpdate)
            assertEquals(UpdateCheckResult.UpdateAvailable(release), state.lastManualCheckResult)
            assertEquals(true, state.showUpdateDialog)
            assertEquals(release, state.availableRelease)
        }

    @Test
    fun `manual check when already up to date shows no dialog`() =
        runTest(testDispatcher) {
            val currentRelease = release.copy(versionName = "0.7.0-RC1")
            val updateRepository =
                FakeUpdateRepository(latestReleaseResult = ReleaseFetchResult.Success(currentRelease))
            val viewModel = buildViewModel(updateRepository = updateRepository, currentVersionName = "0.7.0-RC1")

            viewModel.checkForUpdatesManually()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(UpdateCheckResult.UpToDate, state.lastManualCheckResult)
            assertEquals(false, state.showUpdateDialog)
        }

    @Test
    fun `manual check network failure surfaces as Failed without crashing`() =
        runTest(testDispatcher) {
            val updateRepository =
                FakeUpdateRepository(latestReleaseResult = ReleaseFetchResult.NetworkError("offline"))
            val viewModel = buildViewModel(updateRepository = updateRepository)

            viewModel.checkForUpdatesManually()
            advanceUntilIdle()

            val result = viewModel.uiState.value.lastManualCheckResult
            assertTrue(result is UpdateCheckResult.Failed)
            assertEquals(false, viewModel.uiState.value.showUpdateDialog)
        }

    @Test
    fun `downloading and installing collects progress through to a final success state`() =
        runTest(testDispatcher) {
            val downloadStates =
                listOf(
                    DownloadState.Progress(0),
                    DownloadState.Progress(50),
                    DownloadState.Progress(100),
                    DownloadState.Success("/tmp/app.apk"),
                )
            val updateRepository =
                FakeUpdateRepository(
                    latestReleaseResult = ReleaseFetchResult.Success(release),
                    downloadStates = downloadStates,
                )
            val viewModel = buildViewModel(updateRepository = updateRepository)

            viewModel.checkForUpdatesManually()
            advanceUntilIdle()
            viewModel.onDownloadAndInstallClicked()
            advanceUntilIdle()

            assertEquals(DownloadState.Success("/tmp/app.apk"), viewModel.uiState.value.downloadState)
        }

    @Test
    fun `dismissing dialogs clears their visibility flags`() =
        runTest(testDispatcher) {
            val updateRepository = FakeUpdateRepository(latestReleaseResult = ReleaseFetchResult.Success(release))
            val viewModel = buildViewModel(updateRepository = updateRepository)

            viewModel.checkForUpdatesManually()
            advanceUntilIdle()
            assertEquals(true, viewModel.uiState.value.showUpdateDialog)

            viewModel.onUpdateDialogDismissed()
            assertEquals(false, viewModel.uiState.value.showUpdateDialog)
        }
}

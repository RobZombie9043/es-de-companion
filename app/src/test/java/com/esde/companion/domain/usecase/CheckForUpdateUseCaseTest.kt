package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.ApkAsset
import com.esde.companion.domain.model.DownloadState
import com.esde.companion.domain.model.ReleaseFetchResult
import com.esde.companion.domain.model.ReleaseInfo
import com.esde.companion.domain.model.UpdateCheckFailureReason
import com.esde.companion.domain.model.UpdateCheckResult
import com.esde.companion.domain.repository.UpdateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckForUpdateUseCaseTest {
    private class FakeUpdateRepository(
        private val latestReleaseResult: ReleaseFetchResult,
    ) : UpdateRepository {
        override suspend fun fetchLatestRelease(): ReleaseFetchResult = latestReleaseResult

        override suspend fun fetchReleaseForVersion(versionName: String): ReleaseFetchResult = latestReleaseResult

        override fun downloadApk(asset: ApkAsset): Flow<DownloadState> = emptyFlow()
    }

    private val apkAsset = ApkAsset("https://example.com/app.apk", "app.apk", 100L)

    private fun release(
        versionName: String,
        apkAsset: ApkAsset? = this.apkAsset,
    ) = ReleaseInfo(
        versionName = versionName,
        tagName = "v$versionName",
        releaseNotes = "Notes",
        apkAsset = apkAsset,
        htmlUrl = "https://example.com/release",
    )

    @Test
    fun `a newer release is reported as UpdateAvailable`() =
        runTest {
            val release = release(versionName = "0.8.0")
            val useCase = CheckForUpdateUseCase(FakeUpdateRepository(ReleaseFetchResult.Success(release)))

            val result = useCase("0.7.0")

            assertEquals(UpdateCheckResult.UpdateAvailable(release), result)
        }

    @Test
    fun `a same-or-older release is reported as UpToDate`() =
        runTest {
            val release = release(versionName = "0.7.0")
            val useCase = CheckForUpdateUseCase(FakeUpdateRepository(ReleaseFetchResult.Success(release)))

            val result = useCase("0.7.0")

            assertEquals(UpdateCheckResult.UpToDate, result)
        }

    @Test
    fun `a newer release with no apk asset fails with NoApkAsset rather than reporting an update`() =
        runTest {
            val release = release(versionName = "0.8.0", apkAsset = null)
            val useCase = CheckForUpdateUseCase(FakeUpdateRepository(ReleaseFetchResult.Success(release)))

            val result = useCase("0.7.0")

            assertEquals(UpdateCheckResult.Failed(UpdateCheckFailureReason.NoApkAsset), result)
        }

    @Test
    fun `NotFound fails with NoReleaseFound`() =
        runTest {
            val useCase = CheckForUpdateUseCase(FakeUpdateRepository(ReleaseFetchResult.NotFound))

            val result = useCase("0.7.0")

            assertEquals(UpdateCheckResult.Failed(UpdateCheckFailureReason.NoReleaseFound), result)
        }

    @Test
    fun `NetworkError fails with Network, forwarding the message unchanged`() =
        runTest {
            val useCase = CheckForUpdateUseCase(FakeUpdateRepository(ReleaseFetchResult.NetworkError("offline")))

            val result = useCase("0.7.0")

            assertTrue(result is UpdateCheckResult.Failed)
            assertEquals(UpdateCheckFailureReason.Network("offline"), (result as UpdateCheckResult.Failed).reason)
        }
}

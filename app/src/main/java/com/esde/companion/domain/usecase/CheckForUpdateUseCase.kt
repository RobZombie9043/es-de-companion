package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.ReleaseFetchResult
import com.esde.companion.domain.model.UpdateCheckFailureReason
import com.esde.companion.domain.model.UpdateCheckResult
import com.esde.companion.domain.repository.UpdateRepository
import com.esde.companion.domain.update.VersionComparator

class CheckForUpdateUseCase(
    private val updateRepository: UpdateRepository,
) {
    suspend operator fun invoke(currentVersionName: String): UpdateCheckResult =
        when (val result = updateRepository.fetchLatestRelease()) {
            is ReleaseFetchResult.Success -> {
                val asset = result.release.apkAsset
                when {
                    asset == null -> UpdateCheckResult.Failed(UpdateCheckFailureReason.NoApkAsset)
                    VersionComparator.isNewer(result.release.versionName, currentVersionName) ->
                        UpdateCheckResult.UpdateAvailable(result.release)
                    else -> UpdateCheckResult.UpToDate
                }
            }
            ReleaseFetchResult.NotFound -> UpdateCheckResult.Failed(UpdateCheckFailureReason.NoReleaseFound)
            is ReleaseFetchResult.NetworkError ->
                UpdateCheckResult.Failed(UpdateCheckFailureReason.Network(result.message))
        }
}

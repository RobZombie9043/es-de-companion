package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.ApkAsset
import com.esde.companion.domain.model.DownloadState
import com.esde.companion.domain.repository.UpdateRepository
import kotlinx.coroutines.flow.Flow

class DownloadApkUseCase(
    private val updateRepository: UpdateRepository,
) {
    operator fun invoke(asset: ApkAsset): Flow<DownloadState> = updateRepository.downloadApk(asset)
}

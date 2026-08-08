package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.ReleaseFetchResult
import com.esde.companion.domain.repository.UpdateRepository

class FetchReleaseNotesForVersionUseCase(
    private val updateRepository: UpdateRepository,
) {
    suspend operator fun invoke(versionName: String): ReleaseFetchResult {
        return updateRepository.fetchReleaseForVersion(versionName)
    }
}

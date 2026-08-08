package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.UpdateStateRepository

class SetLastSeenChangelogVersionCodeUseCase(
    private val updateStateRepository: UpdateStateRepository,
) {
    suspend operator fun invoke(versionCode: Int) = updateStateRepository.setLastSeenChangelogVersionCode(versionCode)
}

package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.UpdateStateRepository
import kotlinx.coroutines.flow.Flow

class ObserveLastSeenChangelogVersionCodeUseCase(
    private val updateStateRepository: UpdateStateRepository,
) {
    operator fun invoke(): Flow<Int?> = updateStateRepository.observeLastSeenChangelogVersionCode()
}

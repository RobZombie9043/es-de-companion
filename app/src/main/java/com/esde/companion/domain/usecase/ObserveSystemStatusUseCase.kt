package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.SystemStatus
import com.esde.companion.domain.repository.SystemStatusRepository
import kotlinx.coroutines.flow.Flow

class ObserveSystemStatusUseCase(
    private val systemStatusRepository: SystemStatusRepository,
) {
    operator fun invoke(): Flow<SystemStatus> = systemStatusRepository.observeSystemStatus()
}

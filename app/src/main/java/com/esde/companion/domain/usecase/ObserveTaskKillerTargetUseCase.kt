package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.TaskKillerTarget
import com.esde.companion.domain.repository.ThorSettingsRepository
import kotlinx.coroutines.flow.Flow

class ObserveTaskKillerTargetUseCase(
    private val thorSettingsRepository: ThorSettingsRepository,
) {
    operator fun invoke(): Flow<TaskKillerTarget> = thorSettingsRepository.observeTaskKillerTarget()
}

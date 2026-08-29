package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.TaskKillerTarget
import com.esde.companion.domain.repository.ThorSettingsRepository

class SetTaskKillerTargetUseCase(
    private val thorSettingsRepository: ThorSettingsRepository,
) {
    suspend operator fun invoke(target: TaskKillerTarget) = thorSettingsRepository.setTaskKillerTarget(target)
}

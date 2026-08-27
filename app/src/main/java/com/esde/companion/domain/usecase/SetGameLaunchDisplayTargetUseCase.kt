package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.GameLaunchDisplayTarget
import com.esde.companion.domain.repository.GameLaunchAppRepository

class SetGameLaunchDisplayTargetUseCase(
    private val gameLaunchAppRepository: GameLaunchAppRepository,
) {
    suspend operator fun invoke(target: GameLaunchDisplayTarget) {
        gameLaunchAppRepository.setLaunchDisplayTarget(target)
    }
}

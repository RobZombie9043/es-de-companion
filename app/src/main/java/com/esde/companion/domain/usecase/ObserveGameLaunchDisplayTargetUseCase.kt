package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.GameLaunchDisplayTarget
import com.esde.companion.domain.repository.GameLaunchAppRepository
import kotlinx.coroutines.flow.Flow

class ObserveGameLaunchDisplayTargetUseCase(
    private val gameLaunchAppRepository: GameLaunchAppRepository,
) {
    operator fun invoke(): Flow<GameLaunchDisplayTarget> = gameLaunchAppRepository.observeLaunchDisplayTarget()
}

package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.GameLaunchAppRepository
import kotlinx.coroutines.flow.Flow

class ObserveGameLaunchEnabledUseCase(
    private val gameLaunchAppRepository: GameLaunchAppRepository,
) {
    operator fun invoke(): Flow<Boolean> = gameLaunchAppRepository.observeEnabled()
}

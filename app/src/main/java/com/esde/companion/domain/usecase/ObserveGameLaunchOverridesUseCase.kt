package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.GameLaunchOverride
import com.esde.companion.domain.repository.GameLaunchAppRepository
import kotlinx.coroutines.flow.Flow

class ObserveGameLaunchOverridesUseCase(
    private val gameLaunchAppRepository: GameLaunchAppRepository,
) {
    operator fun invoke(): Flow<List<GameLaunchOverride>> = gameLaunchAppRepository.observeGameOverrides()
}

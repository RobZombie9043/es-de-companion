package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.GameLaunchAppRepository
import kotlinx.coroutines.flow.Flow

class ObserveGameLaunchSystemDefaultsUseCase(
    private val gameLaunchAppRepository: GameLaunchAppRepository,
) {
    operator fun invoke(): Flow<Map<String, String>> = gameLaunchAppRepository.observeSystemDefaults()
}

package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.GameLaunchAppRepository

class SetGameLaunchEnabledUseCase(
    private val gameLaunchAppRepository: GameLaunchAppRepository,
) {
    suspend operator fun invoke(enabled: Boolean) {
        gameLaunchAppRepository.setEnabled(enabled)
    }
}

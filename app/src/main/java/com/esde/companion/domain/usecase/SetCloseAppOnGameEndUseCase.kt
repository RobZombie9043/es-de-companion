package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.GameLaunchAppRepository

class SetCloseAppOnGameEndUseCase(
    private val gameLaunchAppRepository: GameLaunchAppRepository,
) {
    suspend operator fun invoke(enabled: Boolean) {
        gameLaunchAppRepository.setCloseAppOnGameEnd(enabled)
    }
}

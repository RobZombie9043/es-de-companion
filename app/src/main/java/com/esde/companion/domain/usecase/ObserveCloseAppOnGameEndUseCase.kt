package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.GameLaunchAppRepository
import kotlinx.coroutines.flow.Flow

class ObserveCloseAppOnGameEndUseCase(
    private val gameLaunchAppRepository: GameLaunchAppRepository,
) {
    operator fun invoke(): Flow<Boolean> = gameLaunchAppRepository.observeCloseAppOnGameEnd()
}

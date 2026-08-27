package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.GameLaunchAppRepository

class SetGameLaunchSystemDefaultUseCase(
    private val gameLaunchAppRepository: GameLaunchAppRepository,
) {
    suspend operator fun invoke(
        systemShortName: String,
        packageName: String?,
    ) = gameLaunchAppRepository.setSystemDefault(systemShortName, packageName)
}

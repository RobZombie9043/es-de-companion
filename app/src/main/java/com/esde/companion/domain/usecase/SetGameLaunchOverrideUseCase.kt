package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.GameLaunchAppRepository

/** [packageName] null persists an explicit "never launch anything for this game" override -
 * distinct from [ClearGameLaunchOverrideUseCase], which removes the entry entirely. */
class SetGameLaunchOverrideUseCase(
    private val gameLaunchAppRepository: GameLaunchAppRepository,
) {
    suspend operator fun invoke(
        systemShortName: String,
        relativeRomPath: String,
        packageName: String?,
    ) = gameLaunchAppRepository.setGameOverride(systemShortName, relativeRomPath, packageName)
}

package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.GameLaunchAppRepository

/** Removes a per-game override entirely, returning that game to inheriting the system default -
 * see [SetGameLaunchOverrideUseCase] for the distinct "explicitly none" state. */
class ClearGameLaunchOverrideUseCase(
    private val gameLaunchAppRepository: GameLaunchAppRepository,
) {
    suspend operator fun invoke(
        systemShortName: String,
        relativeRomPath: String,
    ) = gameLaunchAppRepository.clearGameOverride(systemShortName, relativeRomPath)
}

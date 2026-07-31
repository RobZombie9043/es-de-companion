package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.GameDescription
import com.esde.companion.domain.repository.GameDescriptionRepository

/**
 * The single entry point the UI layer should depend on to look up a game's description -
 * same shape as ResolveGameMediaUseCase, kept as its own use case for the same reason.
 */
class ResolveGameDescriptionUseCase(
    private val gameDescriptionRepository: GameDescriptionRepository,
) {
    suspend operator fun invoke(systemShortName: String, romPath: String): GameDescription =
        gameDescriptionRepository.resolveDescription(systemShortName, romPath)
}
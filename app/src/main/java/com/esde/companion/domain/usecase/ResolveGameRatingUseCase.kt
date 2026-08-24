package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.GameRating
import com.esde.companion.domain.repository.GameRatingRepository

/**
 * The single entry point the UI layer should depend on to look up a game's rating - same
 * shape as ResolveGameDescriptionUseCase, kept as its own use case for the same reason.
 */
class ResolveGameRatingUseCase(
    private val gameRatingRepository: GameRatingRepository,
) {
    suspend operator fun invoke(
        systemShortName: String,
        romPath: String,
    ): GameRating = gameRatingRepository.resolveRating(systemShortName, romPath)
}

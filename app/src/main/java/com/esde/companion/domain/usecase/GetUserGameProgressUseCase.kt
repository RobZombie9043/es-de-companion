package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.UserGameProgress
import com.esde.companion.domain.repository.RetroAchievementsRepository

/**
 * The signed-in user's cross-console completion progress, keyed by RA gameId - not scoped to
 * any particular system, since RA's own endpoint isn't console-filterable (see
 * `UserProgressCache`, data layer). Callers do their own O(1) gameId lookups against the
 * returned map rather than this use case taking a console/system argument.
 */
class GetUserGameProgressUseCase(
    private val retroAchievementsRepository: RetroAchievementsRepository,
) {
    suspend operator fun invoke(): Map<Long, UserGameProgress> = retroAchievementsRepository.getUserGameProgress()
}

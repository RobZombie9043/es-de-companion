package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.AchievementSummaryFetchResult
import com.esde.companion.domain.repository.RetroAchievementsRepository

/** A thin wrapper over fetching a resolved game's achievement summary, deliberately separate from resolution. */
class GetGameAchievementSummaryUseCase(
    private val retroAchievementsRepository: RetroAchievementsRepository,
) {
    suspend operator fun invoke(
        gameId: Long,
        forceRefresh: Boolean = false,
    ): AchievementSummaryFetchResult {
        return retroAchievementsRepository.getAchievementSummary(gameId, forceRefresh)
    }
}

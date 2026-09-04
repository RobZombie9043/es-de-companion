package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.AchievementSummaryPeek
import com.esde.companion.domain.repository.RetroAchievementsRepository

/**
 * A cache-only peek at a resolved game's achievement summary - never triggers a network fetch,
 * unlike [GetGameAchievementSummaryUseCase]. Lets a caller show a stale-but-cached summary
 * immediately while deciding whether to also fetch a fresh one in the background.
 */
class PeekGameAchievementSummaryUseCase(
    private val retroAchievementsRepository: RetroAchievementsRepository,
) {
    suspend operator fun invoke(gameId: Long): AchievementSummaryPeek? {
        return retroAchievementsRepository.peekAchievementSummary(gameId)
    }
}

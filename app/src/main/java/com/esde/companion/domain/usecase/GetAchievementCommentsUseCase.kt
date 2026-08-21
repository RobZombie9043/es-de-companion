package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.AchievementCommentsFetchResult
import com.esde.companion.domain.repository.RetroAchievementsRepository

/** A thin wrapper over fetching an achievement's wall comments, deliberately separate from the achievement summary. */
class GetAchievementCommentsUseCase(
    private val retroAchievementsRepository: RetroAchievementsRepository,
) {
    suspend operator fun invoke(achievementId: Long): AchievementCommentsFetchResult {
        return retroAchievementsRepository.getAchievementComments(achievementId)
    }
}

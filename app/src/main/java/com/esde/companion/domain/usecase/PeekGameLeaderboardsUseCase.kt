package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.GameLeaderboardsPeek
import com.esde.companion.domain.repository.RetroAchievementsRepository

/**
 * A cache-only peek at a resolved game's leaderboard list - never triggers a network fetch,
 * unlike [GetGameLeaderboardsUseCase]. Mirrors [PeekGameAchievementSummaryUseCase]'s reasoning
 * for the Leaderboards facet.
 */
class PeekGameLeaderboardsUseCase(
    private val retroAchievementsRepository: RetroAchievementsRepository,
) {
    suspend operator fun invoke(gameId: Long): GameLeaderboardsPeek? {
        return retroAchievementsRepository.peekGameLeaderboards(gameId)
    }
}

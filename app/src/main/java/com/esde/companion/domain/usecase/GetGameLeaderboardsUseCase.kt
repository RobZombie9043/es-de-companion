package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.LeaderboardsFetchResult
import com.esde.companion.domain.repository.RetroAchievementsRepository

/** A thin wrapper over fetching a resolved game's leaderboard list, deliberately separate from resolution. */
class GetGameLeaderboardsUseCase(
    private val retroAchievementsRepository: RetroAchievementsRepository,
) {
    suspend operator fun invoke(
        gameId: Long,
        forceRefresh: Boolean = false,
    ): LeaderboardsFetchResult {
        return retroAchievementsRepository.getGameLeaderboards(gameId, forceRefresh)
    }
}

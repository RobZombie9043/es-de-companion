package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.LeaderboardEntriesFetchResult
import com.esde.companion.domain.repository.RetroAchievementsRepository

/** A thin wrapper over fetching one leaderboard's entries. */
class GetLeaderboardEntriesUseCase(
    private val retroAchievementsRepository: RetroAchievementsRepository,
) {
    suspend operator fun invoke(leaderboardId: Long): LeaderboardEntriesFetchResult {
        return retroAchievementsRepository.getLeaderboardEntries(leaderboardId)
    }
}

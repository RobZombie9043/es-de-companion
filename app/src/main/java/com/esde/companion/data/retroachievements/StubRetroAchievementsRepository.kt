package com.esde.companion.data.retroachievements

import com.esde.companion.domain.model.AchievementSummaryFetchResult
import com.esde.companion.domain.model.RetroAchievementsAuthState
import com.esde.companion.domain.model.RetroAchievementsCandidateGame
import com.esde.companion.domain.model.RetroAchievementsConsole
import com.esde.companion.domain.model.RetroAchievementsCredentials
import com.esde.companion.domain.repository.RetroAchievementsRepository

private const val NOT_YET_CONNECTED_MESSAGE = "RetroAchievements isn't connected yet in this build."

/**
 * Placeholder [RetroAchievementsRepository] until the real `RetroAchievements/api-kotlin`
 * client is wired in (see CLAUDE.md's RetroAchievements section). Every credential
 * submitted through Settings' Connect flow fails validation, and there is no game or
 * achievement data to return - this exists purely so the Settings UI's validate-then-persist
 * flow (and the invariant that persistence never happens on a failed validation) can be
 * built and exercised end-to-end before the live API client exists. Replace this wiring in
 * AppContainer with the real implementation once that client lands.
 */
class StubRetroAchievementsRepository : RetroAchievementsRepository {
    override suspend fun validateCredentials(credentials: RetroAchievementsCredentials): RetroAchievementsAuthState =
        RetroAchievementsAuthState.Error(NOT_YET_CONNECTED_MESSAGE)

    override suspend fun getCandidateGames(console: RetroAchievementsConsole): List<RetroAchievementsCandidateGame> {
        return emptyList()
    }

    override suspend fun getAchievementSummary(gameId: Long): AchievementSummaryFetchResult {
        return AchievementSummaryFetchResult.NotFound
    }
}

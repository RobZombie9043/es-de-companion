package com.esde.companion.data.retroachievements

import com.esde.companion.domain.model.AchievementSummaryFetchResult
import com.esde.companion.domain.model.RetroAchievementsAuthState
import com.esde.companion.domain.model.RetroAchievementsCandidateGame
import com.esde.companion.domain.model.RetroAchievementsConsole
import com.esde.companion.domain.model.RetroAchievementsCredentials
import com.esde.companion.domain.repository.RetroAchievementsRepository

/**
 * Real [RetroAchievementsRepository]. Only [validateCredentials] is backed by the live API
 * so far - [getCandidateGames]/[getAchievementSummary] still return placeholders, since
 * nothing calls them for real yet (no `GameListCache`, no identification/achievement UI -
 * see CLAUDE.md's RetroAchievements section for the remaining PRs that wire them up).
 *
 * [apiFactory] builds a fresh [RetroAchievementsApi] for whichever [RetroAchievementsCredentials]
 * a call needs to run as, rather than this repository holding one fixed client - [validateCredentials]
 * is inherently about testing not-yet-stored, possibly-wrong credentials, so there is no
 * single "current" client to reuse for it.
 */
class RetroAchievementsRepositoryImpl(
    private val apiFactory: (RetroAchievementsCredentials) -> RetroAchievementsApi,
) : RetroAchievementsRepository {
    override suspend fun validateCredentials(credentials: RetroAchievementsCredentials): RetroAchievementsAuthState {
        val api = apiFactory(credentials)
        return when (val result = api.getUserSummary(credentials.username)) {
            is RetroAchievementsApiResult.Success ->
                RetroAchievementsAuthState.SignedIn(
                    username = result.data.username,
                    points = result.data.points,
                    avatarUrl = result.data.avatarUrl,
                )
            is RetroAchievementsApiResult.Error -> RetroAchievementsAuthState.Error(result.message)
        }
    }

    override suspend fun getCandidateGames(console: RetroAchievementsConsole): List<RetroAchievementsCandidateGame> {
        return emptyList()
    }

    override suspend fun getAchievementSummary(gameId: Long): AchievementSummaryFetchResult {
        return AchievementSummaryFetchResult.NotFound
    }
}

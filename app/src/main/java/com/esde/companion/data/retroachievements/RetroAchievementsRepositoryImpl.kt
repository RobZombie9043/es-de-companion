package com.esde.companion.data.retroachievements

import com.esde.companion.domain.model.AchievementSummaryFetchResult
import com.esde.companion.domain.model.RetroAchievementsAuthState
import com.esde.companion.domain.model.RetroAchievementsCandidateGame
import com.esde.companion.domain.model.RetroAchievementsConsole
import com.esde.companion.domain.model.RetroAchievementsCredentials
import com.esde.companion.domain.model.UserGameProgress
import com.esde.companion.domain.repository.RetroAchievementsCredentialsRepository
import com.esde.companion.domain.repository.RetroAchievementsRepository
import kotlinx.coroutines.flow.first

/**
 * Real [RetroAchievementsRepository], now fully wired to the live API.
 *
 * [apiFactory] builds a fresh [RetroAchievementsApi] for whichever [RetroAchievementsCredentials]
 * a call needs to run as, rather than this repository holding one fixed client:
 * [validateCredentials] is inherently about testing not-yet-stored, possibly-wrong
 * credentials, and [getCandidateGames]/[getAchievementSummary] need whichever credentials
 * are *currently* signed in (read from [credentialsRepository] at call time, since sign-in
 * state can change across this repository's lifetime) - there is no single "current" client
 * to hold onto in either case.
 */
class RetroAchievementsRepositoryImpl(
    private val credentialsRepository: RetroAchievementsCredentialsRepository,
    private val gameListCache: GameListCache,
    private val userProgressCache: UserProgressCache,
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
        val credentials = currentCredentials() ?: return emptyList()
        return gameListCache.getGames(console, apiFactory(credentials))
    }

    override suspend fun getAchievementSummary(gameId: Long): AchievementSummaryFetchResult {
        val credentials = currentCredentials() ?: return AchievementSummaryFetchResult.NotFound
        val api = apiFactory(credentials)
        return when (val result = api.getGameInfoAndUserProgress(credentials.username, gameId)) {
            is RetroAchievementsApiResult.Success -> AchievementSummaryFetchResult.Success(result.data)
            is RetroAchievementsApiResult.Error -> AchievementSummaryFetchResult.NetworkError(result.message)
        }
    }

    override suspend fun getUserGameProgress(): Map<Long, UserGameProgress> {
        val credentials = currentCredentials() ?: return emptyMap()
        return userProgressCache.getProgress(credentials.username, apiFactory(credentials))
    }

    private suspend fun currentCredentials(): RetroAchievementsCredentials? {
        return credentialsRepository.observeCredentials().first()
    }
}

package com.esde.companion.domain.repository

import com.esde.companion.domain.model.AchievementSummaryFetchResult
import com.esde.companion.domain.model.RetroAchievementsAuthState
import com.esde.companion.domain.model.RetroAchievementsCandidateGame
import com.esde.companion.domain.model.RetroAchievementsConsole
import com.esde.companion.domain.model.RetroAchievementsCredentials
import com.esde.companion.domain.model.UserGameProgress

/**
 * Talks to the RetroAchievements Web API. Deliberately has no method that judges which
 * candidate game is "right" for a ROM - that is
 * [com.esde.companion.domain.parser.GameTitleMatcher]'s job, called from
 * `ResolveRetroAchievementsGameUseCase`. This interface only ever hands back
 * RetroAchievements data as-is.
 */
interface RetroAchievementsRepository {
    /**
     * Confirms [credentials] against the live API - does not persist them, see
     * `ValidateRetroAchievementsCredentialsUseCase`.
     */
    suspend fun validateCredentials(credentials: RetroAchievementsCredentials): RetroAchievementsAuthState

    /**
     * [console]'s full game list, already resolved through whatever caching/stale-fallback
     * the implementation applies.
     */
    suspend fun getCandidateGames(console: RetroAchievementsConsole): List<RetroAchievementsCandidateGame>

    suspend fun getAchievementSummary(gameId: Long): AchievementSummaryFetchResult

    /**
     * The signed-in user's cross-console completion progress, keyed by RA gameId. Returns
     * `emptyMap()` when signed out or on total failure, mirroring [getCandidateGames]'s
     * `emptyList()` contract - a game absent from the map has no recorded progress, not
     * an error.
     */
    suspend fun getUserGameProgress(): Map<Long, UserGameProgress>
}

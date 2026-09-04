package com.esde.companion.domain.repository

import com.esde.companion.domain.model.AchievementCommentsFetchResult
import com.esde.companion.domain.model.AchievementSummaryFetchResult
import com.esde.companion.domain.model.AchievementSummaryPeek
import com.esde.companion.domain.model.GameLeaderboardsPeek
import com.esde.companion.domain.model.LeaderboardEntriesFetchResult
import com.esde.companion.domain.model.LeaderboardsFetchResult
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

    /**
     * [forceRefresh] bypasses whatever caching the implementation applies, so a user can
     * manually pull fresh unlock status instead of waiting out its TTL.
     */
    suspend fun getAchievementSummary(
        gameId: Long,
        forceRefresh: Boolean = false,
    ): AchievementSummaryFetchResult

    /**
     * A cache-only peek at [gameId]'s achievement summary - never triggers a network fetch,
     * `null` if nothing is cached (signed out, or never fetched/persisted). See
     * [AchievementSummaryPeek]'s kdoc.
     */
    suspend fun peekAchievementSummary(gameId: Long): AchievementSummaryPeek?

    /**
     * The signed-in user's cross-console completion progress, keyed by RA gameId. Returns
     * `emptyMap()` when signed out or on total failure, mirroring [getCandidateGames]'s
     * `emptyList()` contract - a game absent from the map has no recorded progress, not
     * an error.
     */
    suspend fun getUserGameProgress(): Map<Long, UserGameProgress>

    /** [achievementId]'s wall comments, already resolved through whatever caching the implementation applies. */
    suspend fun getAchievementComments(achievementId: Long): AchievementCommentsFetchResult

    /**
     * [gameId]'s full leaderboard list, merged with the signed-in user's own entries.
     * [forceRefresh] bypasses whatever caching the implementation applies, same as [getAchievementSummary].
     */
    suspend fun getGameLeaderboards(
        gameId: Long,
        forceRefresh: Boolean = false,
    ): LeaderboardsFetchResult

    /**
     * A cache-only peek at [gameId]'s leaderboard list - never triggers a network fetch, `null`
     * if nothing is cached. See [GameLeaderboardsPeek]'s kdoc.
     */
    suspend fun peekGameLeaderboards(gameId: Long): GameLeaderboardsPeek?

    /** [leaderboardId]'s entries, already resolved through whatever caching the implementation applies. */
    suspend fun getLeaderboardEntries(leaderboardId: Long): LeaderboardEntriesFetchResult
}

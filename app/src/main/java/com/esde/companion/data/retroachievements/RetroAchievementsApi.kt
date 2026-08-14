package com.esde.companion.data.retroachievements

import com.esde.companion.domain.model.GameAchievementSummary
import com.esde.companion.domain.model.RetroAchievementsCandidateGame
import com.esde.companion.domain.model.UserGameProgress

typealias GameListResult = RetroAchievementsApiResult<List<RetroAchievementsCandidateGame>>

/**
 * Narrow seam over RetroAchievements/api-kotlin's real `RetroInterface`, exposing only the
 * calls this integration needs - implemented for real by [RetroClientRetroAchievementsApi].
 * This is the key testability move (see CLAUDE.md's RetroAchievements section): every method
 * returns a small domain-shaped [RetroAchievementsApiResult] rather than api-kotlin's own
 * Retrofit/Gson-shaped `NetworkResponse`/response POJOs, so [RetroAchievementsRepositoryImpl]
 * and [GameListCache] can be unit-tested against a hand-rolled fake instead of the real
 * Retrofit/OkHttp stack, since there's no practical way to stand up a fake server under a
 * client this app doesn't construct itself.
 */
interface RetroAchievementsApi {
    suspend fun getUserSummary(username: String): RetroAchievementsApiResult<RetroAchievementsUserSummary>

    suspend fun getGameList(consoleId: Long): GameListResult

    suspend fun getGameInfoAndUserProgress(
        username: String,
        gameId: Long,
    ): RetroAchievementsApiResult<GameAchievementSummary>

    /** One page of [username]'s cross-console completion progress - see [UserCompletionProgressPage]. */
    suspend fun getUserCompletionProgress(
        username: String,
        offset: Int,
        count: Int,
    ): RetroAchievementsApiResult<UserCompletionProgressPage>
}

/**
 * One page of `getUserCompletionProgress` - [total] is RA's reported total entry count across
 * every page (not just this page's size), used by [UserProgressCache] to know when to stop
 * paginating.
 */
data class UserCompletionProgressPage(
    val total: Int,
    val entries: List<UserGameProgress>,
)

package com.esde.companion.data.retroachievements

import org.retroachivements.api.data.pojo.game.GetGameInfoAndUserProgress
import org.retroachivements.api.data.pojo.system.GetGameList

/**
 * Narrow seam over RetroAchievements/api-kotlin's real `RetroInterface`, exposing only the
 * calls this integration needs - implemented for real by [RetroClientRetroAchievementsApi].
 * This is the key testability move (see CLAUDE.md's RetroAchievements section): it lets
 * [RetroAchievementsRepositoryImpl] be unit-tested against a hand-rolled fake instead of the
 * real Retrofit/OkHttp stack, since there's no practical way to stand up a fake server under
 * a client this app doesn't construct itself.
 *
 * [getGameList]/[getGameInfoAndUserProgress] pass api-kotlin's own response POJOs straight
 * through rather than mapping to a minimal DTO the way [getUserSummary] does - nothing calls
 * them for real yet (`RetroAchievementsRepositoryImpl.getCandidateGames`/`getAchievementSummary`
 * are still placeholders pending `GameListCache`), so there's no fake/test yet to justify
 * narrowing their shape. Narrow it when that PR actually needs to.
 */
interface RetroAchievementsApi {
    suspend fun getUserSummary(username: String): RetroAchievementsApiResult<RetroAchievementsUserSummary>

    suspend fun getGameList(consoleId: Long): RetroAchievementsApiResult<GetGameList.Response>

    suspend fun getGameInfoAndUserProgress(
        username: String,
        gameId: Long,
    ): RetroAchievementsApiResult<GetGameInfoAndUserProgress.Response>
}

package com.esde.companion.data.retroachievements

import com.esde.companion.domain.model.RetroAchievementsCredentials
import com.haroldadmin.cnradapter.NetworkResponse
import org.retroachivements.api.RetroClient
import org.retroachivements.api.data.RetroCredentials
import org.retroachivements.api.data.pojo.ErrorResponse
import org.retroachivements.api.data.pojo.game.GetGameInfoAndUserProgress
import org.retroachivements.api.data.pojo.system.GetGameList
import org.retroachivements.api.data.pojo.user.GetUserSummary

private const val MEDIA_BASE_URL = "https://i.retroachievements.org"

/**
 * Real [RetroAchievementsApi], wrapping api-kotlin's `RetroClient(credentials).api`. A
 * fresh instance is constructed for whichever [credentials] a given call needs to run
 * as - see `RetroAchievementsRepositoryImpl`'s `apiFactory` - rather than being a single
 * long-lived client, since which credentials are "current" can change (a fresh Connect
 * attempt with different credentials than whatever's already stored).
 */
class RetroClientRetroAchievementsApi(
    credentials: RetroAchievementsCredentials,
) : RetroAchievementsApi {
    private val api = RetroClient(RetroCredentials(credentials.username, credentials.webApiKey)).api

    override suspend fun getUserSummary(username: String): RetroAchievementsApiResult<RetroAchievementsUserSummary> =
        api.getUserSummary(username).toApiResult { it.toUserSummary() }

    override suspend fun getGameList(consoleId: Long): RetroAchievementsApiResult<GetGameList.Response> =
        api.getGameList(consoleId).toApiResult { it }

    override suspend fun getGameInfoAndUserProgress(
        username: String,
        gameId: Long,
    ): RetroAchievementsApiResult<GetGameInfoAndUserProgress.Response> {
        return api.getGameInfoAndUserProgress(username, gameId).toApiResult { it }
    }

    private fun GetUserSummary.Response.toUserSummary() =
        RetroAchievementsUserSummary(
            username = user,
            points = totalPoints?.toInt() ?: 0,
            avatarUrl = userPic?.let { "$MEDIA_BASE_URL$it" },
        )

    private fun <S, T> NetworkResponse<S, ErrorResponse>.toApiResult(map: (S) -> T): RetroAchievementsApiResult<T> =
        when (this) {
            is NetworkResponse.Success -> RetroAchievementsApiResult.Success(map(body))
            is NetworkResponse.Error ->
                RetroAchievementsApiResult.Error(
                    body?.message ?: error?.message ?: "RetroAchievements request failed",
                )
        }
}

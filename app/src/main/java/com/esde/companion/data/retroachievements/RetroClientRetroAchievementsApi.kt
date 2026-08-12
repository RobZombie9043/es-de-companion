package com.esde.companion.data.retroachievements

import com.esde.companion.domain.model.AchievementItem
import com.esde.companion.domain.model.GameAchievementSummary
import com.esde.companion.domain.model.RetroAchievementsCandidateGame
import com.esde.companion.domain.model.RetroAchievementsCredentials
import com.haroldadmin.cnradapter.NetworkResponse
import org.retroachivements.api.RetroClient
import org.retroachivements.api.data.RetroCredentials
import org.retroachivements.api.data.pojo.ErrorResponse
import org.retroachivements.api.data.pojo.game.GetGameInfoAndUserProgress
import org.retroachivements.api.data.pojo.user.GetUserSummary
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private const val MEDIA_BASE_URL = "https://i.retroachievements.org"
private const val PERCENT_MULTIPLIER = 100f
private val RA_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

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

    override suspend fun getGameList(consoleId: Long): GameListResult =
        api.getGameList(consoleId).toApiResult { games -> games.map { it.toCandidateGame() } }

    override suspend fun getGameInfoAndUserProgress(
        username: String,
        gameId: Long,
    ): RetroAchievementsApiResult<GameAchievementSummary> {
        return api.getGameInfoAndUserProgress(username, gameId).toApiResult { it.toSummary() }
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

private fun org.retroachivements.api.data.pojo.system.GetGameList.Response.Game.toCandidateGame() =
    RetroAchievementsCandidateGame(gameId = id, title = title, iconUrl = "https://i.retroachievements.org$imageIcon")

private fun GetGameInfoAndUserProgress.Response.toSummary(): GameAchievementSummary {
    val achievementItems = achievements.values.map { it.toAchievementItem() }
    val unlockedItems = achievementItems.filter { it.unlocked }
    val completionPercent =
        if (achievementItems.isEmpty()) {
            0f
        } else {
            (unlockedItems.size.toFloat() / achievementItems.size.toFloat()) * PERCENT_MULTIPLIER
        }
    return GameAchievementSummary(
        gameId = id.toLong(),
        gameTitle = title,
        totalPoints = achievementItems.sumOf { it.points },
        earnedPoints = unlockedItems.sumOf { it.points },
        completionPercent = completionPercent,
        achievements = achievementItems,
    )
}

private fun GetGameInfoAndUserProgress.Response.Achievement.toAchievementItem(): AchievementItem {
    val unlockedAtTimestamp = dateEarnedHardcore ?: dateEarned
    return AchievementItem(
        id = id.toLongOrNull() ?: 0L,
        title = title,
        description = description,
        points = points.toInt(),
        badgeUrl = "$MEDIA_BASE_URL/Badge/$badgeName.png",
        unlocked = unlockedAtTimestamp != null,
        unlockedAt = unlockedAtTimestamp?.let(::parseRaTimestamp),
    )
}

/**
 * RetroAchievements' documented timestamp format is `yyyy-MM-dd HH:mm:ss` (UTC) - if a
 * response ever deviates from that, this yields `null` (an "unknown unlock time", not a
 * crash) rather than trusting an unverified format string absolutely.
 */
private fun parseRaTimestamp(value: String): Long? =
    try {
        LocalDateTime.parse(value, RA_TIMESTAMP_FORMAT).toInstant(ZoneOffset.UTC).toEpochMilli()
    } catch (
        @Suppress("SwallowedException") e: DateTimeParseException,
    ) {
        null
    }

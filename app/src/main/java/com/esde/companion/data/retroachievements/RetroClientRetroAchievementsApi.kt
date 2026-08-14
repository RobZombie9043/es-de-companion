package com.esde.companion.data.retroachievements

import com.esde.companion.domain.model.AchievementItem
import com.esde.companion.domain.model.GameAchievementSummary
import com.esde.companion.domain.model.RetroAchievementsCandidateGame
import com.esde.companion.domain.model.RetroAchievementsCredentials
import com.esde.companion.domain.model.UserGameProgress
import com.esde.companion.domain.model.progressStatusFor
import com.esde.companion.domain.model.toAchievementType
import com.haroldadmin.cnradapter.NetworkResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import org.retroachivements.api.RetroClient
import org.retroachivements.api.data.RetroCredentials
import org.retroachivements.api.data.pojo.ErrorResponse
import org.retroachivements.api.data.pojo.game.GetGameInfoAndUserProgress
import org.retroachivements.api.data.pojo.user.GetUserCompletionProgress
import org.retroachivements.api.data.pojo.user.GetUserSummary
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private const val MEDIA_BASE_URL = "https://i.retroachievements.org"
private const val PERCENT_MULTIPLIER = 100f
private val RA_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

// TODO(cheevos): api-kotlin 2.0.0 has a confirmed bug affecting achievement type/classification
// (Missable/Progression/WinCondition) data on both endpoints that could supply it - see
// fetchAchievementTypesById's kdoc. Recheck whether a newer api-kotlin release fixes this
// (upstream issue: https://github.com/RetroAchievements/api-kotlin/issues, file one referencing
// GetGameExtended.Response.Achievement.type's @SerializedName("Type") vs. the live API's actual
// lowercase "type" key if one doesn't already exist) and remove this raw-HTTP workaround in
// favor of api-kotlin's own client once it's safe to do so.
private const val GAME_EXTENDED_ENDPOINT = "https://retroachievements.org/API/API_GetGameExtended.php"
private const val ACHIEVEMENT_TYPES_CONNECT_TIMEOUT_MS = 10_000
private const val ACHIEVEMENT_TYPES_READ_TIMEOUT_MS = 15_000

/**
 * Real [RetroAchievementsApi], wrapping api-kotlin's `RetroClient(credentials).api`. A
 * fresh instance is constructed for whichever [credentials] a given call needs to run
 * as - see `RetroAchievementsRepositoryImpl`'s `apiFactory` - rather than being a single
 * long-lived client, since which credentials are "current" can change (a fresh Connect
 * attempt with different credentials than whatever's already stored).
 */
class RetroClientRetroAchievementsApi(
    private val credentials: RetroAchievementsCredentials,
) : RetroAchievementsApi {
    private val api = RetroClient(RetroCredentials(credentials.username, credentials.webApiKey)).api

    override suspend fun getUserSummary(username: String): RetroAchievementsApiResult<RetroAchievementsUserSummary> =
        api.getUserSummary(username).toApiResult { it.toUserSummary() }

    override suspend fun getGameList(consoleId: Long): GameListResult {
        val response = api.getGameList(consoleId, shouldRetrieveGameHashes = 1)
        return response.toApiResult { games -> games.map { it.toCandidateGame() } }
    }

    override suspend fun getGameInfoAndUserProgress(
        username: String,
        gameId: Long,
    ): RetroAchievementsApiResult<GameAchievementSummary> =
        coroutineScope {
            // GetGameInfoAndUserProgress's own achievement `type` field is unusable - see
            // fetchAchievementTypesById's kdoc for the confirmed root cause - so achievement
            // type/classification (Missable/Progression/WinCondition) is fetched separately and
            // merged in below.
            val progressDeferred = async { api.getGameInfoAndUserProgress(username, gameId, includeUserAward = 1) }
            val typesDeferred = async { fetchAchievementTypesById(gameId) }
            val progressResult = progressDeferred.await()
            val typesById = typesDeferred.await()
            progressResult.toApiResult { it.toSummary(typesById) }
        }

    /**
     * A narrow, deliberate exception to "RetroAchievements traffic only goes through api-kotlin"
     * (see CLAUDE.md's What NOT to Do) - a raw [HttpURLConnection] call, the same pattern
     * `GitHubUpdateRepository` already uses for the update checker, solely to recover achievement
     * type/classification data that api-kotlin's own client can never return correctly through
     * either endpoint that exposes it, confirmed by direct inspection (raw response body, and
     * the library's own decompiled/sources-jar `@SerializedName` annotations):
     * - `GetGameInfoAndUserProgress`'s `Achievement.type` comes back `null` unconditionally - the
     *   live response for this endpoint has no `type` key on any achievement at all, regardless
     *   of `includeUserAward` or any other request parameter.
     * - `GetGameExtended`'s `Achievement.type` field IS present in the live response (confirmed
     *   via raw JSON: a real classified achievement's entry contains `"type":"progression"`,
     *   lowercase), but api-kotlin's own class annotates it `@SerializedName("Type")`
     *   (capitalized) - Gson's matching is case-sensitive, so it silently deserializes to `null`
     *   despite the data genuinely being there.
     *
     * See this file's top-of-file TODO for the upstream issue to track / revisit.
     */
    private suspend fun fetchAchievementTypesById(gameId: Long): Map<Long, String?> =
        withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                connection = openGameExtendedConnection(gameId)
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val body = connection.inputStream.use { it.reader().readText() }
                    parseAchievementTypesById(body)
                } else {
                    emptyMap()
                }
            } catch (
                @Suppress("SwallowedException") e: IOException,
            ) {
                emptyMap()
            } catch (
                @Suppress("SwallowedException") e: JSONException,
            ) {
                emptyMap()
            } finally {
                connection?.disconnect()
            }
        }

    private fun openGameExtendedConnection(gameId: Long): HttpURLConnection {
        val url = "$GAME_EXTENDED_ENDPOINT?i=$gameId&f=3&z=${credentials.username}&y=${credentials.webApiKey}"
        return (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = ACHIEVEMENT_TYPES_CONNECT_TIMEOUT_MS
            readTimeout = ACHIEVEMENT_TYPES_READ_TIMEOUT_MS
        }
    }

    override suspend fun getUserCompletionProgress(
        username: String,
        offset: Int,
        count: Int,
    ): RetroAchievementsApiResult<UserCompletionProgressPage> {
        // api-kotlin's RetroInterface declares this as (username, count, offset) - confirmed via
        // javap's RuntimeVisibleParameterAnnotations showing @Query("c") on parameter 1 and
        // @Query("o") on parameter 2, not the (username, offset, count) order this method's own
        // parameter list uses. Passing count/offset positionally in our own declared order sent
        // count=<offset> and offset=<count> to the real API - e.g. the first page's count=0
        // silently returned zero results (with a correct `total`), so every fetch "succeeded"
        // with an empty page and pagination stopped immediately, permanently caching an empty
        // progress map. Confirmed as the root cause of the system-view Progress filter showing
        // "No games found" for every non-None/AllGames bucket despite the user having real
        // progress. Do not "simplify" this back to positional (username, offset, count).
        return api.getUserCompletionProgress(username, count, offset).toApiResult { it.toPage() }
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

/**
 * Achievement ID -> raw `type` string, parsed directly from a `GetGameExtended` JSON body -
 * see [RetroClientRetroAchievementsApi.fetchAchievementTypesById]'s kdoc for why this bypasses
 * api-kotlin's own (broken) deserialization for this one field. A missing/absent `type` key
 * (the normal case for a standard, unclassified achievement) maps to `null`, not an error.
 */
private fun parseAchievementTypesById(body: String): Map<Long, String?> {
    val achievements = JSONObject(body).optJSONObject("Achievements") ?: return emptyMap()
    val typesById = mutableMapOf<Long, String?>()
    achievements.keys().forEach { key ->
        val achievement = achievements.getJSONObject(key)
        val id = achievement.optLong("ID")
        val type = achievement.takeIf { it.has("type") && !it.isNull("type") }?.getString("type")
        typesById[id] = type
    }
    return typesById
}

private fun org.retroachivements.api.data.pojo.system.GetGameList.Response.Game.toCandidateGame() =
    RetroAchievementsCandidateGame(
        gameId = id,
        title = title,
        iconUrl = "https://i.retroachievements.org$imageIcon",
        numAchievements = numAchievements,
        totalPoints = points.toInt(),
        hashes = hashes.orEmpty(),
    )

private fun GetGameInfoAndUserProgress.Response.toSummary(typesById: Map<Long, String?>): GameAchievementSummary {
    val achievementItems = achievements.values.map { it.toAchievementItem(typesById) }
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
        // numDistinctPlayers and playersTotal are interchangeable in practice; prefer whichever
        // is actually populated rather than trusting one field name absolutely.
        totalPlayers = numDistinctPlayers.toInt().takeIf { it > 0 } ?: playersTotal,
    )
}

private typealias RaAchievement = GetGameInfoAndUserProgress.Response.Achievement

private fun RaAchievement.toAchievementItem(typesById: Map<Long, String?>): AchievementItem {
    val achievementId = id.toLongOrNull() ?: 0L
    val unlockedAtTimestamp = dateEarnedHardcore ?: dateEarned
    return AchievementItem(
        id = achievementId,
        title = title,
        description = description,
        points = points.toInt(),
        badgeUrl = "$MEDIA_BASE_URL/Badge/$badgeName.png",
        unlocked = unlockedAtTimestamp != null,
        unlockedAt = unlockedAtTimestamp?.let(::parseRaTimestamp),
        displayOrder = displayOrder,
        numAwarded = numAwarded.toInt(),
        numAwardedHardcore = numAwardedHardcore.toInt(),
        type = typesById[achievementId].toAchievementType(),
    )
}

private fun GetUserCompletionProgress.Response.toPage(): UserCompletionProgressPage {
    val entries = results.map { it.toUserGameProgress() }
    return UserCompletionProgressPage(total.toInt(), entries)
}

/**
 * [highestAwardKind] is typed non-null on the compiled class but Gson can populate it with
 * `null` via reflection - laundering it through this explicitly nullable local before passing
 * it to [progressStatusFor] is mandatory, not defensive style, per the confirmed crash risk
 * (see the plan's Verified Facts).
 */
private fun GetUserCompletionProgress.Progress.toUserGameProgress(): UserGameProgress {
    val awardKind: String? = highestAwardKind
    val numAwardedInt = numAwarded.toInt()
    return UserGameProgress(
        gameId = gameId,
        numAwarded = numAwardedInt,
        maxPossible = maxPossible.toInt(),
        status = progressStatusFor(awardKind, numAwardedInt),
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

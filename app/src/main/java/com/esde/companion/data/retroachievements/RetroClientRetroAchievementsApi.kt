package com.esde.companion.data.retroachievements

import com.esde.companion.domain.model.AchievementComment
import com.esde.companion.domain.model.AchievementItem
import com.esde.companion.domain.model.GameAchievementSummary
import com.esde.companion.domain.model.GameLeaderboardsSummary
import com.esde.companion.domain.model.GamePlaytimeStats
import com.esde.companion.domain.model.LeaderboardEntry
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
import org.retroachivements.api.data.pojo.comments.GetComments
import org.retroachivements.api.data.pojo.game.GetGameInfoAndUserProgress
import org.retroachivements.api.data.pojo.game.GetGameProgression
import org.retroachivements.api.data.pojo.game.GetUserGameLeaderboard
import org.retroachivements.api.data.pojo.user.GetUserCompletionProgress
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
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

// Single page only - generous enough for a typical achievement's wall, no "load more" UI yet.
// Revisit if a heavily-commented achievement ever needs pagination.
private const val ACHIEVEMENT_COMMENTS_PAGE_SIZE = 50

// Single page only, same "no load more UI yet" convention as ACHIEVEMENT_COMMENTS_PAGE_SIZE -
// generous enough to cover a leaderboard's top entries for the accordion drill-down.
private const val LEADERBOARD_ENTRIES_PAGE_SIZE = 100

// RA's automated audit-log account - see getAchievementComments' kdoc for why its entries are filtered out.
private const val RA_SERVER_USERNAME = "Server"

private typealias AchievementCommentsResult = RetroAchievementsApiResult<List<AchievementComment>>
private typealias LeaderboardEntriesResult = RetroAchievementsApiResult<List<LeaderboardEntry>>
private typealias RaNetworkResponse<T> = NetworkResponse<T, ErrorResponse>

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
            val progressDeferred =
                async { retryOnceOnError { api.getGameInfoAndUserProgress(username, gameId, includeUserAward = 1) } }
            val typesDeferred = async { fetchAchievementTypesById(gameId) }
            // Community-wide median beat/complete/master times (see GamePlaytimeStats' kdoc) -
            // a separate, unrelated-to-the-user endpoint, so a failure here degrades to "no
            // playtime stats line" rather than failing the whole achievement summary, same
            // graceful-degradation shape getGameLeaderboards' userEntriesDeferred uses. Retried
            // once on error for the same reason progressDeferred is (see retryOnceOnError's
            // kdoc) - this call fires as part of the same concurrent burst, confirmed on-device
            // as the cause of a widget intermittently showing "no time to beat data" for a game
            // that has it, recovering as soon as anything (a manual refresh, or simply
            // revisiting later) re-fetches outside that burst.
            val playtimeDeferred = async { retryOnceOnError { api.getGameProgression(gameId) } }
            val progressResult = progressDeferred.await()
            val typesById = typesDeferred.await()
            val playtimeStats = (playtimeDeferred.await() as? NetworkResponse.Success)?.body?.toPlaytimeStats()
            progressResult.toApiResult { it.toSummary(typesById, playtimeStats) }
        }

    /**
     * Retries [call] once on any [NetworkResponse.Error] - confirmed on-device as the fix for
     * "Couldn't load achievements: ...Use JsonReader.setLenient(true) to accept malformed
     * JSON..." surfacing after fast-scrolling ES-DE's game list and settling on a not-yet-cached
     * game. That message is cnradapter's [NetworkResponse.Error] wrapping a genuine (not merely
     * cancelled) response whose body failed to parse as JSON - i.e. a real, completed request
     * got back a bad body, not a stale request racing a live one (the debounce in
     * [RetroAchievementsViewModel] already handles that case). A single settle now fires several
     * concurrent RA requests at once (`GetGameInfoAndUserProgress`, [fetchAchievementTypesById],
     * [GamePlaytimeStats]' progression fetch, and - one level up - two leaderboard calls), and
     * this symptom reads like an occasional connection-reuse/timing hiccup under that burst
     * rather than a persistent failure - a debounced settle represents one deliberate user
     * action, so it shouldn't read as "no achievements"/"no playtime data" when a second attempt
     * reliably succeeds.
     */
    private suspend fun <T> retryOnceOnError(call: suspend () -> RaNetworkResponse<T>): RaNetworkResponse<T> {
        val first = call()
        return if (first is NetworkResponse.Error) call() else first
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

    /**
     * api-kotlin's `getCommentsOnAchievementWall` declares its parameters as `(achievementId,
     * count, offset, type, sort)` - count before offset - the exact same trap that already bit
     * [getUserCompletionProgress] above (see that method's kdoc). Named arguments only; do not
     * "simplify" this to positional args.
     *
     * RA's achievement wall mixes real user comments with automated audit-log entries posted
     * under a `Server` account (e.g. "X uploaded this achievement.", edit/promotion notices) -
     * confirmed via this endpoint's own mock response data. Those aren't user comments, so they're
     * filtered out here rather than left for the UI to reason about.
     *
     * `sort` defaults to `"submitted"` (oldest first, confirmed via this endpoint's own mock
     * response data - a 2020 comment ahead of a 2024 one). Explicitly requesting `"-submitted"`
     * both orders the UI newest-first and - since `count` caps the page at
     * [ACHIEVEMENT_COMMENTS_PAGE_SIZE] with no "load more" - ensures a wall with more comments
     * than that page size actually surfaces its most recent comments instead of silently
     * truncating to only the oldest ones.
     */
    override suspend fun getAchievementComments(achievementId: Long): AchievementCommentsResult {
        val response =
            api.getCommentsOnAchievementWall(
                achievementId = achievementId,
                count = ACHIEVEMENT_COMMENTS_PAGE_SIZE,
                offset = 0,
                sort = "-submitted",
            )
        return response.toApiResult { comments ->
            comments.results
                .filterNot { it.user.equals(RA_SERVER_USERNAME, ignoreCase = true) }
                .map { comment -> comment.toAchievementComment() }
        }
    }

    /**
     * Merges the game's public leaderboard list with the signed-in user's own entries via two
     * concurrent calls, the same [coroutineScope]/[async] shape [getGameInfoAndUserProgress] uses
     * to merge achievement-type data. A failure fetching the user's own entries degrades to "no
     * `myEntry` for any row" rather than failing the whole leaderboard list - the same
     * graceful-degradation shape [fetchAchievementTypesById] already uses for its own secondary
     * merge.
     */
    override suspend fun getGameLeaderboards(gameId: Long): RetroAchievementsApiResult<GameLeaderboardsSummary> =
        coroutineScope {
            val leaderboardsDeferred = async { api.getGameLeaderboards(gameId) }
            val userEntriesDeferred = async { api.getUserGameLeaderboards(gameId, credentials.username) }
            val leaderboardsResult = leaderboardsDeferred.await()
            // userEntry is typed non-null on the compiled class, but the same "declared non-null,
            // actually null via Gson" risk applies here as GetGameLeaderboards.Leaderboard.topEntry
            // (see toLeaderboardSummary's kdoc) - laundering through an explicit nullable local
            // and mapNotNull, rather than trusting the declared type, avoids the same crash shape.
            val myEntriesById =
                (userEntriesDeferred.await() as? NetworkResponse.Success)
                    ?.body
                    ?.results
                    .orEmpty()
                    .mapNotNull { result ->
                        val userEntry: GetUserGameLeaderboard.Response.Result.UserEntry? = result.userEntry
                        userEntry?.let { result.id.toLong() to it.toUserEntry() }
                    }
                    .toMap()
            leaderboardsResult.toApiResult { it.toGameLeaderboardsSummary(gameId, myEntriesById) }
        }

    override suspend fun getLeaderboardEntries(leaderboardId: Long): LeaderboardEntriesResult =
        api.getLeaderboardEntries(leaderboardId, count = LEADERBOARD_ENTRIES_PAGE_SIZE)
            .toApiResult { response -> response.results.map { it.toLeaderboardEntry() } }
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

private fun GetGameInfoAndUserProgress.Response.toSummary(
    typesById: Map<Long, String?>,
    playtimeStats: GamePlaytimeStats?,
): GameAchievementSummary {
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
        playtimeStats = playtimeStats,
    )
}

/**
 * A milestone's median is only meaningful once at least one player has actually reached it -
 * RA's `TimesUsedIn*Median` counters report exactly that, so a zero count degrades that one
 * field to `null` ("no data yet") rather than a misleading `0h 0m`.
 */
private fun GetGameProgression.Response.toPlaytimeStats(): GamePlaytimeStats =
    GamePlaytimeStats(
        beatSeconds = medianTimeToBeat.takeIf { timesUsedInBeatMedian > 0 },
        beatHardcoreSeconds = medianTimeToBeatHardcore.takeIf { timesUsedInHardcoreBeatMedia > 0 },
        // mediaTimeToComplete (not "median") is api-kotlin's own field name - a library typo, not ours.
        completedSeconds = mediaTimeToComplete.takeIf { timesUsedInCompletionMedian > 0 },
        masteredSeconds = medianTimeToMaster.takeIf { timesUsedInMasteryMedian > 0 },
    )

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

private fun GetComments.Result.toAchievementComment() =
    AchievementComment(
        user = user,
        submittedAtMillis = parseCommentTimestamp(submitted),
        text = commentText,
    )

/**
 * Unlike achievement-unlock timestamps ([parseRaTimestamp]'s ad-hoc `yyyy-MM-dd HH:mm:ss`
 * format), `GetComments`' `Submitted` field is real ISO-8601 (e.g.
 * `2020-02-18T06:04:01.000000Z`) - [Instant.parse] handles the arbitrary fractional-second
 * digits directly, no custom formatter needed.
 */
private fun parseCommentTimestamp(value: String): Long? =
    try {
        Instant.parse(value).toEpochMilli()
    } catch (
        @Suppress("SwallowedException") e: DateTimeParseException,
    ) {
        null
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
internal fun parseRaTimestamp(value: String): Long? =
    try {
        LocalDateTime.parse(value, RA_TIMESTAMP_FORMAT).toInstant(ZoneOffset.UTC).toEpochMilli()
    } catch (
        @Suppress("SwallowedException") e: DateTimeParseException,
    ) {
        null
    }

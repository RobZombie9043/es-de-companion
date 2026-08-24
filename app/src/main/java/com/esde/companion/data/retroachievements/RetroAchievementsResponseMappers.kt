package com.esde.companion.data.retroachievements

import com.esde.companion.domain.model.GameLeaderboardsSummary
import com.esde.companion.domain.model.LeaderboardEntry
import com.esde.companion.domain.model.LeaderboardSummary
import com.esde.companion.domain.model.LeaderboardTopEntry
import com.esde.companion.domain.model.LeaderboardUserEntry
import com.haroldadmin.cnradapter.NetworkResponse
import org.retroachivements.api.data.pojo.ErrorResponse
import org.retroachivements.api.data.pojo.game.GetGameLeaderboards
import org.retroachivements.api.data.pojo.game.GetLeaderboardEntries
import org.retroachivements.api.data.pojo.game.GetUserGameLeaderboard
import org.retroachivements.api.data.pojo.user.GetUserSummary

/**
 * Small response-to-domain-model mapping helpers used by [RetroClientRetroAchievementsApi] -
 * pulled out to their own file (rather than staying as members of that class) so that class/file
 * stay under detekt's `TooManyFunctions` thresholds. None of these read [RetroClientRetroAchievementsApi]'s
 * own `credentials`/`api` fields, so moving them out is behavior-preserving.
 */
internal fun GetUserSummary.Response.toUserSummary() =
    RetroAchievementsUserSummary(
        username = user,
        points = totalPoints?.toInt() ?: 0,
        avatarUrl = userPic?.let { "https://i.retroachievements.org$it" },
    )

internal fun <S, T> NetworkResponse<S, ErrorResponse>.toApiResult(map: (S) -> T): RetroAchievementsApiResult<T> =
    when (this) {
        is NetworkResponse.Success -> RetroAchievementsApiResult.Success(map(body))
        is NetworkResponse.Error ->
            RetroAchievementsApiResult.Error(
                body?.message ?: error?.message ?: "RetroAchievements request failed",
            )
    }

/**
 * [displayOrder] is assigned from this leaderboard's index in RA's own returned order - see
 * [GameLeaderboardsSummary]'s kdoc.
 */
internal fun GetGameLeaderboards.Response.toGameLeaderboardsSummary(
    gameId: Long,
    myEntriesById: Map<Long, LeaderboardUserEntry>,
): GameLeaderboardsSummary {
    val leaderboards =
        results.mapIndexed { displayOrder, leaderboard ->
            leaderboard.toLeaderboardSummary(displayOrder, myEntriesById[leaderboard.id])
        }
    return GameLeaderboardsSummary(gameId = gameId, leaderboards = leaderboards)
}

/**
 * [topEntry] is typed non-null on the compiled class but Gson leaves it `null` for a leaderboard
 * nobody has submitted a score to yet (confirmed via a live NPE crash on `topEntry.getUser()`) -
 * laundering it through this explicitly nullable local before use is mandatory, not defensive
 * style, same as [com.esde.companion.domain.model.progressStatusFor]'s `highestAwardKind`
 * laundering. `user`/`formattedScore` are laundered the same way via [String.orEmpty] - calling
 * an extension declared on a nullable receiver skips Kotlin's null-check insertion, so this stays
 * safe even if those individual fields turn out to be `null` too despite their own non-null type.
 */
internal fun GetGameLeaderboards.Leaderboard.toLeaderboardSummary(
    displayOrder: Int,
    myEntry: LeaderboardUserEntry?,
): LeaderboardSummary {
    val rawTopEntry: GetGameLeaderboards.TopEntry? = topEntry
    val mappedTopEntry =
        rawTopEntry?.let { LeaderboardTopEntry(user = it.user.orEmpty(), formattedScore = it.formattedScore.orEmpty()) }
    return LeaderboardSummary(
        id = id,
        title = title,
        description = description,
        displayOrder = displayOrder,
        topEntry = mappedTopEntry,
        myEntry = myEntry,
    )
}

internal fun GetUserGameLeaderboard.Response.Result.UserEntry.toUserEntry() =
    LeaderboardUserEntry(rank = rank.toLong(), formattedScore = formattedScore.orEmpty())

/**
 * [dateSubmitted]'s exact format isn't yet confirmed against the live API (unlike
 * [org.retroachivements.api.data.pojo.comments.GetComments.Result.submitted], which is confirmed
 * real ISO-8601) - [parseRaTimestamp] is tried first since that's the format every other RA
 * timestamp field in [RetroClientRetroAchievementsApi] uses, and yields `null` rather than
 * crashing if that assumption turns out to be wrong for this field. Revisit once confirmed
 * against a live response.
 */
internal fun GetLeaderboardEntries.Entry.toLeaderboardEntry() =
    LeaderboardEntry(
        rank = rank,
        user = user,
        formattedScore = formattedScore,
        submittedAtMillis = parseRaTimestamp(dateSubmitted),
    )

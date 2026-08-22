package com.esde.companion.domain.model

/**
 * One RetroAchievements leaderboard as it appears in a game's leaderboard list. [displayOrder] is
 * this leaderboard's index in RA's own returned order - the API has no explicit display-order
 * field the way achievements do, so the fetched list's own order is the closest available proxy
 * for "the order RA itself presents these in".
 *
 * [topEntry]/[myEntry] carry only pre-formatted display data (user/score), not raw numeric score -
 * RA already reports [LeaderboardUserEntry.rank] as the correct 1-is-best ordering regardless of
 * whether the underlying leaderboard sorts ascending (e.g. speedrun time) or descending (e.g.
 * points), so no additional direction handling is needed here.
 */
data class LeaderboardSummary(
    val id: Long,
    val title: String,
    val description: String,
    val displayOrder: Int,
    val topEntry: LeaderboardTopEntry?,
    val myEntry: LeaderboardUserEntry?,
)

/** The current #1 entry on a [LeaderboardSummary]. */
data class LeaderboardTopEntry(
    val user: String,
    val formattedScore: String,
)

/** The signed-in user's own entry on a [LeaderboardSummary], if they have one. */
data class LeaderboardUserEntry(
    val rank: Long,
    val formattedScore: String,
)

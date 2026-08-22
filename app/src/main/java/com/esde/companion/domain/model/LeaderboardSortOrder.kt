package com.esde.companion.domain.model

/**
 * Sort options for a game's leaderboard list. Unlike [AchievementSortOrder], there is no
 * Missable/Locked-style filter and no selectable display field for leaderboards - a leaderboard
 * row's right-side content (top entry, my entry) is fixed, not user-selectable.
 */
enum class LeaderboardSortOrder {
    DisplayOrderFirst,
    DisplayOrderLast,
    TitleAToZ,
    TitleZToA,
    MyRankBest,
    MyRankWorst,
}

/**
 * Pure sort - see [LeaderboardSortOrder]. [MyRankBest]/[MyRankWorst] always sort leaderboards with
 * no entry for the signed-in user to the end regardless of direction:
 * `compareBy { it.myEntry == null }` groups "has an entry" (`false`) before "no entry" (`true`),
 * so the rank comparison that follows only ever orders within the has-entry group.
 */
fun List<LeaderboardSummary>.sortedByLeaderboardOrder(order: LeaderboardSortOrder): List<LeaderboardSummary> =
    when (order) {
        LeaderboardSortOrder.DisplayOrderFirst -> sortedBy { it.displayOrder }
        LeaderboardSortOrder.DisplayOrderLast -> sortedByDescending { it.displayOrder }
        LeaderboardSortOrder.TitleAToZ -> sortedBy { it.title.lowercase() }
        LeaderboardSortOrder.TitleZToA -> sortedByDescending { it.title.lowercase() }
        LeaderboardSortOrder.MyRankBest ->
            sortedWith(
                compareBy<LeaderboardSummary> { it.myEntry == null }
                    .thenBy { it.myEntry?.rank ?: Long.MAX_VALUE },
            )
        LeaderboardSortOrder.MyRankWorst ->
            sortedWith(
                compareBy<LeaderboardSummary> { it.myEntry == null }
                    .thenByDescending { it.myEntry?.rank ?: Long.MIN_VALUE },
            )
    }

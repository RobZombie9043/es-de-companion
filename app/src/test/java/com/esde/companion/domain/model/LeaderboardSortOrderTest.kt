package com.esde.companion.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class LeaderboardSortOrderTest {
    private fun leaderboard(
        id: Long,
        title: String,
        displayOrder: Int = 0,
        myRank: Long? = null,
    ) = LeaderboardSummary(
        id = id,
        title = title,
        description = "",
        displayOrder = displayOrder,
        topEntry = null,
        myEntry = myRank?.let { LeaderboardUserEntry(rank = it, formattedScore = "") },
    )

    @Test
    fun `DisplayOrderFirst and DisplayOrderLast are opposite orderings`() {
        val a = leaderboard(1, "A", displayOrder = 2)
        val b = leaderboard(2, "B", displayOrder = 0)
        val c = leaderboard(3, "C", displayOrder = 1)
        val leaderboards = listOf(a, b, c)

        assertEquals(listOf(b, c, a), leaderboards.sortedByLeaderboardOrder(LeaderboardSortOrder.DisplayOrderFirst))
        assertEquals(listOf(a, c, b), leaderboards.sortedByLeaderboardOrder(LeaderboardSortOrder.DisplayOrderLast))
    }

    @Test
    fun `TitleAToZ and TitleZToA sort case-insensitively`() {
        val a = leaderboard(1, "banana")
        val b = leaderboard(2, "Apple")
        val leaderboards = listOf(a, b)

        assertEquals(listOf(b, a), leaderboards.sortedByLeaderboardOrder(LeaderboardSortOrder.TitleAToZ))
        assertEquals(listOf(a, b), leaderboards.sortedByLeaderboardOrder(LeaderboardSortOrder.TitleZToA))
    }

    @Test
    fun `MyRankBest and MyRankWorst order by rank among ranked leaderboards`() {
        val first = leaderboard(1, "First", myRank = 1)
        val tenth = leaderboard(2, "Tenth", myRank = 10)
        val fiftieth = leaderboard(3, "Fiftieth", myRank = 50)
        val leaderboards = listOf(fiftieth, first, tenth)

        assertEquals(
            listOf(first, tenth, fiftieth),
            leaderboards.sortedByLeaderboardOrder(LeaderboardSortOrder.MyRankBest),
        )
        assertEquals(
            listOf(fiftieth, tenth, first),
            leaderboards.sortedByLeaderboardOrder(LeaderboardSortOrder.MyRankWorst),
        )
    }

    @Test
    fun `MyRankBest and MyRankWorst both push leaderboards with no entry for the user to the end`() {
        val ranked = leaderboard(1, "Ranked", myRank = 5)
        val unranked = leaderboard(2, "Unranked", myRank = null)
        val leaderboards = listOf(unranked, ranked)

        assertEquals(
            listOf(ranked, unranked),
            leaderboards.sortedByLeaderboardOrder(LeaderboardSortOrder.MyRankBest),
        )
        assertEquals(
            listOf(ranked, unranked),
            leaderboards.sortedByLeaderboardOrder(LeaderboardSortOrder.MyRankWorst),
        )
    }
}

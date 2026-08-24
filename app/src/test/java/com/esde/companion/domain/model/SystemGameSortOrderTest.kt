package com.esde.companion.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SystemGameSortOrderTest {
    private fun game(
        gameId: Long,
        title: String,
        numAchievements: Int = 0,
        totalPoints: Int = 0,
    ) = RetroAchievementsCandidateGame(gameId, title, iconUrl = null, numAchievements, totalPoints)

    private fun progress(
        gameId: Long,
        numAwarded: Int,
        maxPossible: Int,
    ) = UserGameProgress(gameId, numAwarded, maxPossible, ProgressStatus.Some)

    @Test
    fun `AchievementsMost and AchievementsLeast sort by achievement count`() {
        val games = listOf(game(1, "A", numAchievements = 5), game(2, "B", numAchievements = 10))

        val most = games.sortedBySystemGameOrder(SystemGameSortOrder.AchievementsMost, emptyMap())
        val least = games.sortedBySystemGameOrder(SystemGameSortOrder.AchievementsLeast, emptyMap())

        assertEquals(listOf(games[1], games[0]), most)
        assertEquals(listOf(games[0], games[1]), least)
    }

    @Test
    fun `PointsMost and PointsLeast sort by total points`() {
        val games = listOf(game(1, "A", totalPoints = 100), game(2, "B", totalPoints = 400))

        val most = games.sortedBySystemGameOrder(SystemGameSortOrder.PointsMost, emptyMap())
        val least = games.sortedBySystemGameOrder(SystemGameSortOrder.PointsLeast, emptyMap())

        assertEquals(listOf(games[1], games[0]), most)
        assertEquals(listOf(games[0], games[1]), least)
    }

    @Test
    fun `ProgressMost and ProgressLeast sort by completion percent, absent games sorting as 0 percent`() {
        val halfDone = game(1, "Half")
        val untouched = game(2, "Untouched")
        val fullyDone = game(3, "Full")
        val progressByGameId =
            mapOf(
                halfDone.gameId to progress(1, numAwarded = 5, maxPossible = 10),
                fullyDone.gameId to progress(3, numAwarded = 10, maxPossible = 10),
            )
        val games = listOf(halfDone, untouched, fullyDone)

        val most = games.sortedBySystemGameOrder(SystemGameSortOrder.ProgressMost, progressByGameId)
        val least = games.sortedBySystemGameOrder(SystemGameSortOrder.ProgressLeast, progressByGameId)

        assertEquals(listOf(fullyDone, halfDone, untouched), most)
        assertEquals(listOf(untouched, halfDone, fullyDone), least)
    }

    @Test
    fun `TitleAToZ and TitleZToA sort case-insensitively`() {
        val games = listOf(game(1, "zelda"), game(2, "Apple"))

        val aToZ = games.sortedBySystemGameOrder(SystemGameSortOrder.TitleAToZ, emptyMap())
        val zToA = games.sortedBySystemGameOrder(SystemGameSortOrder.TitleZToA, emptyMap())

        assertEquals(listOf(games[1], games[0]), aToZ)
        assertEquals(listOf(games[0], games[1]), zToA)
    }

    @Test
    fun `ties on the primary sort key fall back to a deterministic title tie-break`() {
        val games = listOf(game(1, "Zebra", numAchievements = 10), game(2, "Apple", numAchievements = 10))

        val result = games.sortedBySystemGameOrder(SystemGameSortOrder.AchievementsMost, emptyMap())

        assertEquals(listOf(games[1], games[0]), result)
    }
}

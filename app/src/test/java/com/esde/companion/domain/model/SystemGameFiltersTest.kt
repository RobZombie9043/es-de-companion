package com.esde.companion.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SystemGameFiltersTest {
    private fun game(
        gameId: Long,
        title: String,
        numAchievements: Int = 0,
    ) = RetroAchievementsCandidateGame(gameId, title, iconUrl = null, numAchievements)

    private fun progress(
        gameId: Long,
        status: ProgressStatus,
    ) = UserGameProgress(gameId, numAwarded = 0, maxPossible = 0, status = status)

    @Test
    fun `an empty Game Type set matches every game`() {
        val games = listOf(game(1, "A"), game(2, "B"))
        val filters = SystemGameFilters(hasAchievements = HasAchievementsFilter.Both)

        val result = games.filteredBySystemGameFilters(filters, emptyMap(), emptyMap())

        assertEquals(games, result)
    }

    @Test
    fun `Game Type matches OR within the facet - any selected tag matches, not all`() {
        val hackOnly = game(1, "Hack")
        val demoOnly = game(2, "Demo")
        val retail = game(3, "Retail")
        val gameTypesByGameId =
            mapOf(
                hackOnly.gameId to setOf(RetroGameType.Hack),
                demoOnly.gameId to setOf(RetroGameType.Demo),
                retail.gameId to setOf(RetroGameType.Retail),
            )
        val filters =
            SystemGameFilters(
                gameTypes = setOf(RetroGameType.Hack, RetroGameType.Demo),
                hasAchievements = HasAchievementsFilter.Both,
            )

        val result =
            listOf(hackOnly, demoOnly, retail).filteredBySystemGameFilters(filters, gameTypesByGameId, emptyMap())

        assertEquals(listOf(hackOnly, demoOnly), result)
    }

    @Test
    fun `HasAchievementsFilter Yes keeps only games with achievements`() {
        val withAchievements = game(1, "A", numAchievements = 5)
        val withoutAchievements = game(2, "B", numAchievements = 0)
        val games = listOf(withAchievements, withoutAchievements)
        val filters = SystemGameFilters(hasAchievements = HasAchievementsFilter.Yes)

        val result = games.filteredBySystemGameFilters(filters, emptyMap(), emptyMap())

        assertEquals(listOf(withAchievements), result)
    }

    @Test
    fun `HasAchievementsFilter No keeps only games without achievements`() {
        val withAchievements = game(1, "A", numAchievements = 5)
        val withoutAchievements = game(2, "B", numAchievements = 0)
        val games = listOf(withAchievements, withoutAchievements)
        val filters = SystemGameFilters(hasAchievements = HasAchievementsFilter.No)

        val result = games.filteredBySystemGameFilters(filters, emptyMap(), emptyMap())

        assertEquals(listOf(withoutAchievements), result)
    }

    @Test
    fun `HasAchievementsFilter Both keeps every game regardless of achievement count`() {
        val withAchievements = game(1, "A", numAchievements = 5)
        val withoutAchievements = game(2, "B", numAchievements = 0)
        val games = listOf(withAchievements, withoutAchievements)
        val filters = SystemGameFilters(hasAchievements = HasAchievementsFilter.Both)

        val result = games.filteredBySystemGameFilters(filters, emptyMap(), emptyMap())

        assertEquals(listOf(withAchievements, withoutAchievements), result)
    }

    @Test
    fun `ProgressFilter buckets match a game's recorded status, and absence maps to None`() {
        val mastered = game(1, "Mastered")
        val untouched = game(2, "Untouched")
        val progressByGameId = mapOf(mastered.gameId to progress(1, ProgressStatus.Mastered))
        val games = listOf(mastered, untouched)
        val hasAchievements = HasAchievementsFilter.Both

        val masteredResult =
            games.filteredBySystemGameFilters(
                SystemGameFilters(progress = ProgressFilter.Mastered, hasAchievements = hasAchievements),
                emptyMap(),
                progressByGameId,
            )
        val noneResult =
            games.filteredBySystemGameFilters(
                SystemGameFilters(progress = ProgressFilter.None, hasAchievements = hasAchievements),
                emptyMap(),
                progressByGameId,
            )

        assertEquals(listOf(mastered), masteredResult)
        assertEquals(listOf(untouched), noneResult)
    }

    @Test
    fun `ProgressFilter AllGames ignores status entirely`() {
        val games = listOf(game(1, "A"), game(2, "B"))
        val filters =
            SystemGameFilters(progress = ProgressFilter.AllGames, hasAchievements = HasAchievementsFilter.Both)

        val result = games.filteredBySystemGameFilters(filters, emptyMap(), emptyMap())

        assertEquals(games, result)
    }

    @Test
    fun `combined filters apply as an AND across facets`() {
        val matches = game(1, "Match", numAchievements = 5)
        val wrongType = game(2, "WrongType", numAchievements = 5)
        val gameTypesByGameId =
            mapOf(matches.gameId to setOf(RetroGameType.Hack), wrongType.gameId to setOf(RetroGameType.Retail))
        val filters =
            SystemGameFilters(gameTypes = setOf(RetroGameType.Hack), hasAchievements = HasAchievementsFilter.Yes)

        val result = listOf(matches, wrongType).filteredBySystemGameFilters(filters, gameTypesByGameId, emptyMap())

        assertEquals(listOf(matches), result)
    }
}

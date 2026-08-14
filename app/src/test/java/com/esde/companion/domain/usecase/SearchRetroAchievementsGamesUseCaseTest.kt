package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.AchievementSummaryFetchResult
import com.esde.companion.domain.model.RetroAchievementsAuthState
import com.esde.companion.domain.model.RetroAchievementsCandidateGame
import com.esde.companion.domain.model.RetroAchievementsConsole
import com.esde.companion.domain.model.RetroAchievementsCredentials
import com.esde.companion.domain.repository.RetroAchievementsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchRetroAchievementsGamesUseCaseTest {
    private class FakeRetroAchievementsRepository(
        private val candidates: List<RetroAchievementsCandidateGame>,
    ) : RetroAchievementsRepository {
        override suspend fun validateCredentials(c: RetroAchievementsCredentials): RetroAchievementsAuthState {
            error("not used in this test")
        }

        override suspend fun getCandidateGames(console: RetroAchievementsConsole) = candidates

        override suspend fun getAchievementSummary(
            gameId: Long,
            forceRefresh: Boolean,
        ): AchievementSummaryFetchResult {
            error("not used in this test")
        }

        override suspend fun getUserGameProgress() = error("not used in this test")
    }

    private fun candidate(
        gameId: Long,
        title: String,
    ) = RetroAchievementsCandidateGame(gameId, title, iconUrl = null)

    @Test
    fun `an unmapped system returns an empty list rather than throwing`() =
        runTest {
            val useCase = SearchRetroAchievementsGamesUseCase(FakeRetroAchievementsRepository(emptyList()))

            val result = useCase("some-unmapped-system", query = "", gameName = "Anything")

            assertEquals(emptyList<RetroAchievementsCandidateGame>(), result)
        }

    @Test
    fun `a blank query ranks every candidate by similarity to the current game name`() =
        runTest {
            val closeMatch = candidate(1, "Moto Racer Advance")
            val repository = FakeRetroAchievementsRepository(listOf(candidate(2, "Chrono Trigger"), closeMatch))
            val useCase = SearchRetroAchievementsGamesUseCase(repository)

            val result = useCase("snes", query = "", gameName = "Motorace Advance")

            assertEquals(closeMatch, result.first())
        }

    @Test
    fun `a non-blank query filters by substring instead of ranking by similarity`() =
        runTest {
            val repository =
                FakeRetroAchievementsRepository(listOf(candidate(1, "Chrono Trigger"), candidate(2, "Chrono Cross")))
            val useCase = SearchRetroAchievementsGamesUseCase(repository)

            val result = useCase("snes", query = "trigger", gameName = "Chrono Trigger")

            assertEquals(listOf(candidate(1, "Chrono Trigger")), result)
        }

    @Test
    fun `a non-blank query is case-insensitive`() =
        runTest {
            val repository = FakeRetroAchievementsRepository(listOf(candidate(1, "Chrono Trigger")))
            val useCase = SearchRetroAchievementsGamesUseCase(repository)

            val result = useCase("snes", query = "CHRONO", gameName = "")

            assertTrue(result.isNotEmpty())
        }
}

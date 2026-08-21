package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.AchievementSummaryFetchResult
import com.esde.companion.domain.model.RetroAchievementsAuthState
import com.esde.companion.domain.model.RetroAchievementsCandidateGame
import com.esde.companion.domain.model.RetroAchievementsConsole
import com.esde.companion.domain.model.RetroAchievementsCredentials
import com.esde.companion.domain.repository.RetroAchievementsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetRetroAchievementsSystemGamesUseCaseTest {
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

        override suspend fun getAchievementComments(achievementId: Long) = error("not used in this test")
    }

    private fun candidate(
        gameId: Long,
        title: String,
    ) = RetroAchievementsCandidateGame(gameId, title, iconUrl = null)

    @Test
    fun `an unmapped system returns null rather than an empty list`() =
        runTest {
            val useCase = GetRetroAchievementsSystemGamesUseCase(FakeRetroAchievementsRepository(emptyList()))

            val result = useCase("some-unmapped-system")

            assertNull(result)
        }

    @Test
    fun `a mapped system with no games returns an empty, non-null list`() =
        runTest {
            val useCase = GetRetroAchievementsSystemGamesUseCase(FakeRetroAchievementsRepository(emptyList()))

            val result = useCase("snes")

            assertEquals(emptyList<RetroAchievementsCandidateGame>(), result)
        }

    @Test
    fun `games are returned sorted alphabetically by title, case-insensitively`() =
        runTest {
            val candidates = listOf(candidate(1, "zelda"), candidate(2, "Chrono Trigger"), candidate(3, "apple"))
            val useCase = GetRetroAchievementsSystemGamesUseCase(FakeRetroAchievementsRepository(candidates))

            val result = useCase("snes")

            assertEquals(listOf(candidates[2], candidates[1], candidates[0]), result)
        }
}

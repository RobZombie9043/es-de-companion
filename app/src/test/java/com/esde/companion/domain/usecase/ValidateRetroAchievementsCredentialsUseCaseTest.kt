package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.AchievementSummaryFetchResult
import com.esde.companion.domain.model.RetroAchievementsAuthState
import com.esde.companion.domain.model.RetroAchievementsCandidateGame
import com.esde.companion.domain.model.RetroAchievementsConsole
import com.esde.companion.domain.model.RetroAchievementsCredentials
import com.esde.companion.domain.repository.RetroAchievementsCredentialsRepository
import com.esde.companion.domain.repository.RetroAchievementsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ValidateRetroAchievementsCredentialsUseCaseTest {
    private class FakeRetroAchievementsRepository(
        private val result: RetroAchievementsAuthState,
    ) : RetroAchievementsRepository {
        override suspend fun validateCredentials(creds: RetroAchievementsCredentials): RetroAchievementsAuthState {
            return result
        }

        override suspend fun getCandidateGames(c: RetroAchievementsConsole): List<RetroAchievementsCandidateGame> {
            error("not used in this test")
        }

        override suspend fun getAchievementSummary(
            gameId: Long,
            forceRefresh: Boolean,
        ): AchievementSummaryFetchResult {
            error("not used in this test")
        }

        override suspend fun getUserGameProgress() = error("not used in this test")

        override suspend fun getAchievementComments(achievementId: Long) = error("not used in this test")

        override suspend fun getGameLeaderboards(
            gameId: Long,
            forceRefresh: Boolean,
        ) = error("not used in this test")

        override suspend fun getLeaderboardEntries(leaderboardId: Long) = error("not used in this test")
    }

    private class FakeRetroAchievementsCredentialsRepository : RetroAchievementsCredentialsRepository {
        var stored: RetroAchievementsCredentials? = null

        override suspend fun setCredentials(credentials: RetroAchievementsCredentials) {
            stored = credentials
        }

        override suspend fun clearCredentials() {
            stored = null
        }

        override fun observeCredentials(): Flow<RetroAchievementsCredentials?> = flowOf(stored)
    }

    private val credentials = RetroAchievementsCredentials(username = "player1", webApiKey = "secret-key")

    @Test
    fun `a successful validation persists the credentials`() =
        runTest {
            val credentialsRepository = FakeRetroAchievementsCredentialsRepository()
            val useCase =
                ValidateRetroAchievementsCredentialsUseCase(
                    FakeRetroAchievementsRepository(RetroAchievementsAuthState.SignedIn("player1", 1000, null)),
                    credentialsRepository,
                )

            val result = useCase(credentials)

            assertEquals(RetroAchievementsAuthState.SignedIn("player1", 1000, null), result)
            assertEquals(credentials, credentialsRepository.stored)
        }

    @Test
    fun `a failed validation does not persist the credentials`() =
        runTest {
            val credentialsRepository = FakeRetroAchievementsCredentialsRepository()
            val useCase =
                ValidateRetroAchievementsCredentialsUseCase(
                    FakeRetroAchievementsRepository(RetroAchievementsAuthState.Error("Invalid Web API Key")),
                    credentialsRepository,
                )

            val result = useCase(credentials)

            assertEquals(RetroAchievementsAuthState.Error("Invalid Web API Key"), result)
            assertNull(credentialsRepository.stored)
        }
}

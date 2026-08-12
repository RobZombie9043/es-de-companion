package com.esde.companion.data.retroachievements

import com.esde.companion.domain.model.AchievementSummaryFetchResult
import com.esde.companion.domain.model.GameAchievementSummary
import com.esde.companion.domain.model.RetroAchievementsAuthState
import com.esde.companion.domain.model.RetroAchievementsCandidateGame
import com.esde.companion.domain.model.RetroAchievementsConsole
import com.esde.companion.domain.model.RetroAchievementsCredentials
import com.esde.companion.domain.repository.RetroAchievementsCredentialsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RetroAchievementsRepositoryImplTest {
    private class FakeRetroAchievementsApi(
        private val userSummaryResult: RetroAchievementsApiResult<RetroAchievementsUserSummary>? = null,
        private val gameListResult: RetroAchievementsApiResult<List<RetroAchievementsCandidateGame>>? = null,
        private val summaryResult: RetroAchievementsApiResult<GameAchievementSummary>? = null,
    ) : RetroAchievementsApi {
        var requestedUsername: String? = null

        override suspend fun getUserSummary(u: String): RetroAchievementsApiResult<RetroAchievementsUserSummary> {
            requestedUsername = u
            return userSummaryResult ?: error("not used in this test")
        }

        override suspend fun getGameList(consoleId: Long) = gameListResult ?: error("not used in this test")

        override suspend fun getGameInfoAndUserProgress(
            username: String,
            gameId: Long,
        ): RetroAchievementsApiResult<GameAchievementSummary> {
            requestedUsername = username
            return summaryResult ?: error("not used in this test")
        }
    }

    private class FakeGameListCacheStore : GameListCacheStore {
        override suspend fun read(consoleId: Long): CachedGameList? = null

        override suspend fun write(
            consoleId: Long,
            cached: CachedGameList,
        ) = Unit
    }

    private class FakeCredentialsRepository(
        private val credentials: RetroAchievementsCredentials?,
    ) : RetroAchievementsCredentialsRepository {
        override suspend fun setCredentials(credentials: RetroAchievementsCredentials) = error("not used in this test")

        override suspend fun clearCredentials() = error("not used in this test")

        override fun observeCredentials(): Flow<RetroAchievementsCredentials?> = flowOf(credentials)
    }

    private val credentials = RetroAchievementsCredentials(username = "player1", webApiKey = "secret-key")

    private fun repositoryWith(
        api: RetroAchievementsApi,
        signedInAs: RetroAchievementsCredentials? = credentials,
    ) = RetroAchievementsRepositoryImpl(
        credentialsRepository = FakeCredentialsRepository(signedInAs),
        gameListCache = GameListCache(FakeGameListCacheStore()),
        apiFactory = { api },
    )

    @Test
    fun `a successful user summary maps to SignedIn with the username, points, and full avatar url`() =
        runTest {
            val avatarUrl = "https://i.retroachievements.org/UserPic/player1.png"
            val summary = RetroAchievementsUserSummary(username = "player1", points = 1234, avatarUrl = avatarUrl)
            val fakeApi = FakeRetroAchievementsApi(userSummaryResult = RetroAchievementsApiResult.Success(summary))
            val repository = repositoryWith(fakeApi)

            val result = repository.validateCredentials(credentials)

            assertEquals(RetroAchievementsAuthState.SignedIn("player1", 1234, avatarUrl), result)
        }

    @Test
    fun `a null avatar url maps to a null avatarUrl rather than a malformed one`() =
        runTest {
            val summary = RetroAchievementsUserSummary(username = "player1", points = 0, avatarUrl = null)
            val fakeApi = FakeRetroAchievementsApi(userSummaryResult = RetroAchievementsApiResult.Success(summary))
            val repository = repositoryWith(fakeApi)

            val result = repository.validateCredentials(credentials)

            assertEquals(RetroAchievementsAuthState.SignedIn("player1", 0, null), result)
        }

    @Test
    fun `an api error maps to Error with the same message, and never a SignedIn state`() =
        runTest {
            val errorResult = RetroAchievementsApiResult.Error("Invalid API Key")
            val fakeApi = FakeRetroAchievementsApi(userSummaryResult = errorResult)
            val repository = repositoryWith(fakeApi)

            val result = repository.validateCredentials(credentials)

            assertEquals(RetroAchievementsAuthState.Error("Invalid API Key"), result)
        }

    @Test
    fun `validateCredentials queries the user summary for the candidate username, not any previously stored one`() =
        runTest {
            val summary = RetroAchievementsUserSummary(username = "player1", points = 0, avatarUrl = null)
            val fakeApi = FakeRetroAchievementsApi(userSummaryResult = RetroAchievementsApiResult.Success(summary))
            val repository = repositoryWith(fakeApi)

            repository.validateCredentials(credentials)

            assertEquals("player1", fakeApi.requestedUsername)
        }

    @Test
    fun `getCandidateGames returns an empty list when nobody is signed in`() =
        runTest {
            val repository = repositoryWith(FakeRetroAchievementsApi(), signedInAs = null)

            val result = repository.getCandidateGames(RetroAchievementsConsole.Snes)

            assertEquals(emptyList<RetroAchievementsCandidateGame>(), result)
        }

    @Test
    fun `getCandidateGames delegates to the game list cache using the signed-in credentials`() =
        runTest {
            val games = listOf(RetroAchievementsCandidateGame(1L, "Chrono Trigger", iconUrl = null))
            val fakeApi = FakeRetroAchievementsApi(gameListResult = RetroAchievementsApiResult.Success(games))
            val repository = repositoryWith(fakeApi)

            val result = repository.getCandidateGames(RetroAchievementsConsole.Snes)

            assertEquals(games, result)
        }

    @Test
    fun `getAchievementSummary reports NotFound when nobody is signed in`() =
        runTest {
            val repository = repositoryWith(FakeRetroAchievementsApi(), signedInAs = null)

            val result = repository.getAchievementSummary(gameId = 1L)

            assertEquals(AchievementSummaryFetchResult.NotFound, result)
        }

    @Test
    fun `getAchievementSummary maps a successful fetch to Success and queries as the signed-in username`() =
        runTest {
            val summary = GameAchievementSummary(1L, "Chrono Trigger", 100, 0, 0f, achievements = emptyList())
            val fakeApi = FakeRetroAchievementsApi(summaryResult = RetroAchievementsApiResult.Success(summary))
            val repository = repositoryWith(fakeApi)

            val result = repository.getAchievementSummary(gameId = 1L)

            assertEquals(AchievementSummaryFetchResult.Success(summary), result)
            assertEquals("player1", fakeApi.requestedUsername)
        }

    @Test
    fun `getAchievementSummary maps an api error to NetworkError with the same message`() =
        runTest {
            val fakeApi = FakeRetroAchievementsApi(summaryResult = RetroAchievementsApiResult.Error("offline"))
            val repository = repositoryWith(fakeApi)

            val result = repository.getAchievementSummary(gameId = 1L)

            assertEquals(AchievementSummaryFetchResult.NetworkError("offline"), result)
        }
}

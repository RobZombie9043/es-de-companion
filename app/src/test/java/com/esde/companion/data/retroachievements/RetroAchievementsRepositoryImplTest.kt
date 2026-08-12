package com.esde.companion.data.retroachievements

import com.esde.companion.domain.model.RetroAchievementsAuthState
import com.esde.companion.domain.model.RetroAchievementsCredentials
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RetroAchievementsRepositoryImplTest {
    private class FakeRetroAchievementsApi(
        private val userSummaryResult: RetroAchievementsApiResult<RetroAchievementsUserSummary>,
    ) : RetroAchievementsApi {
        var requestedUsername: String? = null

        override suspend fun getUserSummary(u: String): RetroAchievementsApiResult<RetroAchievementsUserSummary> {
            requestedUsername = u
            return userSummaryResult
        }

        override suspend fun getGameList(consoleId: Long) = error("not used in this test")

        override suspend fun getGameInfoAndUserProgress(
            username: String,
            gameId: Long,
        ) = error("not used in this test")
    }

    private val credentials = RetroAchievementsCredentials(username = "player1", webApiKey = "secret-key")

    @Test
    fun `a successful user summary maps to SignedIn with the username, points, and full avatar url`() =
        runTest {
            val avatarUrl = "https://i.retroachievements.org/UserPic/player1.png"
            val summary = RetroAchievementsUserSummary(username = "player1", points = 1234, avatarUrl = avatarUrl)
            val fakeApi = FakeRetroAchievementsApi(RetroAchievementsApiResult.Success(summary))
            val repository = RetroAchievementsRepositoryImpl(apiFactory = { fakeApi })

            val result = repository.validateCredentials(credentials)

            assertEquals(RetroAchievementsAuthState.SignedIn("player1", 1234, avatarUrl), result)
        }

    @Test
    fun `a null avatar url maps to a null avatarUrl rather than a malformed one`() =
        runTest {
            val summary = RetroAchievementsUserSummary(username = "player1", points = 0, avatarUrl = null)
            val fakeApi = FakeRetroAchievementsApi(RetroAchievementsApiResult.Success(summary))
            val repository = RetroAchievementsRepositoryImpl(apiFactory = { fakeApi })

            val result = repository.validateCredentials(credentials)

            assertEquals(RetroAchievementsAuthState.SignedIn("player1", 0, null), result)
        }

    @Test
    fun `an api error maps to Error with the same message, and never a SignedIn state`() =
        runTest {
            val fakeApi = FakeRetroAchievementsApi(RetroAchievementsApiResult.Error("Invalid API Key"))
            val repository = RetroAchievementsRepositoryImpl(apiFactory = { fakeApi })

            val result = repository.validateCredentials(credentials)

            assertEquals(RetroAchievementsAuthState.Error("Invalid API Key"), result)
        }

    @Test
    fun `validateCredentials queries the user summary for the candidate username, not any previously stored one`() =
        runTest {
            val summary = RetroAchievementsUserSummary(username = "player1", points = 0, avatarUrl = null)
            val fakeApi = FakeRetroAchievementsApi(RetroAchievementsApiResult.Success(summary))
            val repository = RetroAchievementsRepositoryImpl(apiFactory = { fakeApi })

            repository.validateCredentials(credentials)

            assertEquals("player1", fakeApi.requestedUsername)
        }
}

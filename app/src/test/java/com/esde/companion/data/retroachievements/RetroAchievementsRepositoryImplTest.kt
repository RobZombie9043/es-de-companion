package com.esde.companion.data.retroachievements

import com.esde.companion.domain.model.AchievementComment
import com.esde.companion.domain.model.AchievementCommentsFetchResult
import com.esde.companion.domain.model.AchievementSummaryFetchResult
import com.esde.companion.domain.model.GameAchievementSummary
import com.esde.companion.domain.model.GameLeaderboardsSummary
import com.esde.companion.domain.model.LeaderboardEntriesFetchResult
import com.esde.companion.domain.model.LeaderboardEntry
import com.esde.companion.domain.model.LeaderboardsFetchResult
import com.esde.companion.domain.model.ProgressStatus
import com.esde.companion.domain.model.RetroAchievementsAuthState
import com.esde.companion.domain.model.RetroAchievementsCandidateGame
import com.esde.companion.domain.model.RetroAchievementsConsole
import com.esde.companion.domain.model.RetroAchievementsCredentials
import com.esde.companion.domain.model.UserGameProgress
import com.esde.companion.domain.repository.RetroAchievementsCredentialsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private typealias CommentsApiResult = RetroAchievementsApiResult<List<AchievementComment>>
private typealias LeaderboardsApiResult = RetroAchievementsApiResult<GameLeaderboardsSummary>
private typealias LeaderboardEntriesApiResult = RetroAchievementsApiResult<List<LeaderboardEntry>>

class RetroAchievementsRepositoryImplTest {
    private class FakeRetroAchievementsApi(
        private val userSummaryResult: RetroAchievementsApiResult<RetroAchievementsUserSummary>? = null,
        private val gameListResult: RetroAchievementsApiResult<List<RetroAchievementsCandidateGame>>? = null,
        private val summaryResult: RetroAchievementsApiResult<GameAchievementSummary>? = null,
        private val progressResult: RetroAchievementsApiResult<UserCompletionProgressPage>? = null,
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

        override suspend fun getUserCompletionProgress(
            username: String,
            offset: Int,
            count: Int,
        ): RetroAchievementsApiResult<UserCompletionProgressPage> {
            requestedUsername = username
            return progressResult ?: error("not used in this test")
        }

        override suspend fun getAchievementComments(achievementId: Long) = error("not used in this test")

        override suspend fun getGameLeaderboards(gameId: Long) = error("not used in this test")

        override suspend fun getLeaderboardEntries(leaderboardId: Long) = error("not used in this test")
    }

    private class CountingGameInfoApi(
        private val summaryResult: RetroAchievementsApiResult<GameAchievementSummary>,
    ) : RetroAchievementsApi {
        var callCount = 0
            private set

        override suspend fun getUserSummary(username: String) = error("not used in this test")

        override suspend fun getGameList(consoleId: Long) = error("not used in this test")

        override suspend fun getGameInfoAndUserProgress(
            username: String,
            gameId: Long,
        ): RetroAchievementsApiResult<GameAchievementSummary> {
            callCount++
            return summaryResult
        }

        override suspend fun getUserCompletionProgress(
            username: String,
            offset: Int,
            count: Int,
        ) = error("not used in this test")

        override suspend fun getAchievementComments(achievementId: Long) = error("not used in this test")

        override suspend fun getGameLeaderboards(gameId: Long) = error("not used in this test")

        override suspend fun getLeaderboardEntries(leaderboardId: Long) = error("not used in this test")
    }

    private class FakeCommentsApi(
        private val commentsResult: CommentsApiResult,
    ) : RetroAchievementsApi {
        var callCount = 0
            private set

        override suspend fun getUserSummary(username: String) = error("not used in this test")

        override suspend fun getGameList(consoleId: Long) = error("not used in this test")

        override suspend fun getGameInfoAndUserProgress(
            username: String,
            gameId: Long,
        ) = error("not used in this test")

        override suspend fun getUserCompletionProgress(
            username: String,
            offset: Int,
            count: Int,
        ) = error("not used in this test")

        override suspend fun getAchievementComments(achievementId: Long): CommentsApiResult {
            callCount++
            return commentsResult
        }

        override suspend fun getGameLeaderboards(gameId: Long) = error("not used in this test")

        override suspend fun getLeaderboardEntries(leaderboardId: Long) = error("not used in this test")
    }

    private class FakeLeaderboardsApi(
        private val leaderboardsResult: LeaderboardsApiResult,
    ) : RetroAchievementsApi {
        var callCount = 0
            private set

        override suspend fun getUserSummary(username: String) = error("not used in this test")

        override suspend fun getGameList(consoleId: Long) = error("not used in this test")

        override suspend fun getGameInfoAndUserProgress(
            username: String,
            gameId: Long,
        ) = error("not used in this test")

        override suspend fun getUserCompletionProgress(
            username: String,
            offset: Int,
            count: Int,
        ) = error("not used in this test")

        override suspend fun getAchievementComments(achievementId: Long) = error("not used in this test")

        override suspend fun getGameLeaderboards(gameId: Long): LeaderboardsApiResult {
            callCount++
            return leaderboardsResult
        }

        override suspend fun getLeaderboardEntries(leaderboardId: Long) = error("not used in this test")
    }

    private class FakeLeaderboardEntriesApi(
        private val entriesResult: LeaderboardEntriesApiResult,
    ) : RetroAchievementsApi {
        var callCount = 0
            private set

        override suspend fun getUserSummary(username: String) = error("not used in this test")

        override suspend fun getGameList(consoleId: Long) = error("not used in this test")

        override suspend fun getGameInfoAndUserProgress(
            username: String,
            gameId: Long,
        ) = error("not used in this test")

        override suspend fun getUserCompletionProgress(
            username: String,
            offset: Int,
            count: Int,
        ) = error("not used in this test")

        override suspend fun getAchievementComments(achievementId: Long) = error("not used in this test")

        override suspend fun getGameLeaderboards(gameId: Long) = error("not used in this test")

        override suspend fun getLeaderboardEntries(leaderboardId: Long): LeaderboardEntriesApiResult {
            callCount++
            return entriesResult
        }
    }

    private class FakeGameListCacheStore : GameListCacheStore {
        override suspend fun read(consoleId: Long): CachedGameList? = null

        override suspend fun write(
            consoleId: Long,
            cached: CachedGameList,
        ) = Unit
    }

    private class FakeUserProgressCacheStore : UserProgressCacheStore {
        override suspend fun read(username: String): CachedUserProgress? = null

        override suspend fun write(
            username: String,
            cached: CachedUserProgress,
        ) = Unit
    }

    private class FakeAchievementSummaryCacheStore : AchievementSummaryCacheStore {
        var stored: CachedAchievementSummary? = null

        override suspend fun read(
            username: String,
            gameId: Long,
        ): CachedAchievementSummary? = stored

        override suspend fun write(
            username: String,
            gameId: Long,
            cached: CachedAchievementSummary,
        ) {
            stored = cached
        }
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
    ): RetroAchievementsRepositoryImpl {
        val caches =
            RetroAchievementsCaches(
                gameList = GameListCache(FakeGameListCacheStore()),
                userProgress = UserProgressCache(FakeUserProgressCacheStore()),
                achievementSummary = AchievementSummaryCache(FakeAchievementSummaryCacheStore()),
                achievementComments = AchievementCommentsCache(),
                gameLeaderboards = GameLeaderboardsCache(),
                leaderboardEntries = LeaderboardEntriesCache(),
            )
        return RetroAchievementsRepositoryImpl(
            credentialsRepository = FakeCredentialsRepository(signedInAs),
            caches = caches,
            apiFactory = { api },
        )
    }

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

    @Test
    fun `getAchievementSummary caches a successful fetch and does not re-query the api on a second call`() =
        runTest {
            val summary = GameAchievementSummary(1L, "Chrono Trigger", 100, 0, 0f, achievements = emptyList())
            val fakeApi =
                CountingGameInfoApi(RetroAchievementsApiResult.Success(summary))
            val repository = repositoryWith(fakeApi)

            repository.getAchievementSummary(gameId = 1L)
            repository.getAchievementSummary(gameId = 1L)

            assertEquals(1, fakeApi.callCount)
        }

    @Test
    fun `getAchievementSummary with forceRefresh re-queries the api even when a fresh cache entry exists`() =
        runTest {
            val summary = GameAchievementSummary(1L, "Chrono Trigger", 100, 0, 0f, achievements = emptyList())
            val fakeApi =
                CountingGameInfoApi(RetroAchievementsApiResult.Success(summary))
            val repository = repositoryWith(fakeApi)

            repository.getAchievementSummary(gameId = 1L)
            repository.getAchievementSummary(gameId = 1L, forceRefresh = true)

            assertEquals(2, fakeApi.callCount)
        }

    @Test
    fun `peekAchievementSummary returns null when nobody is signed in`() =
        runTest {
            val repository = repositoryWith(FakeRetroAchievementsApi(), signedInAs = null)

            val result = repository.peekAchievementSummary(gameId = 1L)

            assertEquals(null, result)
        }

    @Test
    fun `peekAchievementSummary returns null when nothing has been fetched yet`() =
        runTest {
            val repository = repositoryWith(FakeRetroAchievementsApi())

            val result = repository.peekAchievementSummary(gameId = 1L)

            assertEquals(null, result)
        }

    @Test
    fun `peekAchievementSummary returns a prior fetch without querying the api`() =
        runTest {
            val summary = GameAchievementSummary(1L, "Chrono Trigger", 100, 0, 0f, achievements = emptyList())
            val fakeApi = CountingGameInfoApi(RetroAchievementsApiResult.Success(summary))
            val repository = repositoryWith(fakeApi)
            repository.getAchievementSummary(gameId = 1L)

            val result = repository.peekAchievementSummary(gameId = 1L)

            assertEquals(summary, result?.summary)
            assertEquals(false, result?.isStale)
            assertEquals(1, fakeApi.callCount)
        }

    @Test
    fun `getUserGameProgress returns an empty map when nobody is signed in`() =
        runTest {
            val repository = repositoryWith(FakeRetroAchievementsApi(), signedInAs = null)

            val result = repository.getUserGameProgress()

            assertEquals(emptyMap<Long, UserGameProgress>(), result)
        }

    @Test
    fun `getUserGameProgress delegates to the user progress cache using the signed-in credentials`() =
        runTest {
            val progress = UserGameProgress(gameId = 1L, numAwarded = 5, maxPossible = 10, status = ProgressStatus.Some)
            val page = UserCompletionProgressPage(total = 1, entries = listOf(progress))
            val fakeApi = FakeRetroAchievementsApi(progressResult = RetroAchievementsApiResult.Success(page))
            val repository = repositoryWith(fakeApi)

            val result = repository.getUserGameProgress()

            assertEquals(mapOf(1L to progress), result)
            assertEquals("player1", fakeApi.requestedUsername)
        }

    @Test
    fun `getAchievementComments reports an empty list when nobody is signed in`() =
        runTest {
            val repository = repositoryWith(FakeRetroAchievementsApi(), signedInAs = null)

            val result = repository.getAchievementComments(achievementId = 1L)

            assertEquals(AchievementCommentsFetchResult.Success(emptyList()), result)
        }

    @Test
    fun `getAchievementComments maps a successful fetch to Success`() =
        runTest {
            val comment = AchievementComment("player1", 0L, "gg")
            val fakeApi = FakeCommentsApi(RetroAchievementsApiResult.Success(listOf(comment)))
            val repository = repositoryWith(fakeApi)

            val result = repository.getAchievementComments(achievementId = 1L)

            assertEquals(AchievementCommentsFetchResult.Success(listOf(comment)), result)
        }

    @Test
    fun `getAchievementComments maps an api error to NetworkError with the same message`() =
        runTest {
            val fakeApi = FakeCommentsApi(RetroAchievementsApiResult.Error("offline"))
            val repository = repositoryWith(fakeApi)

            val result = repository.getAchievementComments(achievementId = 1L)

            assertEquals(AchievementCommentsFetchResult.NetworkError("offline"), result)
        }

    @Test
    fun `getAchievementComments caches a successful fetch and does not re-query the api on a second call`() =
        runTest {
            val comment = AchievementComment("player1", 0L, "gg")
            val fakeApi = FakeCommentsApi(RetroAchievementsApiResult.Success(listOf(comment)))
            val repository = repositoryWith(fakeApi)

            repository.getAchievementComments(achievementId = 1L)
            repository.getAchievementComments(achievementId = 1L)

            assertEquals(1, fakeApi.callCount)
        }

    @Test
    fun `getGameLeaderboards reports NotFound when nobody is signed in`() =
        runTest {
            val repository = repositoryWith(FakeRetroAchievementsApi(), signedInAs = null)

            val result = repository.getGameLeaderboards(gameId = 1L)

            assertEquals(LeaderboardsFetchResult.NotFound, result)
        }

    @Test
    fun `getGameLeaderboards maps a successful fetch to Success`() =
        runTest {
            val summary = GameLeaderboardsSummary(gameId = 1L, leaderboards = emptyList())
            val fakeApi = FakeLeaderboardsApi(RetroAchievementsApiResult.Success(summary))
            val repository = repositoryWith(fakeApi)

            val result = repository.getGameLeaderboards(gameId = 1L)

            assertEquals(LeaderboardsFetchResult.Success(summary), result)
        }

    @Test
    fun `getGameLeaderboards maps an api error to NetworkError with the same message`() =
        runTest {
            val fakeApi = FakeLeaderboardsApi(RetroAchievementsApiResult.Error("offline"))
            val repository = repositoryWith(fakeApi)

            val result = repository.getGameLeaderboards(gameId = 1L)

            assertEquals(LeaderboardsFetchResult.NetworkError("offline"), result)
        }

    @Test
    fun `getGameLeaderboards caches a successful fetch and does not re-query the api on a second call`() =
        runTest {
            val summary = GameLeaderboardsSummary(gameId = 1L, leaderboards = emptyList())
            val fakeApi = FakeLeaderboardsApi(RetroAchievementsApiResult.Success(summary))
            val repository = repositoryWith(fakeApi)

            repository.getGameLeaderboards(gameId = 1L)
            repository.getGameLeaderboards(gameId = 1L)

            assertEquals(1, fakeApi.callCount)
        }

    @Test
    fun `getGameLeaderboards with forceRefresh re-queries the api even when a fresh cache entry exists`() =
        runTest {
            val summary = GameLeaderboardsSummary(gameId = 1L, leaderboards = emptyList())
            val fakeApi = FakeLeaderboardsApi(RetroAchievementsApiResult.Success(summary))
            val repository = repositoryWith(fakeApi)

            repository.getGameLeaderboards(gameId = 1L)
            repository.getGameLeaderboards(gameId = 1L, forceRefresh = true)

            assertEquals(2, fakeApi.callCount)
        }

    @Test
    fun `peekGameLeaderboards returns null when nobody is signed in`() =
        runTest {
            val repository = repositoryWith(FakeRetroAchievementsApi(), signedInAs = null)

            val result = repository.peekGameLeaderboards(gameId = 1L)

            assertEquals(null, result)
        }

    @Test
    fun `peekGameLeaderboards returns null when nothing has been fetched yet`() =
        runTest {
            val repository = repositoryWith(FakeRetroAchievementsApi())

            val result = repository.peekGameLeaderboards(gameId = 1L)

            assertEquals(null, result)
        }

    @Test
    fun `peekGameLeaderboards returns a prior fetch without querying the api`() =
        runTest {
            val summary = GameLeaderboardsSummary(gameId = 1L, leaderboards = emptyList())
            val fakeApi = FakeLeaderboardsApi(RetroAchievementsApiResult.Success(summary))
            val repository = repositoryWith(fakeApi)
            repository.getGameLeaderboards(gameId = 1L)

            val result = repository.peekGameLeaderboards(gameId = 1L)

            assertEquals(summary, result?.summary)
            assertEquals(false, result?.isStale)
            assertEquals(1, fakeApi.callCount)
        }

    @Test
    fun `getLeaderboardEntries reports an empty list when nobody is signed in`() =
        runTest {
            val repository = repositoryWith(FakeRetroAchievementsApi(), signedInAs = null)

            val result = repository.getLeaderboardEntries(leaderboardId = 1L)

            assertEquals(LeaderboardEntriesFetchResult.Success(emptyList()), result)
        }

    @Test
    fun `getLeaderboardEntries maps a successful fetch to Success`() =
        runTest {
            val entry = LeaderboardEntry(rank = 1L, user = "player1", formattedScore = "100", submittedAtMillis = 0L)
            val fakeApi = FakeLeaderboardEntriesApi(RetroAchievementsApiResult.Success(listOf(entry)))
            val repository = repositoryWith(fakeApi)

            val result = repository.getLeaderboardEntries(leaderboardId = 1L)

            assertEquals(LeaderboardEntriesFetchResult.Success(listOf(entry)), result)
        }

    @Test
    fun `getLeaderboardEntries maps an api error to NetworkError with the same message`() =
        runTest {
            val fakeApi = FakeLeaderboardEntriesApi(RetroAchievementsApiResult.Error("offline"))
            val repository = repositoryWith(fakeApi)

            val result = repository.getLeaderboardEntries(leaderboardId = 1L)

            assertEquals(LeaderboardEntriesFetchResult.NetworkError("offline"), result)
        }

    @Test
    fun `getLeaderboardEntries caches a successful fetch and does not re-query the api on a second call`() =
        runTest {
            val entry = LeaderboardEntry(rank = 1L, user = "player1", formattedScore = "100", submittedAtMillis = 0L)
            val fakeApi = FakeLeaderboardEntriesApi(RetroAchievementsApiResult.Success(listOf(entry)))
            val repository = repositoryWith(fakeApi)

            repository.getLeaderboardEntries(leaderboardId = 1L)
            repository.getLeaderboardEntries(leaderboardId = 1L)

            assertEquals(1, fakeApi.callCount)
        }
}

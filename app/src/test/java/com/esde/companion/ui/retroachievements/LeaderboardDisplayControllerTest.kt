package com.esde.companion.ui.retroachievements

import app.cash.turbine.test
import com.esde.companion.domain.model.AchievementCommentsFetchResult
import com.esde.companion.domain.model.AchievementSummaryFetchResult
import com.esde.companion.domain.model.LeaderboardEntriesFetchResult
import com.esde.companion.domain.model.LeaderboardSortOrder
import com.esde.companion.domain.model.LeaderboardsFetchResult
import com.esde.companion.domain.model.RetroAchievementsAuthState
import com.esde.companion.domain.model.RetroAchievementsCandidateGame
import com.esde.companion.domain.model.RetroAchievementsConsole
import com.esde.companion.domain.model.RetroAchievementsCredentials
import com.esde.companion.domain.model.UserGameProgress
import com.esde.companion.domain.repository.RetroAchievementsRepository
import com.esde.companion.domain.usecase.GetLeaderboardEntriesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LeaderboardDisplayControllerTest {
    /**
     * Same rendezvous-[Channel] forcing-a-real-suspension-point pattern as
     * [AchievementDisplayControllerTest]'s equivalent.
     */
    private class FakeRetroAchievementsRepository : RetroAchievementsRepository {
        private val pendingEntries = Channel<LeaderboardEntriesFetchResult>()
        var requestedLeaderboardIds = mutableListOf<Long>()

        suspend fun completeNextEntries(result: LeaderboardEntriesFetchResult) = pendingEntries.send(result)

        override suspend fun validateCredentials(unused: RetroAchievementsCredentials): RetroAchievementsAuthState =
            error("not used by this test")

        override suspend fun getCandidateGames(unused: RetroAchievementsConsole): List<RetroAchievementsCandidateGame> =
            error("not used by this test")

        override suspend fun getAchievementSummary(
            gameId: Long,
            forceRefresh: Boolean,
        ): AchievementSummaryFetchResult = error("not used by this test")

        override suspend fun getUserGameProgress(): Map<Long, UserGameProgress> = error("not used by this test")

        override suspend fun getAchievementComments(achievementId: Long): AchievementCommentsFetchResult {
            error("not used by this test")
        }

        override suspend fun getGameLeaderboards(
            gameId: Long,
            forceRefresh: Boolean,
        ): LeaderboardsFetchResult = error("not used by this test")

        override suspend fun getLeaderboardEntries(leaderboardId: Long): LeaderboardEntriesFetchResult {
            requestedLeaderboardIds += leaderboardId
            return pendingEntries.receive()
        }
    }

    private fun buildController(
        repository: FakeRetroAchievementsRepository,
        scope: CoroutineScope,
    ) = LeaderboardDisplayController(GetLeaderboardEntriesUseCase(repository), scope)

    @Test
    fun `sort order setter round-trips`() =
        runTest(UnconfinedTestDispatcher()) {
            val controller = buildController(FakeRetroAchievementsRepository(), backgroundScope)

            controller.onSortOrderChanged(LeaderboardSortOrder.TitleAToZ)

            assertEquals(LeaderboardSortOrder.TitleAToZ, controller.sortOrder.value)
        }

    @Test
    fun `tapping a leaderboard expands it with loading then resolves`() =
        runTest(UnconfinedTestDispatcher()) {
            val repository = FakeRetroAchievementsRepository()
            val controller = buildController(repository, backgroundScope)

            controller.expanded.test {
                assertNull(awaitItem())

                controller.onLeaderboardTapped(42L)
                val expanding = awaitItem()
                assertEquals(42L, expanding?.leaderboardId)
                assertEquals(LeaderboardEntriesFetchState.Loading, expanding?.entries)

                repository.completeNextEntries(LeaderboardEntriesFetchResult.Success(emptyList()))
                val loaded = awaitItem()
                assertEquals(42L, loaded?.leaderboardId)
                assertEquals(LeaderboardEntriesFetchState.Loaded(emptyList()), loaded?.entries)
            }
        }

    @Test
    fun `tapping the same expanded leaderboard again collapses it`() =
        runTest(UnconfinedTestDispatcher()) {
            val repository = FakeRetroAchievementsRepository()
            val controller = buildController(repository, backgroundScope)

            controller.expanded.test {
                assertNull(awaitItem())

                controller.onLeaderboardTapped(1L)
                awaitItem() // Loading
                repository.completeNextEntries(LeaderboardEntriesFetchResult.Success(emptyList()))
                awaitItem() // Loaded

                controller.onLeaderboardTapped(1L)
                assertNull(awaitItem())
            }
        }

    @Test
    fun `a network error surfaces through the expanded entries state`() =
        runTest(UnconfinedTestDispatcher()) {
            val repository = FakeRetroAchievementsRepository()
            val controller = buildController(repository, backgroundScope)

            controller.expanded.test {
                assertNull(awaitItem())

                controller.onLeaderboardTapped(7L)
                awaitItem() // Loading
                repository.completeNextEntries(LeaderboardEntriesFetchResult.NetworkError("offline"))
                val resolved = awaitItem()
                assertEquals(LeaderboardEntriesFetchState.NetworkError("offline"), resolved?.entries)
            }
        }

    @Test
    fun `onTargetChanged clears the expanded entries`() =
        runTest(UnconfinedTestDispatcher()) {
            val repository = FakeRetroAchievementsRepository()
            val controller = buildController(repository, backgroundScope)

            controller.expanded.test {
                assertNull(awaitItem())

                controller.onLeaderboardTapped(3L)
                awaitItem() // Loading
                repository.completeNextEntries(LeaderboardEntriesFetchResult.Success(emptyList()))
                awaitItem() // Loaded

                controller.onTargetChanged()
                assertNull(awaitItem())
            }
        }
}

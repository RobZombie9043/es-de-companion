package com.esde.companion.ui.retroachievements

import app.cash.turbine.test
import com.esde.companion.domain.model.AchievementCommentsFetchResult
import com.esde.companion.domain.model.AchievementDisplayField
import com.esde.companion.domain.model.AchievementFilterOption
import com.esde.companion.domain.model.AchievementSortOrder
import com.esde.companion.domain.model.AchievementSummaryFetchResult
import com.esde.companion.domain.model.LeaderboardEntriesFetchResult
import com.esde.companion.domain.model.LeaderboardsFetchResult
import com.esde.companion.domain.model.RetroAchievementsAuthState
import com.esde.companion.domain.model.RetroAchievementsCandidateGame
import com.esde.companion.domain.model.RetroAchievementsConsole
import com.esde.companion.domain.model.RetroAchievementsCredentials
import com.esde.companion.domain.model.UserGameProgress
import com.esde.companion.domain.repository.RetroAchievementsRepository
import com.esde.companion.domain.usecase.GetAchievementCommentsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AchievementDisplayControllerTest {
    /**
     * [getAchievementComments] suspends on a rendezvous [Channel] until the test explicitly
     * completes it via [completeNextComments] - a plain immediately-returning fake would let
     * the controller's Loading -> Loaded transition happen synchronously inside the same
     * StateFlow notification that delivers Loading, which can race Turbine's collector past
     * the Loading state entirely (StateFlow only guarantees the latest value to a collector,
     * not every intermediate one). Forcing a real suspension point here guarantees the test
     * observes Loading before Loaded.
     */
    private class FakeRetroAchievementsRepository : RetroAchievementsRepository {
        private val pendingComments = Channel<AchievementCommentsFetchResult>()
        var requestedAchievementIds = mutableListOf<Long>()

        suspend fun completeNextComments(result: AchievementCommentsFetchResult) = pendingComments.send(result)

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
            requestedAchievementIds += achievementId
            return pendingComments.receive()
        }

        override suspend fun getGameLeaderboards(
            gameId: Long,
            forceRefresh: Boolean,
        ): LeaderboardsFetchResult = error("not used by this test")

        override suspend fun getLeaderboardEntries(leaderboardId: Long): LeaderboardEntriesFetchResult {
            error("not used by this test")
        }
    }

    private fun buildController(
        repository: FakeRetroAchievementsRepository,
        scope: CoroutineScope,
    ) = AchievementDisplayController(GetAchievementCommentsUseCase(repository), scope)

    @Test
    fun `sort filter and display field setters round-trip`() =
        runTest(UnconfinedTestDispatcher()) {
            val controller = buildController(FakeRetroAchievementsRepository(), backgroundScope)

            controller.onSortOrderChanged(AchievementSortOrder.PointsMost)
            controller.onFilterChanged(setOf(AchievementFilterOption.LockedOnly))
            controller.onDisplayFieldChanged(AchievementDisplayField.Points)

            assertEquals(AchievementSortOrder.PointsMost, controller.sortOrder.value)
            assertEquals(setOf(AchievementFilterOption.LockedOnly), controller.filter.value)
            assertEquals(AchievementDisplayField.Points, controller.displayField.value)
        }

    @Test
    fun `tapping an achievement expands it with loading then resolves`() =
        runTest(UnconfinedTestDispatcher()) {
            val repository = FakeRetroAchievementsRepository()
            val controller = buildController(repository, backgroundScope)

            controller.expanded.test {
                assertNull(awaitItem())

                controller.onAchievementTapped(42L)
                val expanding = awaitItem()
                assertEquals(42L, expanding?.achievementId)
                assertEquals(CommentsFetchState.Loading, expanding?.comments)

                repository.completeNextComments(AchievementCommentsFetchResult.Success(emptyList()))
                val loaded = awaitItem()
                assertEquals(42L, loaded?.achievementId)
                assertEquals(CommentsFetchState.Loaded(emptyList()), loaded?.comments)
            }
        }

    @Test
    fun `tapping the same expanded achievement again collapses it`() =
        runTest(UnconfinedTestDispatcher()) {
            val repository = FakeRetroAchievementsRepository()
            val controller = buildController(repository, backgroundScope)

            controller.expanded.test {
                assertNull(awaitItem())

                controller.onAchievementTapped(1L)
                awaitItem() // Loading
                repository.completeNextComments(AchievementCommentsFetchResult.Success(emptyList()))
                awaitItem() // Loaded

                controller.onAchievementTapped(1L)
                assertNull(awaitItem())
            }
        }

    @Test
    fun `a network error surfaces through the expanded comments state`() =
        runTest(UnconfinedTestDispatcher()) {
            val repository = FakeRetroAchievementsRepository()
            val controller = buildController(repository, backgroundScope)

            controller.expanded.test {
                assertNull(awaitItem())

                controller.onAchievementTapped(7L)
                awaitItem() // Loading
                repository.completeNextComments(AchievementCommentsFetchResult.NetworkError("offline"))
                val resolved = awaitItem()
                assertEquals(CommentsFetchState.NetworkError("offline"), resolved?.comments)
            }
        }

    @Test
    fun `onTargetChanged clears the expanded comments`() =
        runTest(UnconfinedTestDispatcher()) {
            val repository = FakeRetroAchievementsRepository()
            val controller = buildController(repository, backgroundScope)

            controller.expanded.test {
                assertNull(awaitItem())

                controller.onAchievementTapped(3L)
                awaitItem() // Loading
                repository.completeNextComments(AchievementCommentsFetchResult.Success(emptyList()))
                awaitItem() // Loaded

                controller.onTargetChanged()
                assertNull(awaitItem())
            }
        }
}

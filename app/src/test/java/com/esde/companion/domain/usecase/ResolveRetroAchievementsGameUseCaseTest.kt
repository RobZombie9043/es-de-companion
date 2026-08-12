package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.AchievementSummaryFetchResult
import com.esde.companion.domain.model.GameMatchOverride
import com.esde.companion.domain.model.GameReference
import com.esde.companion.domain.model.MatchMethod
import com.esde.companion.domain.model.RetroAchievementsAuthState
import com.esde.companion.domain.model.RetroAchievementsCandidateGame
import com.esde.companion.domain.model.RetroAchievementsConsole
import com.esde.companion.domain.model.RetroAchievementsCredentials
import com.esde.companion.domain.model.RetroAchievementsGameMatch
import com.esde.companion.domain.repository.GameMatchOverrideRepository
import com.esde.companion.domain.repository.RetroAchievementsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ResolveRetroAchievementsGameUseCaseTest {
    private class FakeGameMatchOverrideRepository(
        private val override: GameMatchOverride? = null,
    ) : GameMatchOverrideRepository {
        override suspend fun setOverride(override: GameMatchOverride): Unit = error("not used in this test")

        override suspend fun clearOverride(gameReference: GameReference): Unit = error("not used in this test")

        override suspend fun getOverride(gameReference: GameReference): GameMatchOverride? = override

        override fun observeAllOverrides(): Flow<List<GameMatchOverride>> = flowOf(emptyList())
    }

    private class FakeRetroAchievementsRepository(
        private val candidates: List<RetroAchievementsCandidateGame> = emptyList(),
    ) : RetroAchievementsRepository {
        override suspend fun validateCredentials(creds: RetroAchievementsCredentials): RetroAchievementsAuthState {
            error("not used in this test")
        }

        override suspend fun getCandidateGames(c: RetroAchievementsConsole): List<RetroAchievementsCandidateGame> {
            return candidates
        }

        override suspend fun getAchievementSummary(gameId: Long): AchievementSummaryFetchResult {
            error("not used in this test")
        }
    }

    private val gameReference = GameReference(systemShortName = "snes", romPath = "/roms/snes/Chrono Trigger (USA).sfc")

    @Test
    fun `an unsupported system is reported before checking any override or candidate list`() =
        runTest {
            val unsupportedReference = GameReference(systemShortName = "windows", romPath = "/roms/windows/game.exe")
            val useCase =
                ResolveRetroAchievementsGameUseCase(
                    FakeGameMatchOverrideRepository(),
                    FakeRetroAchievementsRepository(),
                )

            val result = useCase(unsupportedReference, gameName = "Some PC Game")

            assertEquals(RetroAchievementsGameMatch.UnsupportedSystem, result)
        }

    @Test
    fun `a stored override wins even when the title would also match automatically`() =
        runTest {
            val override = GameMatchOverride(gameReference.systemShortName, gameReference.romPath, raGameId = 99L)
            val candidates = listOf(RetroAchievementsCandidateGame(1L, "Chrono Trigger", iconUrl = null))
            val useCase =
                ResolveRetroAchievementsGameUseCase(
                    FakeGameMatchOverrideRepository(override),
                    FakeRetroAchievementsRepository(candidates),
                )

            val result = useCase(gameReference, gameName = "Chrono Trigger")

            assertEquals(RetroAchievementsGameMatch.Found(99L, MatchMethod.ManualOverride), result)
        }

    @Test
    fun `falls back to automatic title matching when no override is stored`() =
        runTest {
            val candidates = listOf(RetroAchievementsCandidateGame(1L, "Chrono Trigger", iconUrl = null))
            val useCase =
                ResolveRetroAchievementsGameUseCase(
                    FakeGameMatchOverrideRepository(override = null),
                    FakeRetroAchievementsRepository(candidates),
                )

            val result = useCase(gameReference, gameName = "Chrono Trigger")

            assertEquals(RetroAchievementsGameMatch.Found(1L, MatchMethod.ExactTitle), result)
        }

    @Test
    fun `no override and no title match reports NoMatch`() =
        runTest {
            val candidates = listOf(RetroAchievementsCandidateGame(1L, "Chrono Cross", iconUrl = null))
            val useCase =
                ResolveRetroAchievementsGameUseCase(
                    FakeGameMatchOverrideRepository(override = null),
                    FakeRetroAchievementsRepository(candidates),
                )

            val result = useCase(gameReference, gameName = "Chrono Trigger")

            assertEquals(RetroAchievementsGameMatch.NoMatch, result)
        }
}

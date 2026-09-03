package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.AchievementSummaryFetchResult
import com.esde.companion.domain.model.GameHashSupport
import com.esde.companion.domain.model.GameReference
import com.esde.companion.domain.model.GameRomHash
import com.esde.companion.domain.model.RetroAchievementsAuthState
import com.esde.companion.domain.model.RetroAchievementsCandidateGame
import com.esde.companion.domain.model.RetroAchievementsConsole
import com.esde.companion.domain.model.RetroAchievementsCredentials
import com.esde.companion.domain.repository.GameRomHashRepository
import com.esde.companion.domain.repository.RetroAchievementsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetGameHashSupportUseCaseTest {
    private class FakeRetroAchievementsRepository(
        private val candidates: List<RetroAchievementsCandidateGame> = emptyList(),
    ) : RetroAchievementsRepository {
        override suspend fun validateCredentials(creds: RetroAchievementsCredentials): RetroAchievementsAuthState {
            error("not used in this test")
        }

        override suspend fun getCandidateGames(c: RetroAchievementsConsole): List<RetroAchievementsCandidateGame> {
            return candidates
        }

        override suspend fun getAchievementSummary(
            gameId: Long,
            forceRefresh: Boolean,
        ): AchievementSummaryFetchResult {
            error("not used in this test")
        }

        override suspend fun peekAchievementSummary(gameId: Long) = error("not used in this test")

        override suspend fun getUserGameProgress() = error("not used in this test")

        override suspend fun getAchievementComments(achievementId: Long) = error("not used in this test")

        override suspend fun getGameLeaderboards(
            gameId: Long,
            forceRefresh: Boolean,
        ) = error("not used in this test")

        override suspend fun getLeaderboardEntries(leaderboardId: Long) = error("not used in this test")
    }

    private class FakeGameRomHashRepository(
        private val hash: String? = null,
    ) : GameRomHashRepository {
        override suspend fun resolveRomHash(
            systemShortName: String,
            romPath: String,
        ): GameRomHash = GameRomHash(value = hash)
    }

    private val gameReference = GameReference(systemShortName = "snes", romPath = "/roms/snes/Chrono Trigger (USA).sfc")

    private fun useCase(
        retroAchievementsRepository: RetroAchievementsRepository = FakeRetroAchievementsRepository(),
        gameRomHashRepository: GameRomHashRepository = FakeGameRomHashRepository(),
    ) = GetGameHashSupportUseCase(gameRomHashRepository, retroAchievementsRepository)

    @Test
    fun `returns the current hash alongside the resolved game's supported hashes`() =
        runTest {
            val candidate =
                RetroAchievementsCandidateGame(1L, "Chrono Trigger", iconUrl = null, hashes = listOf("aaaa", "bbbb"))
            val useCase =
                useCase(
                    retroAchievementsRepository = FakeRetroAchievementsRepository(listOf(candidate)),
                    gameRomHashRepository = FakeGameRomHashRepository(hash = "aaaa"),
                )

            val result = useCase(gameReference, gameId = 1L)

            assertEquals(GameHashSupport(currentHash = "aaaa", supportedHashes = listOf("aaaa", "bbbb")), result)
        }

    @Test
    fun `a null current hash is reported rather than throwing`() =
        runTest {
            val candidate =
                RetroAchievementsCandidateGame(1L, "Chrono Trigger", iconUrl = null, hashes = listOf("aaaa"))
            val useCase =
                useCase(
                    retroAchievementsRepository = FakeRetroAchievementsRepository(listOf(candidate)),
                    gameRomHashRepository = FakeGameRomHashRepository(hash = null),
                )

            val result = useCase(gameReference, gameId = 1L)

            assertEquals(GameHashSupport(currentHash = null, supportedHashes = listOf("aaaa")), result)
        }

    @Test
    fun `a gameId absent from the candidate list yields no supported hashes`() =
        runTest {
            val candidate =
                RetroAchievementsCandidateGame(1L, "Chrono Trigger", iconUrl = null, hashes = listOf("aaaa"))
            val useCase =
                useCase(
                    retroAchievementsRepository = FakeRetroAchievementsRepository(listOf(candidate)),
                    gameRomHashRepository = FakeGameRomHashRepository(hash = "aaaa"),
                )

            val result = useCase(gameReference, gameId = 999L)

            assertEquals(GameHashSupport(currentHash = "aaaa", supportedHashes = emptyList()), result)
        }

    @Test
    fun `an unmapped system returns no supported hashes rather than throwing`() =
        runTest {
            val unsupportedReference = GameReference(systemShortName = "windows", romPath = "/roms/windows/game.exe")
            val useCase = useCase(gameRomHashRepository = FakeGameRomHashRepository(hash = "aaaa"))

            val result = useCase(unsupportedReference, gameId = 1L)

            assertEquals(GameHashSupport(currentHash = "aaaa", supportedHashes = emptyList()), result)
        }
}

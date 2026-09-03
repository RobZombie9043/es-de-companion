package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.AchievementSummaryFetchResult
import com.esde.companion.domain.model.GameMatchOverride
import com.esde.companion.domain.model.GameReference
import com.esde.companion.domain.model.GameRomHash
import com.esde.companion.domain.model.MatchMethod
import com.esde.companion.domain.model.RetroAchievementsAuthState
import com.esde.companion.domain.model.RetroAchievementsCandidateGame
import com.esde.companion.domain.model.RetroAchievementsConsole
import com.esde.companion.domain.model.RetroAchievementsCredentials
import com.esde.companion.domain.model.RetroAchievementsGameMatch
import com.esde.companion.domain.repository.GameMatchOverrideRepository
import com.esde.companion.domain.repository.GameRomHashRepository
import com.esde.companion.domain.repository.RetroAchievementsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    private val noHashRepository = FakeGameRomHashRepository(hash = null)

    private fun useCase(
        overrideRepository: GameMatchOverrideRepository = FakeGameMatchOverrideRepository(),
        retroAchievementsRepository: RetroAchievementsRepository = FakeRetroAchievementsRepository(),
        gameRomHashRepository: GameRomHashRepository = noHashRepository,
        onResolved: (String) -> Unit = {},
    ) = ResolveRetroAchievementsGameUseCase(
        overrideRepository,
        retroAchievementsRepository,
        gameRomHashRepository,
        onResolved,
    )

    @Test
    fun `an unsupported system is reported before checking any override or candidate list`() =
        runTest {
            val unsupportedReference = GameReference(systemShortName = "windows", romPath = "/roms/windows/game.exe")
            val useCase = useCase()

            val result = useCase(unsupportedReference, gameName = "Some PC Game")

            assertEquals(RetroAchievementsGameMatch.UnsupportedSystem, result)
        }

    @Test
    fun `a stored override wins even when the title would also match automatically`() =
        runTest {
            val override = GameMatchOverride(gameReference.systemShortName, gameReference.romPath, raGameId = 99L)
            val candidates = listOf(RetroAchievementsCandidateGame(1L, "Chrono Trigger", iconUrl = null))
            val useCase =
                useCase(
                    overrideRepository = FakeGameMatchOverrideRepository(override),
                    retroAchievementsRepository = FakeRetroAchievementsRepository(candidates),
                )

            val result = useCase(gameReference, gameName = "Chrono Trigger")

            assertEquals(RetroAchievementsGameMatch.Found(99L, MatchMethod.ManualOverride), result)
        }

    @Test
    fun `falls back to automatic title matching when no override is stored`() =
        runTest {
            val candidates = listOf(RetroAchievementsCandidateGame(1L, "Chrono Trigger", iconUrl = null))
            val useCase = useCase(retroAchievementsRepository = FakeRetroAchievementsRepository(candidates))

            val result = useCase(gameReference, gameName = "Chrono Trigger")

            assertEquals(RetroAchievementsGameMatch.Found(1L, MatchMethod.ExactTitle), result)
        }

    @Test
    fun `falls back to matching the ROM filename when the scraped game name matches nothing`() =
        runTest {
            val reference = GameReference(systemShortName = "snes", romPath = "/roms/snes/Chrono Trigger (USA).sfc")
            val candidates = listOf(RetroAchievementsCandidateGame(1L, "Chrono Trigger", iconUrl = null))
            val useCase = useCase(retroAchievementsRepository = FakeRetroAchievementsRepository(candidates))

            // A scraped display name unrelated to the actual game - only the filename matches.
            val result = useCase(reference, gameName = "Some Unrelated Scraped Title")

            assertEquals(RetroAchievementsGameMatch.Found(1L, MatchMethod.NormalizedTitle), result)
        }

    @Test
    fun `a directory-as-launchable-unit ROM path with no file extension still yields a usable filename guess`() =
        runTest {
            val reference = GameReference(systemShortName = "psx", romPath = "/roms/psx/Chrono Trigger (USA)")
            val candidates = listOf(RetroAchievementsCandidateGame(1L, "Chrono Trigger", iconUrl = null))
            val useCase = useCase(retroAchievementsRepository = FakeRetroAchievementsRepository(candidates))

            val result = useCase(reference, gameName = "Some Unrelated Scraped Title")

            assertEquals(RetroAchievementsGameMatch.Found(1L, MatchMethod.NormalizedTitle), result)
        }

    @Test
    fun `no override and no title match reports NoMatch`() =
        runTest {
            val candidates = listOf(RetroAchievementsCandidateGame(1L, "Chrono Cross", iconUrl = null))
            val useCase = useCase(retroAchievementsRepository = FakeRetroAchievementsRepository(candidates))

            val result = useCase(gameReference, gameName = "Chrono Trigger")

            assertEquals(RetroAchievementsGameMatch.NoMatch, result)
        }

    @Test
    fun `a ROM hash match wins over an available title match`() =
        runTest {
            val titleMatch = RetroAchievementsCandidateGame(1L, "Chrono Trigger", iconUrl = null)
            val hashMatch =
                RetroAchievementsCandidateGame(2L, "Chrono Trigger (Alt Dump)", iconUrl = null, hashes = listOf("aaaa"))
            val useCase =
                useCase(
                    retroAchievementsRepository = FakeRetroAchievementsRepository(listOf(titleMatch, hashMatch)),
                    gameRomHashRepository = FakeGameRomHashRepository(hash = "aaaa"),
                )

            val result = useCase(gameReference, gameName = "Chrono Trigger")

            assertEquals(RetroAchievementsGameMatch.Found(2L, MatchMethod.RomHash), result)
        }

    @Test
    fun `an override wins even when a candidate's hash also matches`() =
        runTest {
            val override = GameMatchOverride(gameReference.systemShortName, gameReference.romPath, raGameId = 99L)
            val hashMatch =
                RetroAchievementsCandidateGame(2L, "Chrono Trigger", iconUrl = null, hashes = listOf("aaaa"))
            val useCase =
                useCase(
                    overrideRepository = FakeGameMatchOverrideRepository(override),
                    retroAchievementsRepository = FakeRetroAchievementsRepository(listOf(hashMatch)),
                    gameRomHashRepository = FakeGameRomHashRepository(hash = "aaaa"),
                )

            val result = useCase(gameReference, gameName = "Chrono Trigger")

            assertEquals(RetroAchievementsGameMatch.Found(99L, MatchMethod.ManualOverride), result)
        }

    @Test
    fun `a hash present but matching no candidate falls through to a correct title match`() =
        runTest {
            val titleMatch = RetroAchievementsCandidateGame(1L, "Chrono Trigger", iconUrl = null)
            val useCase =
                useCase(
                    retroAchievementsRepository = FakeRetroAchievementsRepository(listOf(titleMatch)),
                    gameRomHashRepository = FakeGameRomHashRepository(hash = "does-not-match-anything"),
                )

            val result = useCase(gameReference, gameName = "Chrono Trigger")

            assertEquals(RetroAchievementsGameMatch.Found(1L, MatchMethod.ExactTitle), result)
        }

    @Test
    fun `a hash present, no hash match, and no title match reports NoMatch`() =
        runTest {
            val candidates = listOf(RetroAchievementsCandidateGame(1L, "Chrono Cross", iconUrl = null))
            val useCase =
                useCase(
                    retroAchievementsRepository = FakeRetroAchievementsRepository(candidates),
                    gameRomHashRepository = FakeGameRomHashRepository(hash = "does-not-match-anything"),
                )

            val result = useCase(gameReference, gameName = "Chrono Trigger")

            assertEquals(RetroAchievementsGameMatch.NoMatch, result)
        }

    @Test
    fun `onResolved reports the match method and matched game for a hash match`() =
        runTest {
            val hashMatch =
                RetroAchievementsCandidateGame(2L, "Chrono Trigger", iconUrl = null, hashes = listOf("aaaa"))
            val messages = mutableListOf<String>()
            val useCase =
                useCase(
                    retroAchievementsRepository = FakeRetroAchievementsRepository(listOf(hashMatch)),
                    gameRomHashRepository = FakeGameRomHashRepository(hash = "aaaa"),
                    onResolved = { messages += it },
                )

            useCase(gameReference, gameName = "Chrono Trigger")

            assertEquals(1, messages.size)
            assertTrue(messages.single().contains("MATCHED"))
            assertTrue(messages.single().contains("via=RomHash"))
            assertTrue(messages.single().contains("gameId=2"))
        }

    @Test
    fun `onResolved distinguishes a filename-signal match from a gameName-signal match`() =
        runTest {
            val candidates = listOf(RetroAchievementsCandidateGame(1L, "Chrono Trigger", iconUrl = null))
            val messages = mutableListOf<String>()
            val useCase =
                useCase(
                    retroAchievementsRepository = FakeRetroAchievementsRepository(candidates),
                    onResolved = { messages += it },
                )

            useCase(gameReference, gameName = "Some Unrelated Scraped Title")

            assertTrue(messages.single().contains("signal=filename"))
        }

    @Test
    fun `onResolved reports NO MATCH when nothing matches`() =
        runTest {
            val candidates = listOf(RetroAchievementsCandidateGame(1L, "Chrono Cross", iconUrl = null))
            val messages = mutableListOf<String>()
            val useCase =
                useCase(
                    retroAchievementsRepository = FakeRetroAchievementsRepository(candidates),
                    onResolved = { messages += it },
                )

            useCase(gameReference, gameName = "Chrono Trigger")

            assertTrue(messages.single().contains("NO MATCH"))
        }
}

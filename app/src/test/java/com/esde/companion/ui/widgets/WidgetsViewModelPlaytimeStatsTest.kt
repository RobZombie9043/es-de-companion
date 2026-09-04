package com.esde.companion.ui.widgets

import com.esde.companion.domain.model.AchievementItem
import com.esde.companion.domain.model.AchievementSummaryFetchResult
import com.esde.companion.domain.model.AchievementSummaryPeek
import com.esde.companion.domain.model.EsdeEvent
import com.esde.companion.domain.model.GameAchievementSummary
import com.esde.companion.domain.model.GameDescription
import com.esde.companion.domain.model.GameMatchOverride
import com.esde.companion.domain.model.GameMedia
import com.esde.companion.domain.model.GamePlaytimeStats
import com.esde.companion.domain.model.GameRating
import com.esde.companion.domain.model.GameReference
import com.esde.companion.domain.model.GameRomHash
import com.esde.companion.domain.model.GridDimensions
import com.esde.companion.domain.model.MediaType
import com.esde.companion.domain.model.PlacedWidget
import com.esde.companion.domain.model.PlaytimeStatsWidgetState
import com.esde.companion.domain.model.RetroAchievementsAuthState
import com.esde.companion.domain.model.RetroAchievementsCandidateGame
import com.esde.companion.domain.model.RetroAchievementsConsole
import com.esde.companion.domain.model.RetroAchievementsCredentials
import com.esde.companion.domain.model.SavedWidgetCanvas
import com.esde.companion.domain.model.StateGroup
import com.esde.companion.domain.model.WidgetContent
import com.esde.companion.domain.model.WidgetType
import com.esde.companion.domain.repository.BundledSystemLogoRepository
import com.esde.companion.domain.repository.CustomSystemImageRepository
import com.esde.companion.domain.repository.CustomSystemLogoRepository
import com.esde.companion.domain.repository.EsdeLogRepository
import com.esde.companion.domain.repository.GameDescriptionRepository
import com.esde.companion.domain.repository.GameMatchOverrideRepository
import com.esde.companion.domain.repository.GameMediaRepository
import com.esde.companion.domain.repository.GameRatingRepository
import com.esde.companion.domain.repository.GameRomHashRepository
import com.esde.companion.domain.repository.RetroAchievementsCredentialsRepository
import com.esde.companion.domain.repository.RetroAchievementsRepository
import com.esde.companion.domain.repository.SystemMediaRepository
import com.esde.companion.domain.repository.WidgetLayoutRepository
import com.esde.companion.domain.usecase.GetGameAchievementSummaryUseCase
import com.esde.companion.domain.usecase.ObserveAppStateUseCase
import com.esde.companion.domain.usecase.ObserveConnectionStateUseCase
import com.esde.companion.domain.usecase.ObserveRetroAchievementsCredentialsUseCase
import com.esde.companion.domain.usecase.ObserveWidgetCanvasUseCase
import com.esde.companion.domain.usecase.PeekGameAchievementSummaryUseCase
import com.esde.companion.domain.usecase.ResolveBundledSystemLogoUseCase
import com.esde.companion.domain.usecase.ResolveCustomSystemImageUseCase
import com.esde.companion.domain.usecase.ResolveCustomSystemLogoUseCase
import com.esde.companion.domain.usecase.ResolveGameDescriptionUseCase
import com.esde.companion.domain.usecase.ResolveGameMediaUseCase
import com.esde.companion.domain.usecase.ResolveGameRatingUseCase
import com.esde.companion.domain.usecase.ResolveRandomSystemMediaUseCase
import com.esde.companion.domain.usecase.ResolveRetroAchievementsGameUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Sibling to [WidgetsViewModelAchievementSummaryTest] (its own small fakes, same "each test
 * file owns its small fakes" precedent that file's kdoc cites) covering the
 * `WidgetType.PlaytimeStats` widget - which shares its entire match/peek/fetch pipeline with
 * `WidgetType.AchievementSummary` (see `WidgetsViewModel.AchievementDataResolution`'s kdoc),
 * so most of the interesting behavior here is specifically about that sharing and about
 * [GamePlaytimeStats] being a nullable field on the same [GameAchievementSummary], not a
 * re-test of the whole Loading/Unavailable state machine (already covered there).
 */
class WidgetsViewModelPlaytimeStatsTest {
    private class FakeEsdeLogRepository(
        private val fileExists: Flow<Boolean> = flowOf(true),
    ) : EsdeLogRepository {
        val events = MutableSharedFlow<EsdeEvent>()

        override fun observeEvents(): Flow<EsdeEvent> = events

        override fun observeLogFileExists(): Flow<Boolean> = fileExists
    }

    private class FakeWidgetLayoutRepository : WidgetLayoutRepository {
        private val canvases = mutableMapOf<StateGroup, MutableStateFlow<SavedWidgetCanvas>>()

        fun seed(
            stateGroup: StateGroup,
            widgets: List<PlacedWidget>,
        ) {
            flowFor(stateGroup).value = SavedWidgetCanvas(grid = null, widgets = widgets)
        }

        private fun flowFor(stateGroup: StateGroup) =
            canvases.getOrPut(stateGroup) { MutableStateFlow(SavedWidgetCanvas(grid = null, widgets = emptyList())) }

        override fun observeCanvas(stateGroup: StateGroup): Flow<SavedWidgetCanvas> = flowFor(stateGroup)

        override suspend fun saveCanvas(
            stateGroup: StateGroup,
            widgets: List<PlacedWidget>,
            grid: GridDimensions,
        ) {
            flowFor(stateGroup).value = SavedWidgetCanvas(grid, widgets)
        }
    }

    private class NoOpGameMediaRepository : GameMediaRepository {
        override suspend fun resolveMedia(
            systemShortName: String,
            systemPath: String?,
            romPath: String,
            mediaTypes: Set<MediaType>,
        ) = GameMedia(baseRelativePath = null, filesByType = emptyMap())
    }

    private class NoOpGameDescriptionRepository : GameDescriptionRepository {
        override suspend fun resolveDescription(
            systemShortName: String,
            romPath: String,
        ) = GameDescription(text = null)
    }

    private class NoOpGameRatingRepository : GameRatingRepository {
        override suspend fun resolveRating(
            systemShortName: String,
            romPath: String,
        ) = GameRating(value = null)
    }

    private class NoOpSystemMediaRepository : SystemMediaRepository {
        override suspend fun randomMedia(
            systemShortName: String,
            mediaType: MediaType,
        ): String? = null
    }

    private class NoOpCustomSystemImageRepository : CustomSystemImageRepository {
        override suspend fun findImage(systemShortName: String): String? = null
    }

    private class NoOpCustomSystemLogoRepository : CustomSystemLogoRepository {
        override suspend fun findLogo(systemShortName: String): String? = null
    }

    private class NoOpBundledSystemLogoRepository : BundledSystemLogoRepository {
        override suspend fun findLogoAssetPath(assetName: String): String? = null
    }

    private class FakeRetroAchievementsCredentialsRepository(
        signedInAs: RetroAchievementsCredentials? = null,
    ) : RetroAchievementsCredentialsRepository {
        private val credentials = MutableStateFlow(signedInAs)

        override suspend fun setCredentials(credentials: RetroAchievementsCredentials) = error("not used in this test")

        override suspend fun clearCredentials() = error("not used in this test")

        override fun observeCredentials(): Flow<RetroAchievementsCredentials?> = credentials
    }

    private class FakeGameMatchOverrideRepository(
        private val override: GameMatchOverride? = null,
    ) : GameMatchOverrideRepository {
        override suspend fun setOverride(override: GameMatchOverride) = error("not used in this test")

        override suspend fun clearOverride(gameReference: GameReference) = error("not used in this test")

        override suspend fun getOverride(gameReference: GameReference): GameMatchOverride? = override

        override fun observeAllOverrides() = error("not used in this test")
    }

    private class FakeGameRomHashRepository : GameRomHashRepository {
        override suspend fun resolveRomHash(
            systemShortName: String,
            romPath: String,
        ): GameRomHash = GameRomHash(value = null)
    }

    /** Same "peek reflects the last fetch" fake shape as
     * [WidgetsViewModelAchievementSummaryTest]'s equivalent, plus a call counter for the
     * shared-fetch assertion below. */
    private class RecordingRetroAchievementsRepository(
        private val initialPeek: AchievementSummaryPeek? = null,
        private val fetchResult: AchievementSummaryFetchResult = AchievementSummaryFetchResult.NotFound,
        private val candidates: List<RetroAchievementsCandidateGame> = emptyList(),
    ) : RetroAchievementsRepository {
        var getAchievementSummaryCallCount = 0
            private set
        private var fetched = false

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
            getAchievementSummaryCallCount++
            fetched = true
            return fetchResult
        }

        override suspend fun peekAchievementSummary(gameId: Long): AchievementSummaryPeek? {
            val result = fetchResult
            return if (fetched && result is AchievementSummaryFetchResult.Success) {
                AchievementSummaryPeek(result.summary, isStale = false)
            } else {
                initialPeek
            }
        }

        override suspend fun getUserGameProgress() = error("not used in this test")

        override suspend fun getAchievementComments(achievementId: Long) = error("not used in this test")

        override suspend fun getGameLeaderboards(
            gameId: Long,
            forceRefresh: Boolean,
        ) = error("not used in this test")

        override suspend fun peekGameLeaderboards(gameId: Long) = error("not used in this test")

        override suspend fun getLeaderboardEntries(leaderboardId: Long) = error("not used in this test")
    }

    /** Returns a different [AchievementSummaryFetchResult] on each successive
     * [getAchievementSummary] call (repeating the last entry once exhausted) - backs the
     * self-heal test below, which needs the first call to succeed with a null playtimeStats
     * and only the automatic force-refresh call after it to carry real stats. */
    private class SequencedFetchResultRepository(
        private val results: List<AchievementSummaryFetchResult>,
    ) : RetroAchievementsRepository {
        var getAchievementSummaryCallCount = 0
            private set
        private var lastResult: AchievementSummaryFetchResult? = null

        override suspend fun getAchievementSummary(
            gameId: Long,
            forceRefresh: Boolean,
        ): AchievementSummaryFetchResult {
            val result = results.getOrElse(getAchievementSummaryCallCount) { results.last() }
            getAchievementSummaryCallCount++
            lastResult = result
            return result
        }

        override suspend fun peekAchievementSummary(gameId: Long): AchievementSummaryPeek? {
            val result = lastResult
            return if (result is AchievementSummaryFetchResult.Success) {
                AchievementSummaryPeek(result.summary, isStale = false)
            } else {
                null
            }
        }

        override suspend fun validateCredentials(creds: RetroAchievementsCredentials): RetroAchievementsAuthState {
            error("not used in this test")
        }

        override suspend fun getCandidateGames(c: RetroAchievementsConsole): List<RetroAchievementsCandidateGame> {
            return emptyList()
        }

        override suspend fun getUserGameProgress() = error("not used in this test")

        override suspend fun getAchievementComments(achievementId: Long) = error("not used in this test")

        override suspend fun getGameLeaderboards(
            gameId: Long,
            forceRefresh: Boolean,
        ) = error("not used in this test")

        override suspend fun peekGameLeaderboards(gameId: Long) = error("not used in this test")

        override suspend fun getLeaderboardEntries(leaderboardId: Long) = error("not used in this test")
    }

    private val testDispatcher = StandardTestDispatcher()
    private val grid = GridDimensions(columns = 10, rows = 10)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun placedWidget(
        id: String,
        widgetType: WidgetType,
    ) = PlacedWidget(
        id = id,
        widgetType = widgetType,
        gridColumn = 0,
        gridRow = 0,
        columnSpan = 2,
        rowSpan = 2,
        zIndex = 0,
    )

    @Suppress("LongParameterList")
    private fun TestScope.buildViewModel(
        esdeLogRepository: FakeEsdeLogRepository,
        widgetLayoutRepository: FakeWidgetLayoutRepository,
        gameMatchOverrideRepository: GameMatchOverrideRepository = FakeGameMatchOverrideRepository(),
        retroAchievementsCredentialsRepository: RetroAchievementsCredentialsRepository =
            FakeRetroAchievementsCredentialsRepository(),
        retroAchievementsRepository: RetroAchievementsRepository = RecordingRetroAchievementsRepository(),
    ): WidgetsViewModel {
        val observeAppState = ObserveAppStateUseCase(esdeLogRepository, backgroundScope)
        val observeConnectionState = ObserveConnectionStateUseCase(esdeLogRepository, observeAppState)
        val viewModel =
            WidgetsViewModel(
                observeConnectionState = observeConnectionState,
                observeWidgetCanvas = ObserveWidgetCanvasUseCase(widgetLayoutRepository),
                resolveGameMedia = ResolveGameMediaUseCase(NoOpGameMediaRepository()),
                resolveRandomSystemMedia = ResolveRandomSystemMediaUseCase(NoOpSystemMediaRepository()),
                resolveGameDescription = ResolveGameDescriptionUseCase(NoOpGameDescriptionRepository()),
                resolveGameRating = ResolveGameRatingUseCase(NoOpGameRatingRepository()),
                resolveCustomSystemImage = ResolveCustomSystemImageUseCase(NoOpCustomSystemImageRepository()),
                resolveCustomSystemLogo = ResolveCustomSystemLogoUseCase(NoOpCustomSystemLogoRepository()),
                resolveBundledSystemLogo = ResolveBundledSystemLogoUseCase(NoOpBundledSystemLogoRepository()),
                resolveRetroAchievementsGame =
                    ResolveRetroAchievementsGameUseCase(
                        gameMatchOverrideRepository,
                        retroAchievementsRepository,
                        FakeGameRomHashRepository(),
                    ),
                observeRetroAchievementsCredentials =
                    ObserveRetroAchievementsCredentialsUseCase(retroAchievementsCredentialsRepository),
                peekAchievementSummary = PeekGameAchievementSummaryUseCase(retroAchievementsRepository),
                getAchievementSummary = GetGameAchievementSummaryUseCase(retroAchievementsRepository),
            )
        backgroundScope.launch { viewModel.canvasState.collect {} }
        advanceUntilIdle()
        return viewModel
    }

    private val gameSelect = EsdeEvent.GameSelect("/roms/snes/game.sfc", "Game", "snes", "SNES")
    private val override = GameMatchOverride(systemShortName = "snes", romPath = "/roms/snes/game.sfc", raGameId = 1L)
    private val credentials = RetroAchievementsCredentials(username = "player1", webApiKey = "key")

    private fun summaryWithPlaytimeStats(playtimeStats: GamePlaytimeStats?) =
        GameAchievementSummary(
            gameId = 1L,
            gameTitle = "Game",
            totalPoints = 500,
            earnedPoints = 150,
            completionPercent = 30f,
            achievements =
                listOf(
                    AchievementItem(
                        id = 0,
                        title = "A",
                        description = "",
                        points = 50,
                        badgeUrl = null,
                        unlocked = false,
                        unlockedAt = null,
                    ),
                ),
            playtimeStats = playtimeStats,
        )

    @Test
    fun `a matched game with cached playtime stats resolves to Loaded carrying those stats`() =
        runTest(testDispatcher) {
            val esdeLogRepository = FakeEsdeLogRepository()
            val widgetLayoutRepository = FakeWidgetLayoutRepository()
            widgetLayoutRepository.seed(StateGroup.Playing, listOf(placedWidget("pt", WidgetType.PlaytimeStats())))
            val stats =
                GamePlaytimeStats(
                    beatSeconds = 3600,
                    beatHardcoreSeconds = 5400,
                    completedSeconds = 7200,
                    masteredSeconds = null,
                )
            val retroAchievementsRepository =
                RecordingRetroAchievementsRepository(
                    initialPeek = AchievementSummaryPeek(summaryWithPlaytimeStats(stats), isStale = false),
                )
            val viewModel =
                buildViewModel(
                    esdeLogRepository,
                    widgetLayoutRepository,
                    gameMatchOverrideRepository = FakeGameMatchOverrideRepository(override),
                    retroAchievementsCredentialsRepository = FakeRetroAchievementsCredentialsRepository(credentials),
                    retroAchievementsRepository = retroAchievementsRepository,
                )
            viewModel.setGridDimensions(grid)

            esdeLogRepository.events.emit(gameSelect)
            advanceUntilIdle()

            val state = viewModel.canvasState.value
            check(state is WidgetCanvasState.Showing)
            val content = state.contentByWidgetId["pt"]
            check(content is WidgetContent.PlaytimeStats)
            val loaded = content.state
            check(loaded is PlaytimeStatsWidgetState.Loaded)
            assertEquals(stats, loaded.stats)
            assertEquals(false, loaded.isRefreshing)
            assertEquals(0, retroAchievementsRepository.getAchievementSummaryCallCount)
        }

    @Test
    fun `a matched game whose summary has no playtime stats resolves to Unavailable`() =
        runTest(testDispatcher) {
            val esdeLogRepository = FakeEsdeLogRepository()
            val widgetLayoutRepository = FakeWidgetLayoutRepository()
            widgetLayoutRepository.seed(StateGroup.Playing, listOf(placedWidget("pt", WidgetType.PlaytimeStats())))
            val retroAchievementsRepository =
                RecordingRetroAchievementsRepository(
                    initialPeek = AchievementSummaryPeek(summaryWithPlaytimeStats(null), isStale = false),
                )
            val viewModel =
                buildViewModel(
                    esdeLogRepository,
                    widgetLayoutRepository,
                    gameMatchOverrideRepository = FakeGameMatchOverrideRepository(override),
                    retroAchievementsCredentialsRepository = FakeRetroAchievementsCredentialsRepository(credentials),
                    retroAchievementsRepository = retroAchievementsRepository,
                )
            viewModel.setGridDimensions(grid)

            esdeLogRepository.events.emit(gameSelect)
            advanceUntilIdle()

            val state = viewModel.canvasState.value
            check(state is WidgetCanvasState.Showing)
            val content = state.contentByWidgetId["pt"]
            check(content is WidgetContent.PlaytimeStats)
            assertEquals(PlaytimeStatsWidgetState.Unavailable, content.state)
        }

    @Test
    fun `an unmatched game resolves PlaytimeStats to Unavailable, not Empty`() =
        runTest(testDispatcher) {
            val esdeLogRepository = FakeEsdeLogRepository()
            val widgetLayoutRepository = FakeWidgetLayoutRepository()
            widgetLayoutRepository.seed(StateGroup.Playing, listOf(placedWidget("pt", WidgetType.PlaytimeStats())))
            val viewModel =
                buildViewModel(
                    esdeLogRepository,
                    widgetLayoutRepository,
                    retroAchievementsCredentialsRepository = FakeRetroAchievementsCredentialsRepository(credentials),
                )
            viewModel.setGridDimensions(grid)

            val unsupportedSystemGameSelect =
                EsdeEvent.GameSelect("/roms/windows/game.exe", "Game", "windows", "Windows")
            esdeLogRepository.events.emit(unsupportedSystemGameSelect)
            advanceUntilIdle()

            val state = viewModel.canvasState.value
            check(state is WidgetCanvasState.Showing)
            val content = state.contentByWidgetId["pt"]
            check(content is WidgetContent.PlaytimeStats)
            assertEquals(PlaytimeStatsWidgetState.Unavailable, content.state)
        }

    @Test
    fun `both AchievementSummary and PlaytimeStats on the same canvas share one background fetch`() =
        runTest(testDispatcher) {
            val esdeLogRepository = FakeEsdeLogRepository()
            val widgetLayoutRepository = FakeWidgetLayoutRepository()
            widgetLayoutRepository.seed(
                StateGroup.Playing,
                listOf(
                    placedWidget("ra", WidgetType.AchievementSummary()),
                    placedWidget("pt", WidgetType.PlaytimeStats()),
                ),
            )
            val stats =
                GamePlaytimeStats(
                    beatSeconds = 1800,
                    beatHardcoreSeconds = null,
                    completedSeconds = null,
                    masteredSeconds = null,
                )
            val retroAchievementsRepository =
                RecordingRetroAchievementsRepository(
                    initialPeek = null,
                    fetchResult = AchievementSummaryFetchResult.Success(summaryWithPlaytimeStats(stats)),
                )
            val viewModel =
                buildViewModel(
                    esdeLogRepository,
                    widgetLayoutRepository,
                    gameMatchOverrideRepository = FakeGameMatchOverrideRepository(override),
                    retroAchievementsCredentialsRepository = FakeRetroAchievementsCredentialsRepository(credentials),
                    retroAchievementsRepository = retroAchievementsRepository,
                )
            viewModel.setGridDimensions(grid)

            esdeLogRepository.events.emit(gameSelect)
            advanceUntilIdle()

            val state = viewModel.canvasState.value
            check(state is WidgetCanvasState.Showing)
            val ptContent = state.contentByWidgetId["pt"]
            check(ptContent is WidgetContent.PlaytimeStats)
            val loaded = ptContent.state
            check(loaded is PlaytimeStatsWidgetState.Loaded)
            assertEquals(stats, loaded.stats)
            // The whole point of sharing AchievementDataResolution - one fetch serves both
            // widget types' lookups in the same resolve pass, not one per widget type.
            assertEquals(1, retroAchievementsRepository.getAchievementSummaryCallCount)
        }

    @Test
    fun `a Success fetch with no playtime stats triggers one automatic force-refresh that recovers them`() =
        runTest(testDispatcher) {
            // Confirmed on-device: RA's GetGameProgression sub-call occasionally comes back
            // empty even though the main achievement data in the same response loads fine -
            // a plain NetworkError retry doesn't catch this since the overall result genuinely
            // is a Success. A manual "Refresh" in the achievement screen reliably recovers it,
            // so this covers the same self-heal happening automatically for the widget.
            val esdeLogRepository = FakeEsdeLogRepository()
            val widgetLayoutRepository = FakeWidgetLayoutRepository()
            widgetLayoutRepository.seed(StateGroup.Playing, listOf(placedWidget("pt", WidgetType.PlaytimeStats())))
            val stats =
                GamePlaytimeStats(
                    beatSeconds = 3600,
                    beatHardcoreSeconds = null,
                    completedSeconds = 7200,
                    masteredSeconds = null,
                )
            val retroAchievementsRepository =
                SequencedFetchResultRepository(
                    listOf(
                        AchievementSummaryFetchResult.Success(summaryWithPlaytimeStats(null)),
                        AchievementSummaryFetchResult.Success(summaryWithPlaytimeStats(stats)),
                    ),
                )
            val viewModel =
                buildViewModel(
                    esdeLogRepository,
                    widgetLayoutRepository,
                    gameMatchOverrideRepository = FakeGameMatchOverrideRepository(override),
                    retroAchievementsCredentialsRepository = FakeRetroAchievementsCredentialsRepository(credentials),
                    retroAchievementsRepository = retroAchievementsRepository,
                )
            viewModel.setGridDimensions(grid)

            esdeLogRepository.events.emit(gameSelect)
            advanceUntilIdle()

            val state = viewModel.canvasState.value
            check(state is WidgetCanvasState.Showing)
            val content = state.contentByWidgetId["pt"]
            check(content is WidgetContent.PlaytimeStats)
            val loaded = content.state
            check(loaded is PlaytimeStatsWidgetState.Loaded)
            assertEquals(stats, loaded.stats)
            assertEquals(2, retroAchievementsRepository.getAchievementSummaryCallCount)
        }

    @Test
    fun `an AchievementSummary-only canvas never force-refreshes for missing playtime stats`() =
        runTest(testDispatcher) {
            // needsPlaytimeStats gates the self-heal - an achievements-only canvas has no use
            // for playtime data, so it must not pay for the extra network call.
            val esdeLogRepository = FakeEsdeLogRepository()
            val widgetLayoutRepository = FakeWidgetLayoutRepository()
            widgetLayoutRepository.seed(StateGroup.Playing, listOf(placedWidget("ra", WidgetType.AchievementSummary())))
            val retroAchievementsRepository =
                SequencedFetchResultRepository(
                    listOf(AchievementSummaryFetchResult.Success(summaryWithPlaytimeStats(null))),
                )
            val viewModel =
                buildViewModel(
                    esdeLogRepository,
                    widgetLayoutRepository,
                    gameMatchOverrideRepository = FakeGameMatchOverrideRepository(override),
                    retroAchievementsCredentialsRepository = FakeRetroAchievementsCredentialsRepository(credentials),
                    retroAchievementsRepository = retroAchievementsRepository,
                )
            viewModel.setGridDimensions(grid)

            esdeLogRepository.events.emit(gameSelect)
            advanceUntilIdle()

            assertEquals(1, retroAchievementsRepository.getAchievementSummaryCallCount)
        }
}

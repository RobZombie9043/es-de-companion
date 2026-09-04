package com.esde.companion.ui.widgets

import com.esde.companion.domain.model.AchievementItem
import com.esde.companion.domain.model.AchievementSummaryFetchResult
import com.esde.companion.domain.model.AchievementSummaryPeek
import com.esde.companion.domain.model.AchievementSummaryWidgetState
import com.esde.companion.domain.model.EsdeEvent
import com.esde.companion.domain.model.GameAchievementSummary
import com.esde.companion.domain.model.GameDescription
import com.esde.companion.domain.model.GameMatchOverride
import com.esde.companion.domain.model.GameMedia
import com.esde.companion.domain.model.GameRating
import com.esde.companion.domain.model.GameReference
import com.esde.companion.domain.model.GameRomHash
import com.esde.companion.domain.model.GridDimensions
import com.esde.companion.domain.model.MediaType
import com.esde.companion.domain.model.PlacedWidget
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
import kotlinx.coroutines.channels.Channel
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
 * Split out of [WidgetsViewModelTest] (which tripped detekt's LargeClass once this grew) -
 * peek + background-fetch stale-while-revalidate behavior for the `WidgetType.AchievementSummary`
 * widget specifically. Deliberately self-contained (its own small fakes, not shared with
 * [WidgetsViewModelTest]) rather than exposing internal visibility across the two files -
 * same "each test file owns its small fakes" precedent as [com.esde.companion.data.retroachievements.RetroAchievementsRepositoryImplTest]
 * and [com.esde.companion.data.retroachievements.GameListCacheTest] each defining their own
 * `FakeGameListCacheStore` rather than sharing one.
 */
class WidgetsViewModelAchievementSummaryTest {
    // --- Fakes -------------------------------------------------------------------------

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

    // Trivial no-op fakes for WidgetsViewModel's other constructor dependencies - only the
    // AchievementSummary widget is ever placed on the canvas in this file's tests, so none
    // of these are actually invoked; they exist only to satisfy the constructor.
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

    /** [override] always wins in [ResolveRetroAchievementsGameUseCase], bypassing candidate-list/
     * title matching entirely - the simplest deterministic way to get a fixed gameId in tests. */
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

    /** [initialPeek] is what [peekAchievementSummary] returns until [getAchievementSummary]
     * has been called once, after which it switches to reflecting [fetchResult] - simulates
     * a real cache's "peek sees whatever the last fetch wrote" behavior without a real
     * AchievementSummaryCache. [candidates] backs [getCandidateGames] - only exercised when
     * a test's [FakeGameMatchOverrideRepository] has no override, letting
     * ResolveRetroAchievementsGameUseCase's real hash/title-matching tiers run (against an
     * empty list by default, i.e. no match). */
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

    /**
     * [getAchievementSummary] suspends on a rendezvous [Channel] until the test explicitly
     * completes it - same reasoning as [com.esde.companion.ui.retroachievements.AchievementDisplayControllerTest]'s
     * equivalent fake: a plain immediately-returning fetch would let the Loading -> settled
     * transition happen synchronously inside the same `advanceUntilIdle()` call that triggers
     * it, so the test could never observe Loading on its own.
     */
    private class GatedRetroAchievementsRepository(
        private val initialPeek: AchievementSummaryPeek?,
    ) : RetroAchievementsRepository {
        private val pendingFetch = Channel<AchievementSummaryFetchResult>()
        private var fetchedResult: AchievementSummaryFetchResult? = null

        suspend fun completeFetch(result: AchievementSummaryFetchResult) = pendingFetch.send(result)

        override suspend fun getAchievementSummary(
            gameId: Long,
            forceRefresh: Boolean,
        ): AchievementSummaryFetchResult {
            val result = pendingFetch.receive()
            fetchedResult = result
            return result
        }

        override suspend fun peekAchievementSummary(gameId: Long): AchievementSummaryPeek? {
            val result = fetchedResult
            return if (result is AchievementSummaryFetchResult.Success) {
                AchievementSummaryPeek(result.summary, isStale = false)
            } else {
                initialPeek
            }
        }

        override suspend fun validateCredentials(creds: RetroAchievementsCredentials): RetroAchievementsAuthState =
            error("not used in this test")

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

    // --- Setup ---------------------------------------------------------------------------

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

    private fun placedWidget(id: String) =
        PlacedWidget(
            id = id,
            widgetType = WidgetType.AchievementSummary(),
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
        // canvasState is WhileSubscribed - simulates the always-on UI collector
        // (MainScreen's collectAsState()) so reading .value in tests reflects live updates -
        // see WidgetsViewModelTest's equivalent setup for the full reasoning.
        backgroundScope.launch { viewModel.canvasState.collect {} }
        advanceUntilIdle()
        return viewModel
    }

    // --- AchievementSummary widget: peek + background-fetch stale-while-revalidate ---------

    private val achievementsGameSelect = EsdeEvent.GameSelect("/roms/snes/game.sfc", "Game", "snes", "SNES")
    private val achievementsOverride =
        GameMatchOverride(systemShortName = "snes", romPath = "/roms/snes/game.sfc", raGameId = 1L)
    private val achievementsCredentials = RetroAchievementsCredentials(username = "player1", webApiKey = "key")

    private fun achievementSummary(
        title: String = "Game",
        achievementCount: Int = 1,
    ) = GameAchievementSummary(
        gameId = 1L,
        gameTitle = title,
        totalPoints = 500,
        earnedPoints = 150,
        completionPercent = 30f,
        achievements =
            List(achievementCount) { index ->
                AchievementItem(
                    id = index.toLong(),
                    title = "Achievement $index",
                    description = "",
                    points = 50,
                    badgeUrl = null,
                    unlocked = false,
                    unlockedAt = null,
                )
            },
    )

    @Test
    fun `signed out, an AchievementSummary widget resolves to Empty and never fetches`() =
        runTest(testDispatcher) {
            val esdeLogRepository = FakeEsdeLogRepository()
            val widgetLayoutRepository = FakeWidgetLayoutRepository()
            widgetLayoutRepository.seed(StateGroup.Playing, listOf(placedWidget("ra")))
            val retroAchievementsRepository =
                RecordingRetroAchievementsRepository(
                    initialPeek = AchievementSummaryPeek(achievementSummary(), isStale = false),
                )
            val viewModel =
                buildViewModel(
                    esdeLogRepository,
                    widgetLayoutRepository,
                    gameMatchOverrideRepository = FakeGameMatchOverrideRepository(achievementsOverride),
                    retroAchievementsCredentialsRepository =
                        FakeRetroAchievementsCredentialsRepository(signedInAs = null),
                    retroAchievementsRepository = retroAchievementsRepository,
                )
            viewModel.setGridDimensions(grid)

            esdeLogRepository.events.emit(achievementsGameSelect)
            advanceUntilIdle()

            val state = viewModel.canvasState.value
            check(state is WidgetCanvasState.Showing)
            assertEquals(WidgetContent.Empty, state.contentByWidgetId["ra"])
            assertEquals(0, retroAchievementsRepository.getAchievementSummaryCallCount)
        }

    @Test
    fun `signed in with a fresh cached peek, the widget shows it immediately with no background fetch`() =
        runTest(testDispatcher) {
            val esdeLogRepository = FakeEsdeLogRepository()
            val widgetLayoutRepository = FakeWidgetLayoutRepository()
            widgetLayoutRepository.seed(StateGroup.Playing, listOf(placedWidget("ra")))
            val retroAchievementsRepository =
                RecordingRetroAchievementsRepository(
                    initialPeek = AchievementSummaryPeek(achievementSummary("Fresh"), isStale = false),
                )
            val viewModel =
                buildViewModel(
                    esdeLogRepository,
                    widgetLayoutRepository,
                    gameMatchOverrideRepository = FakeGameMatchOverrideRepository(achievementsOverride),
                    retroAchievementsCredentialsRepository =
                        FakeRetroAchievementsCredentialsRepository(signedInAs = achievementsCredentials),
                    retroAchievementsRepository = retroAchievementsRepository,
                )
            viewModel.setGridDimensions(grid)

            esdeLogRepository.events.emit(achievementsGameSelect)
            advanceUntilIdle()

            val state = viewModel.canvasState.value
            check(state is WidgetCanvasState.Showing)
            val content = state.contentByWidgetId["ra"]
            check(content is WidgetContent.AchievementSummary)
            val loaded = content.state
            check(loaded is AchievementSummaryWidgetState.Loaded)
            assertEquals(false, loaded.isRefreshing)
            assertEquals(0, retroAchievementsRepository.getAchievementSummaryCallCount)
        }

    @Test
    fun `signed in with nothing cached yet, the widget shows Loading, then settles once the fetch resolves`() =
        runTest(testDispatcher) {
            val esdeLogRepository = FakeEsdeLogRepository()
            val widgetLayoutRepository = FakeWidgetLayoutRepository()
            widgetLayoutRepository.seed(StateGroup.Playing, listOf(placedWidget("ra")))
            val retroAchievementsRepository = GatedRetroAchievementsRepository(initialPeek = null)
            val viewModel =
                buildViewModel(
                    esdeLogRepository,
                    widgetLayoutRepository,
                    gameMatchOverrideRepository = FakeGameMatchOverrideRepository(achievementsOverride),
                    retroAchievementsCredentialsRepository =
                        FakeRetroAchievementsCredentialsRepository(signedInAs = achievementsCredentials),
                    retroAchievementsRepository = retroAchievementsRepository,
                )
            viewModel.setGridDimensions(grid)

            esdeLogRepository.events.emit(achievementsGameSelect)
            advanceUntilIdle()

            val loadingState = viewModel.canvasState.value
            check(loadingState is WidgetCanvasState.Showing)
            val loadingContent = loadingState.contentByWidgetId["ra"]
            check(loadingContent is WidgetContent.AchievementSummary)
            assertEquals(AchievementSummaryWidgetState.Loading, loadingContent.state)

            retroAchievementsRepository.completeFetch(AchievementSummaryFetchResult.NotFound)
            advanceUntilIdle()

            val settledState = viewModel.canvasState.value
            check(settledState is WidgetCanvasState.Showing)
            val settledContent = settledState.contentByWidgetId["ra"]
            check(settledContent is WidgetContent.AchievementSummary)
            assertEquals(AchievementSummaryWidgetState.Unavailable, settledContent.state)
        }

    @Test
    fun `a matched game with no cached data and a NotFound fetch result settles on Unavailable`() =
        runTest(testDispatcher) {
            val esdeLogRepository = FakeEsdeLogRepository()
            val widgetLayoutRepository = FakeWidgetLayoutRepository()
            widgetLayoutRepository.seed(StateGroup.Playing, listOf(placedWidget("ra")))
            val retroAchievementsRepository =
                RecordingRetroAchievementsRepository(
                    initialPeek = null,
                    fetchResult = AchievementSummaryFetchResult.NotFound,
                )
            val viewModel =
                buildViewModel(
                    esdeLogRepository,
                    widgetLayoutRepository,
                    gameMatchOverrideRepository = FakeGameMatchOverrideRepository(achievementsOverride),
                    retroAchievementsCredentialsRepository =
                        FakeRetroAchievementsCredentialsRepository(signedInAs = achievementsCredentials),
                    retroAchievementsRepository = retroAchievementsRepository,
                )
            viewModel.setGridDimensions(grid)

            esdeLogRepository.events.emit(achievementsGameSelect)
            advanceUntilIdle()

            val state = viewModel.canvasState.value
            check(state is WidgetCanvasState.Showing)
            val content = state.contentByWidgetId["ra"]
            check(content is WidgetContent.AchievementSummary)
            assertEquals(AchievementSummaryWidgetState.Unavailable, content.state)
        }

    @Test
    fun `a matched game with no cached data and a NetworkError fetch result stays Loading, not Unavailable`() =
        runTest(testDispatcher) {
            // A transient failure (offline, rate-limited, RA outage) must never render as a
            // false "no achievements" claim for a game that might genuinely have some - see
            // completedAchievementFetchGameId's kdoc in WidgetsViewModel.
            val esdeLogRepository = FakeEsdeLogRepository()
            val widgetLayoutRepository = FakeWidgetLayoutRepository()
            widgetLayoutRepository.seed(StateGroup.Playing, listOf(placedWidget("ra")))
            val retroAchievementsRepository =
                RecordingRetroAchievementsRepository(
                    initialPeek = null,
                    fetchResult = AchievementSummaryFetchResult.NetworkError("offline"),
                )
            val viewModel =
                buildViewModel(
                    esdeLogRepository,
                    widgetLayoutRepository,
                    gameMatchOverrideRepository = FakeGameMatchOverrideRepository(achievementsOverride),
                    retroAchievementsCredentialsRepository =
                        FakeRetroAchievementsCredentialsRepository(signedInAs = achievementsCredentials),
                    retroAchievementsRepository = retroAchievementsRepository,
                )
            viewModel.setGridDimensions(grid)

            esdeLogRepository.events.emit(achievementsGameSelect)
            advanceUntilIdle()

            val state = viewModel.canvasState.value
            check(state is WidgetCanvasState.Showing)
            val content = state.contentByWidgetId["ra"]
            check(content is WidgetContent.AchievementSummary)
            assertEquals(AchievementSummaryWidgetState.Loading, content.state)
        }

    @Test
    fun `an unmatched game (no RetroAchievements entry) resolves to Unavailable, not Empty`() =
        runTest(testDispatcher) {
            val esdeLogRepository = FakeEsdeLogRepository()
            val widgetLayoutRepository = FakeWidgetLayoutRepository()
            widgetLayoutRepository.seed(StateGroup.Playing, listOf(placedWidget("ra")))
            val viewModel =
                buildViewModel(
                    esdeLogRepository,
                    widgetLayoutRepository,
                    retroAchievementsCredentialsRepository =
                        FakeRetroAchievementsCredentialsRepository(signedInAs = achievementsCredentials),
                )
            viewModel.setGridDimensions(grid)

            // An unsupported system (no EsdeSystemToRaConsoleMapping entry) short-circuits
            // ResolveRetroAchievementsGameUseCase to UnsupportedSystem before it ever touches
            // the candidate list/title-matching - unlike a genuine NoMatch on a supported
            // system, which would hop to the real (non-test-dispatcher-controlled)
            // Dispatchers.Default for title/hash matching and race this test's advanceUntilIdle().
            val unsupportedSystemGameSelect =
                EsdeEvent.GameSelect("/roms/windows/game.exe", "Game", "windows", "Windows")
            esdeLogRepository.events.emit(unsupportedSystemGameSelect)
            advanceUntilIdle()

            val state = viewModel.canvasState.value
            check(state is WidgetCanvasState.Showing)
            val content = state.contentByWidgetId["ra"]
            check(content is WidgetContent.AchievementSummary)
            assertEquals(AchievementSummaryWidgetState.Unavailable, content.state)
        }

    @Test
    fun `a matched game whose achievement set is empty resolves to Unavailable`() =
        runTest(testDispatcher) {
            val esdeLogRepository = FakeEsdeLogRepository()
            val widgetLayoutRepository = FakeWidgetLayoutRepository()
            widgetLayoutRepository.seed(StateGroup.Playing, listOf(placedWidget("ra")))
            val retroAchievementsRepository =
                RecordingRetroAchievementsRepository(
                    initialPeek = AchievementSummaryPeek(achievementSummary(achievementCount = 0), isStale = false),
                )
            val viewModel =
                buildViewModel(
                    esdeLogRepository,
                    widgetLayoutRepository,
                    gameMatchOverrideRepository = FakeGameMatchOverrideRepository(achievementsOverride),
                    retroAchievementsCredentialsRepository =
                        FakeRetroAchievementsCredentialsRepository(signedInAs = achievementsCredentials),
                    retroAchievementsRepository = retroAchievementsRepository,
                )
            viewModel.setGridDimensions(grid)

            esdeLogRepository.events.emit(achievementsGameSelect)
            advanceUntilIdle()

            val state = viewModel.canvasState.value
            check(state is WidgetCanvasState.Showing)
            val content = state.contentByWidgetId["ra"]
            check(content is WidgetContent.AchievementSummary)
            assertEquals(AchievementSummaryWidgetState.Unavailable, content.state)
        }

    @Test
    fun `signed in with no cached peek, the widget fetches once in the background and then shows the result`() =
        runTest(testDispatcher) {
            val esdeLogRepository = FakeEsdeLogRepository()
            val widgetLayoutRepository = FakeWidgetLayoutRepository()
            widgetLayoutRepository.seed(StateGroup.Playing, listOf(placedWidget("ra")))
            val retroAchievementsRepository =
                RecordingRetroAchievementsRepository(
                    initialPeek = null,
                    fetchResult = AchievementSummaryFetchResult.Success(achievementSummary("Fetched")),
                )
            val viewModel =
                buildViewModel(
                    esdeLogRepository,
                    widgetLayoutRepository,
                    gameMatchOverrideRepository = FakeGameMatchOverrideRepository(achievementsOverride),
                    retroAchievementsCredentialsRepository =
                        FakeRetroAchievementsCredentialsRepository(signedInAs = achievementsCredentials),
                    retroAchievementsRepository = retroAchievementsRepository,
                )
            viewModel.setGridDimensions(grid)

            esdeLogRepository.events.emit(achievementsGameSelect)
            advanceUntilIdle()

            val state = viewModel.canvasState.value
            check(state is WidgetCanvasState.Showing)
            val content = state.contentByWidgetId["ra"]
            check(content is WidgetContent.AchievementSummary)
            val loaded = content.state
            check(loaded is AchievementSummaryWidgetState.Loaded)
            assertEquals(150, loaded.earnedPoints)
            assertEquals(500, loaded.totalPoints)
            assertEquals(30f, loaded.completionPercent)
            assertEquals(1, retroAchievementsRepository.getAchievementSummaryCallCount)

            // Re-resolving for an unrelated reason (grid change) must not re-trigger a second
            // fetch for the same gameId - lastAchievementFetchGameId's one-attempt guard.
            viewModel.setGridDimensions(GridDimensions(columns = 20, rows = 10))
            advanceUntilIdle()

            assertEquals(1, retroAchievementsRepository.getAchievementSummaryCallCount)
        }
}

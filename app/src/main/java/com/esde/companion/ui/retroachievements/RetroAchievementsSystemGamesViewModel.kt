package com.esde.companion.ui.retroachievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esde.companion.domain.model.AchievementDisplayField
import com.esde.companion.domain.model.AchievementFilterOption
import com.esde.companion.domain.model.AchievementSortOrder
import com.esde.companion.domain.model.EsdeSystemToRaConsoleMapping
import com.esde.companion.domain.model.RetroAchievementsCandidateGame
import com.esde.companion.domain.model.RetroGameType
import com.esde.companion.domain.model.SystemGameFilters
import com.esde.companion.domain.model.UserGameProgress
import com.esde.companion.domain.model.filteredBySystemGameFilters
import com.esde.companion.domain.model.resolveAchievementsSystem
import com.esde.companion.domain.model.retroGameTypes
import com.esde.companion.domain.model.sortedBySystemGameOrder
import com.esde.companion.domain.usecase.GetAchievementCommentsUseCase
import com.esde.companion.domain.usecase.GetGameAchievementSummaryUseCase
import com.esde.companion.domain.usecase.GetRetroAchievementsSystemGamesUseCase
import com.esde.companion.domain.usecase.GetUserGameProgressUseCase
import com.esde.companion.domain.usecase.ObserveConnectionStateUseCase
import com.esde.companion.domain.usecase.ObserveRetroAchievementsCredentialsUseCase
import com.esde.companion.domain.usecase.ObserveScreensaverAwareContextUseCase
import com.esde.companion.domain.usecase.ObserveUpdateAchievementsOnScreensaverEnabledUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val STATE_STOP_TIMEOUT_MILLIS = 5_000L

private data class CurrentSystem(val systemShortName: String, val systemFullName: String)

/**
 * Drives the system-wide games browser FAB/screen - independent of
 * [RetroAchievementsViewModel], which tracks the current *game*, not the current *system*.
 * Re-loads whenever the signed-in state or the browsed system changes, via the same
 * [ObserveConnectionStateUseCase] source every other RetroAchievements ViewModel follows.
 * [currentSystem] is folded through
 * [com.esde.companion.domain.model.resolveAchievementsSystem] the same way
 * [RetroAchievementsViewModel.currentGame] is - when Settings > RetroAchievements >
 * "Update on Screensaver" is off, a screensaver starting holds the previously browsed
 * system instead of dropping to "no system".
 *
 * [searchQuery]/[filters]/[filteredGames] filter+sort the already-fetched list client-side -
 * unlike the manual correction picker's
 * [com.esde.companion.domain.usecase.SearchRetroAchievementsGamesUseCase], there's no "current
 * game" to rank suggestions against here, this is plain browsing. [filteredGames] is a single
 * derived pipeline over search text + [filters] + [gameProgress] rather than each setter
 * hand-recomputing the list, so changing one control can never leave another's effect stale.
 * [gameTypesByGameId] is precomputed once per [load] (title-tag parsing over potentially
 * thousands of games), not per filter pass.
 *
 * Also owns the per-game drill-down's own achievement sort/filter/display
 * ([gameSortOrder]/[gameFilter]/[gameDisplayField] - distinct from the browse-level [filters])
 * and fetch state ([selectedGameFetch]) - which game is "selected"
 * ([onGameSelected]/[onGameDetailClosed]) lives here, not as Compose-local state in
 * [RetroAchievementsSystemGamesScreen], specifically so it goes through [collectLatest]: tapping
 * game A (slow network), backing out, then tapping game B must never let A's late-arriving
 * response overwrite B's, the same reasoning [RetroAchievementsViewModel] already documents for
 * the live-game flow. [load] additionally clears the selection whenever the browsed system
 * itself changes, so a stale drill-down can't survive past the system it belonged to.
 *
 * [onRefreshRequested] forces the drill-down's next fetch to bypass its cache, the same
 * manual-refresh mechanism [RetroAchievementsViewModel.onRefreshRequested] exposes for the
 * live-game view (see [ForceRefreshCoordinator]).
 */
@Suppress("LongParameterList", "TooManyFunctions")
class RetroAchievementsSystemGamesViewModel(
    observeConnectionState: ObserveConnectionStateUseCase,
    observeCredentials: ObserveRetroAchievementsCredentialsUseCase,
    observeUpdateAchievementsOnScreensaverEnabled: ObserveUpdateAchievementsOnScreensaverEnabledUseCase,
    private val getSystemGames: GetRetroAchievementsSystemGamesUseCase,
    private val getAchievementSummary: GetGameAchievementSummaryUseCase,
    private val getUserGameProgress: GetUserGameProgressUseCase,
    private val getAchievementComments: GetAchievementCommentsUseCase,
) : ViewModel() {
    private val currentSystemContext =
        ObserveScreensaverAwareContextUseCase(
            observeConnectionState,
            observeUpdateAchievementsOnScreensaverEnabled,
            ::resolveAchievementsSystem,
        )

    private val currentSystem =
        currentSystemContext()
            .map { browsingSystem -> browsingSystem?.let { CurrentSystem(it.systemShortName, it.systemFullName) } }
            .distinctUntilChanged()

    private val _state = MutableStateFlow<SystemGamesUiState>(SystemGamesUiState.NoSystem)
    val state: StateFlow<SystemGamesUiState> = _state

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val allGames = MutableStateFlow<List<RetroAchievementsCandidateGame>>(emptyList())

    private val _filters = MutableStateFlow(SystemGameFilters())
    val filters: StateFlow<SystemGameFilters> = _filters

    private val gameProgress = MutableStateFlow<Map<Long, UserGameProgress>>(emptyMap())

    // Precomputed once per load() alongside allGames, not recomputed per filter pass - a
    // system like GBA/PSX has thousands of games, and running the title-tag parse per game per
    // keystroke/filter change would visibly stutter. Read only inside the filteredGames combine
    // below, which always runs after load() has already updated both together.
    private var gameTypesByGameId: Map<Long, Set<RetroGameType>> = emptyMap()

    val filteredGames: StateFlow<List<RetroAchievementsCandidateGame>> =
        combine(allGames, _searchQuery, _filters, gameProgress) { games, query, filters, progress ->
            val searched =
                if (query.isBlank()) games else games.filter { it.title.contains(query, ignoreCase = true) }
            searched
                .filteredBySystemGameFilters(filters, gameTypesByGameId, progress)
                .sortedBySystemGameOrder(filters.sortOrder, progress)
        }.flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STATE_STOP_TIMEOUT_MILLIS), emptyList())

    private val selectedGame = MutableStateFlow<RetroAchievementsCandidateGame?>(null)

    private val _selectedGameFetch =
        MutableStateFlow<RetroAchievementsFetchState>(RetroAchievementsFetchState.Idle)
    val selectedGameFetch: StateFlow<RetroAchievementsFetchState> = _selectedGameFetch

    // Sort/filter/display-field controls and the tap-to-expand comments accordion for the
    // drill-down achievement list - see AchievementDisplayController's kdoc. Shared with
    // RetroAchievementsViewModel, which owns its own instance for its own achievement list.
    private val achievementDisplay = AchievementDisplayController(getAchievementComments, viewModelScope)
    val gameSortOrder: StateFlow<AchievementSortOrder> = achievementDisplay.sortOrder
    val gameFilter: StateFlow<Set<AchievementFilterOption>> = achievementDisplay.filter
    val gameDisplayField: StateFlow<AchievementDisplayField> = achievementDisplay.displayField
    val expanded: StateFlow<ExpandedAchievementComments?> = achievementDisplay.expanded

    // Bumped by onRefreshRequested to force loadSelectedGame to re-run even though
    // selectedGame itself hasn't changed - same mechanism RetroAchievementsViewModel uses
    // for its own manual refresh.
    private val forceRefresh = ForceRefreshCoordinator()

    init {
        viewModelScope.launch {
            combine(observeCredentials(), currentSystem) { credentials, system -> (credentials != null) to system }
                .collectLatest { (signedIn, system) -> load(signedIn, system) }
        }
        viewModelScope.launch {
            combine(selectedGame, forceRefresh.trigger) { game, _ -> game }
                .collectLatest { game -> loadSelectedGame(game) }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onFiltersChanged(filters: SystemGameFilters) {
        _filters.value = filters
    }

    fun onGameSortOrderChanged(order: AchievementSortOrder) = achievementDisplay.onSortOrderChanged(order)

    fun onGameFilterChanged(filter: Set<AchievementFilterOption>) = achievementDisplay.onFilterChanged(filter)

    fun onGameDisplayFieldChanged(displayField: AchievementDisplayField) {
        achievementDisplay.onDisplayFieldChanged(displayField)
    }

    fun onGameSelected(game: RetroAchievementsCandidateGame) {
        selectedGame.value = game
    }

    fun onGameDetailClosed() {
        selectedGame.value = null
    }

    fun onAchievementTapped(achievementId: Long) = achievementDisplay.onAchievementTapped(achievementId)

    /** Forces the next fetch to bypass [GetGameAchievementSummaryUseCase]'s cache. */
    fun onRefreshRequested() = forceRefresh.request()

    private suspend fun loadSelectedGame(game: RetroAchievementsCandidateGame?) {
        achievementDisplay.onTargetChanged()
        if (game == null) {
            _selectedGameFetch.value = RetroAchievementsFetchState.Idle
            return
        }
        _selectedGameFetch.value = RetroAchievementsFetchState.Loading
        _selectedGameFetch.value = getAchievementSummary(game.gameId, forceRefresh.consume()).toFetchState()
    }

    private suspend fun load(
        signedIn: Boolean,
        system: CurrentSystem?,
    ) {
        allGames.value = emptyList()
        _searchQuery.value = ""
        gameTypesByGameId = emptyMap()
        gameProgress.value = emptyMap()
        selectedGame.value = null

        if (!signedIn) {
            _state.value = SystemGamesUiState.NotSignedIn
            return
        }
        if (system == null) {
            _state.value = SystemGamesUiState.NoSystem
            return
        }
        if (EsdeSystemToRaConsoleMapping.consoleFor(system.systemShortName) == null) {
            _state.value = SystemGamesUiState.UnsupportedSystem
            return
        }

        // Support is already confirmed above, so Loading (and everything from here on) is
        // only ever entered for a genuinely RA-supported system - the FAB's own visibility
        // (see MainActivity's systemGamesHasContent) keys off exactly that, not off this
        // fetch actually finishing. A console with thousands of games (e.g. gba, psx) can take
        // a while - or transiently fail - to fetch/cache; without this, the FAB stayed hidden
        // for that entire window even though the system definitely has achievements, which
        // read as "this system has no achievements" when it was really just still loading.
        _state.value = SystemGamesUiState.Loading
        val games = getSystemGames(system.systemShortName).orEmpty()
        allGames.value = games
        gameTypesByGameId = games.associate { it.gameId to it.title.retroGameTypes() }
        gameProgress.value = getUserGameProgress()
        _state.value = SystemGamesUiState.Loaded(system.systemFullName)
    }
}

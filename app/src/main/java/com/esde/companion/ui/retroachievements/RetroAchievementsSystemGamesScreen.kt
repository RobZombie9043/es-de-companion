package com.esde.companion.ui.retroachievements

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.esde.companion.domain.model.RetroAchievementsCandidateGame
import com.esde.companion.domain.model.SystemGameFilters
import com.esde.companion.ui.CORNER_BUTTON_EDGE_PADDING
import com.esde.companion.ui.theme.LocalIsDarkTheme
import com.esde.companion.ui.widgets.fallbackBackgroundAssetPath

/**
 * The system-wide games browser - one of the two screens the shared, contextually-aware
 * [com.esde.companion.domain.model.FabType.RetroAchievements] FAB can open (see
 * MainActivity's `RetroAchievementsFabMode`), shown via a plain
 * [androidx.compose.runtime.saveable.rememberSaveable] boolean in MainActivity, same "not a
 * nav destination" placement as [RetroAchievementsScreen]. Unlike that screen, which tracks
 * the current game, this one tracks the current *system* (see
 * [RetroAchievementsSystemGamesViewModel]) and lists every RA-tracked game for it, each with
 * its achievement count/points (already part of RA's `GetGameList` response - see
 * [RetroAchievementsCandidateGame]), so browsing doesn't need a fetch per game. Styled the
 * same "background image + translucent button tiles" way as [RetroAchievementsScreen].
 *
 * Tapping a game tile drills down into a per-game achievement detail page ([GameDetailPage])
 * reusing the same shared body [RetroAchievementsScreen] does - a second, self-contained
 * "list -> detail" drill-down entirely local to this composable (see [SystemGamesPage]),
 * following the same no-NavHost, plain-state idiom [com.esde.companion.ui.main.LongPressSettingsMenu]
 * uses. [GameDetailPage]'s top bar has no kebab/correction menu - unlike [RetroAchievementsScreen],
 * the game here was explicitly picked, not automatically resolved, so there's nothing to
 * correct - a back arrow (returning to the game list) and a manual-refresh action sit in the
 * kebab's slot instead.
 */
@Composable
fun RetroAchievementsSystemGamesScreen(
    viewModel: RetroAchievementsSystemGamesViewModel,
    overlayOpacityPercent: Int,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selectedGameFetch by viewModel.selectedGameFetch.collectAsStateWithLifecycle()
    val selectedGameLeaderboardsFetch by viewModel.selectedGameLeaderboardsFetch.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()

    var page by remember { mutableStateOf<SystemGamesPage>(SystemGamesPage.Browse) }
    val listState = rememberLazyListState()

    // Hoisted here rather than inside SystemGamesBrowsePage - that composable is a branch of
    // the list<->detail AnimatedContent below and gets remounted on every drill-down round
    // trip, so a LaunchedEffect placed there would fire scrollToItem(0) every time the user
    // returns from a game's detail page, destroying the scroll restoration listState (hoisted
    // to this non-remounted parent) was specifically added for.
    LaunchedEffect(filters) { listState.scrollToItem(0) }

    // If the browsed system genuinely changes (or sign-out, etc.) while mid-detail, this page
    // state has no other way to learn the ViewModel's context moved on - state is a StateFlow
    // of a data class, so this only actually restarts on a genuine transition, never a
    // same-value re-emit. Paired with the ViewModel's own load() clearing its selection for
    // the same reason (see RetroAchievementsSystemGamesViewModel's kdoc).
    LaunchedEffect(state) { page = SystemGamesPage.Browse }

    val onBack: () -> Unit = {
        viewModel.onGameDetailClosed()
        page = SystemGamesPage.Browse
    }
    // Step-back priority: close the detail page first if it's open (same onBack the top
    // bar's own back arrow uses), exit the whole System overlay once neither applies - same
    // one-step-at-a-time chain EditWidgetsOverlay uses, and the same fix GameManualScreen/
    // RetroAchievementsScreen apply for their own onExit - without an unconditional handler
    // here, back at the browse level fell through to MainScreen's handler underneath and did
    // nothing.
    BackHandler(enabled = true) {
        if (page is SystemGamesPage.Detail) onBack() else onExit()
    }

    Box(modifier = modifier.fillMaxSize().blockAppDrawerSwipeFallthrough()) {
        AsyncImage(
            model = fallbackBackgroundAssetPath(LocalIsDarkTheme.current),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        CompositionLocalProvider(LocalContentColor provides themedContentColor()) {
            AnimatedContent(
                targetState = page,
                transitionSpec = { drillDownTransform() },
                label = "systemGamesPage",
                modifier = Modifier.fillMaxSize(),
            ) { targetPage ->
                when (targetPage) {
                    SystemGamesPage.Browse ->
                        SystemGamesBrowsePage(
                            viewModel = viewModel,
                            listState = listState,
                            overlayOpacityPercent = overlayOpacityPercent,
                            onGameSelected = { game ->
                                viewModel.onGameSelected(game)
                                page = SystemGamesPage.Detail(game)
                            },
                            onExit = onExit,
                        )
                    is SystemGamesPage.Detail ->
                        GameDetailPage(
                            viewModel = viewModel,
                            fetch = GameDetailFetch(selectedGameFetch, selectedGameLeaderboardsFetch),
                            overlayOpacityPercent = overlayOpacityPercent,
                            onBack = onBack,
                            onExit = onExit,
                        )
                }
            }
        }
    }
}

private sealed interface SystemGamesPage {
    data object Browse : SystemGamesPage

    data class Detail(val game: RetroAchievementsCandidateGame) : SystemGamesPage
}

/**
 * Drilling into a game slides in from the right, stepping back slides in from the left -
 * mirrors typical drill-down navigation even though this isn't nav-compose under the hood,
 * the same feel [com.esde.companion.ui.main.LongPressSettingsMenu]'s own transition uses.
 * Only two pages exist here, so direction is decided directly from [targetState] rather than
 * a depth comparison.
 */
private fun AnimatedContentTransitionScope<SystemGamesPage>.drillDownTransform(): ContentTransform {
    val enteringDetail = targetState is SystemGamesPage.Detail
    val slideDistance = { width: Int -> width / PAGE_SLIDE_DIVISOR }
    val enter = tween<IntOffset>(PAGE_SLIDE_MILLIS)
    val exit = tween<Float>(PAGE_FADE_OUT_MILLIS)
    return if (enteringDetail) {
        (slideInHorizontally(enter, slideDistance) + fadeIn(tween(PAGE_SLIDE_MILLIS)))
            .togetherWith(slideOutHorizontally(enter) { -slideDistance(it) } + fadeOut(exit))
    } else {
        (slideInHorizontally(enter) { -slideDistance(it) } + fadeIn(tween(PAGE_SLIDE_MILLIS)))
            .togetherWith(slideOutHorizontally(enter, slideDistance) + fadeOut(exit))
    }
}

@Composable
private fun SystemGamesBrowsePage(
    viewModel: RetroAchievementsSystemGamesViewModel,
    listState: LazyListState,
    overlayOpacityPercent: Int,
    onGameSelected: (RetroAchievementsCandidateGame) -> Unit,
    onExit: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val filteredGames by viewModel.filteredGames.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        SystemGamesTopBar(
            title = (state as? SystemGamesUiState.Loaded)?.systemFullName,
            onExit = onExit,
        )
        if (state is SystemGamesUiState.Loaded) {
            SearchField(query = searchQuery, onQueryChanged = viewModel::onSearchQueryChanged)
            Spacer(modifier = Modifier.height(SORT_FILTER_ROW_TOP_SPACING))
            val filterControls = systemGamesFilterControls(filters, viewModel::onFiltersChanged)
            SystemGamesSortFilterRow(controls = filterControls, modifier = Modifier.padding(horizontal = 16.dp))
        }
        val listArgs =
            SystemGamesListArgs(
                overlayOpacityPercent = overlayOpacityPercent,
                listState = listState,
                onGameSelected = onGameSelected,
            )
        val isNarrowed = searchQuery.isNotBlank() || filters != SystemGameFilters()
        SystemGamesBody(
            state = state,
            games = filteredGames,
            isNarrowed = isNarrowed,
            listArgs = listArgs,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun SystemGamesTopBar(
    title: String?,
    onExit: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = CORNER_BUTTON_EDGE_PADDING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.EmojiEvents,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title?.let { "$it Achievements" } ?: "System Achievements",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onExit) {
            Icon(imageVector = Icons.Filled.Close, contentDescription = "Close")
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChanged: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        placeholder = { Text("Search games") },
        leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChanged("") }) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Clear Search")
                }
            }
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    )
}

@Composable
private fun SystemGamesBody(
    state: SystemGamesUiState,
    games: List<RetroAchievementsCandidateGame>,
    isNarrowed: Boolean,
    listArgs: SystemGamesListArgs,
    modifier: Modifier,
) {
    when (state) {
        SystemGamesUiState.NotSignedIn -> {
            val message = "Sign in to RetroAchievements (Settings > RetroAchievements) to browse games."
            RetroAchievementsMessage(message, modifier)
        }
        SystemGamesUiState.NoSystem -> {
            RetroAchievementsMessage("Browse to a system in ES-DE to see its games.", modifier)
        }
        SystemGamesUiState.UnsupportedSystem -> {
            RetroAchievementsMessage("RetroAchievements doesn't support this system.", modifier)
        }
        SystemGamesUiState.Loading ->
            Box(modifier = modifier, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        is SystemGamesUiState.Loaded ->
            if (games.isEmpty()) {
                // Distinguishes "this system genuinely has zero RA-tracked games" from "the
                // search/filter selection matched nothing" - both leave games empty, but only
                // the former is "no achievements"; the latter just needs a different/cleared
                // search or filter.
                val message = if (isNarrowed) "No games found." else "This system has no achievements."
                RetroAchievementsMessage(message, modifier)
            } else {
                GameList(games, listArgs, modifier)
            }
    }
}

/** Bundles the game list's non-data render args - see [AchievementListControls]'s kdoc for
 * why this bundling exists (keeping the functions below under detekt's parameter-count limit). */
private data class SystemGamesListArgs(
    val overlayOpacityPercent: Int,
    val listState: LazyListState,
    val onGameSelected: (RetroAchievementsCandidateGame) -> Unit,
)

@Composable
private fun GameList(
    games: List<RetroAchievementsCandidateGame>,
    listArgs: SystemGamesListArgs,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier,
        state = listArgs.listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(games, key = { it.gameId }) { game ->
            GameTile(game, listArgs.overlayOpacityPercent) { listArgs.onGameSelected(game) }
        }
    }
}

/** Same translucent rounded "button" tile convention as [RetroAchievementsScreen]'s `AchievementRow`. */
@Composable
private fun GameTile(
    game: RetroAchievementsCandidateGame,
    overlayOpacityPercent: Int,
    onClick: () -> Unit,
) {
    val tileModifier =
        Modifier
            .fillMaxWidth()
            .clip(TILE_SHAPE)
            .background(themedTileColor().copy(alpha = overlayOpacityPercent / OVERLAY_PERCENT_DIVISOR))
            .clickable(onClick = onClick)
            .padding(12.dp)

    Row(
        modifier = tileModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(model = game.iconUrl, contentDescription = null, modifier = Modifier.size(GAME_ICON_SIZE))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = game.title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "${game.numAchievements} achievements - ${game.totalPoints} points",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/**
 * Bundles the two independently-resolving fetch states [GameDetailPage] receives from its
 * caller - same "bundle related params to stay under detekt's LongParameterList limit"
 * convention [AchievementListControls]/[AchievementModeContent] use.
 */
private data class GameDetailFetch(
    val achievements: RetroAchievementsFetchState,
    val leaderboards: LeaderboardsFetchState,
)

@Composable
private fun GameDetailPage(
    viewModel: RetroAchievementsSystemGamesViewModel,
    fetch: GameDetailFetch,
    overlayOpacityPercent: Int,
    onBack: () -> Unit,
    onExit: () -> Unit,
) {
    val sortOrder by viewModel.gameSortOrder.collectAsStateWithLifecycle()
    val filter by viewModel.gameFilter.collectAsStateWithLifecycle()
    val displayField by viewModel.gameDisplayField.collectAsStateWithLifecycle()
    val expanded by viewModel.expanded.collectAsStateWithLifecycle()
    val isPlaytimeStatsHardcoreMode by viewModel.isPlaytimeStatsHardcoreMode.collectAsStateWithLifecycle()
    val playtimeStatsHardcoreMode =
        DropdownSelection(isPlaytimeStatsHardcoreMode, viewModel::onPlaytimeStatsHardcoreModeToggled)
    val achievementListControls =
        AchievementListControls(
            sort = DropdownSelection(sortOrder, viewModel::onGameSortOrderChanged),
            filter = DropdownSelection(filter, viewModel::onGameFilterChanged),
            display = DropdownSelection(displayField, viewModel::onGameDisplayFieldChanged),
            overlayOpacityPercent = overlayOpacityPercent,
            playtimeStatsHardcoreMode = playtimeStatsHardcoreMode,
        )
    val achievements =
        AchievementModeContent(
            fetch = fetch.achievements,
            listControls = achievementListControls,
            expansion = AchievementExpansionState(expanded = expanded, onTap = viewModel::onAchievementTapped),
        )

    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val leaderboardSortOrder by viewModel.leaderboardSortOrder.collectAsStateWithLifecycle()
    val leaderboardExpanded by viewModel.leaderboardExpanded.collectAsStateWithLifecycle()
    val leaderboardListControls =
        LeaderboardListControls(
            sort = DropdownSelection(leaderboardSortOrder, viewModel::onLeaderboardSortOrderChanged),
            overlayOpacityPercent = overlayOpacityPercent,
        )
    val leaderboardExpansion =
        LeaderboardExpansionState(expanded = leaderboardExpanded, onTap = viewModel::onLeaderboardTapped)
    val leaderboards =
        LeaderboardModeContent(
            fetch = fetch.leaderboards,
            listControls = leaderboardListControls,
            expansion = leaderboardExpansion,
        )

    Column(modifier = Modifier.fillMaxSize()) {
        GameDetailTopBar(onBack = onBack, onExit = onExit, onRefresh = viewModel::onRefreshRequested)
        RetroAchievementsModeBody(
            modeSelection = DropdownSelection(mode, viewModel::onModeChanged),
            achievements = achievements,
            leaderboards = leaderboards,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** Same shape as [RetroAchievementsScreen]'s top bar, but the kebab/correction menu is
 * replaced with a back arrow (returning to the game list) and a plain refresh action - the
 * game here was explicitly tapped, not automatically resolved, so there's nothing to correct,
 * and a dropdown menu isn't warranted for a single action. */
@Composable
private fun GameDetailTopBar(
    onBack: () -> Unit,
    onExit: () -> Unit,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = CORNER_BUTTON_EDGE_PADDING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = Icons.Filled.EmojiEvents, contentDescription = null, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = "Achievements", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        IconButton(onClick = onRefresh) {
            Icon(imageVector = Icons.Filled.Refresh, contentDescription = "Refresh")
        }
        IconButton(onClick = onBack) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        IconButton(onClick = onExit) {
            Icon(imageVector = Icons.Filled.Close, contentDescription = "Close")
        }
    }
}

private val SORT_FILTER_ROW_TOP_SPACING = 16.dp
private val GAME_ICON_SIZE = 48.dp
private val TILE_SHAPE = RoundedCornerShape(16.dp)
private const val PAGE_SLIDE_MILLIS = 220
private const val PAGE_FADE_OUT_MILLIS = 150
private const val PAGE_SLIDE_DIVISOR = 3

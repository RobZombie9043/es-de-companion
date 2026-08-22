package com.esde.companion.ui.retroachievements

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.esde.companion.domain.model.GameLeaderboardsSummary
import com.esde.companion.domain.model.LeaderboardEntry
import com.esde.companion.domain.model.LeaderboardSortOrder
import com.esde.companion.domain.model.LeaderboardSummary
import com.esde.companion.domain.model.LeaderboardTopEntry
import com.esde.companion.domain.model.LeaderboardUserEntry
import com.esde.companion.domain.model.sortedByLeaderboardOrder
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Bundles everything [RetroAchievementsModeBody] needs to render the Achievements facet - same
 * "bundle related params to stay under detekt's LongParameterList limit" convention
 * [AchievementListControls]/[LeaderboardListControls] use. [hashMatchIndicator] defaults to
 * [HashMatchIndicator.None] since only [RetroAchievementsScreen] has a match-confidence concept
 * to show - see [RetroAchievementsModeBody]'s kdoc.
 */
internal data class AchievementModeContent(
    val fetch: RetroAchievementsFetchState,
    val listControls: AchievementListControls,
    val expansion: AchievementExpansionState,
    val hashMatchIndicator: HashMatchIndicator = HashMatchIndicator.None,
    val onHashMatchIndicatorTapped: () -> Unit = {},
)

/**
 * Whether/how [RetroAchievementsModeBody] should show a match-confidence icon next to the game
 * title. [None] means the concept doesn't apply at all here (e.g.
 * `RetroAchievementsSystemGamesScreen`'s `GameDetailPage`, which never sets this) - not "matched,
 * nothing to show". [Matched]/[TitleOnly] are both tappable via
 * [AchievementModeContent.onHashMatchIndicatorTapped], opening the same "Supported Hashes"
 * dialog either way, so a user can see the actual hash values being compared regardless of which
 * way the match went.
 */
internal enum class HashMatchIndicator {
    None,
    Matched,
    TitleOnly,
}

/** Same bundling as [AchievementModeContent], for the Leaderboards facet. */
internal data class LeaderboardModeContent(
    val fetch: LeaderboardsFetchState,
    val listControls: LeaderboardListControls,
    val expansion: LeaderboardExpansionState,
)

/**
 * Renders a title row - the game name (plus [AchievementModeContent.hashMatchIndicator]'s
 * match-confidence icon - a title-only match isn't *wrong*, just unverified, see
 * `ResolveRetroAchievementsGameUseCase`'s kdoc) on the left, with [RetroAchievementsModeToggleRow]
 * right-aligned in the same row rather than occupying its own full-width row below - then
 * whichever facet [modeSelection] currently selects. Shared by both [RetroAchievementsScreen] and
 * [RetroAchievementsSystemGamesScreen]'s `GameDetailPage`. The toggle's item counts come straight
 * from each fetch's own `Loaded` state (`null` while still resolving), rather than a separate
 * "count" field, since both are fetched eagerly and concurrently - see
 * [RetroAchievementsViewModel]'s kdoc. The game title itself comes from the achievement fetch
 * (RA's leaderboard endpoints don't return one), so it's blank for the brief window before that
 * fetch resolves even if leaderboards happens to load first.
 */
@Composable
internal fun RetroAchievementsModeBody(
    modeSelection: DropdownSelection<RetroAchievementsMode>,
    achievements: AchievementModeContent,
    leaderboards: LeaderboardModeContent,
    modifier: Modifier,
) {
    val achievementsSummary = (achievements.fetch as? RetroAchievementsFetchState.Loaded)?.summary
    val leaderboardsCount = (leaderboards.fetch as? LeaderboardsFetchState.Loaded)?.summary?.leaderboards?.size

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = achievementsSummary?.gameTitle.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                )
                HashMatchIndicatorIcon(
                    indicator = achievements.hashMatchIndicator,
                    onTapped = achievements.onHashMatchIndicatorTapped,
                    modifier = Modifier.size(HASH_MISMATCH_ICON_SIZE),
                )
            }
            RetroAchievementsModeToggleRow(
                mode = modeSelection.value,
                achievementsCount = achievementsSummary?.achievements?.size,
                leaderboardsCount = leaderboardsCount,
                onModeChanged = modeSelection.onValueChanged,
            )
        }
        when (modeSelection.value) {
            RetroAchievementsMode.Achievements ->
                RetroAchievementsFetchBody(
                    fetch = achievements.fetch,
                    listControls = achievements.listControls,
                    expansion = achievements.expansion,
                    modifier = Modifier.fillMaxSize(),
                )
            RetroAchievementsMode.Leaderboards ->
                LeaderboardsFetchBody(
                    fetch = leaderboards.fetch,
                    listControls = leaderboards.listControls,
                    expansion = leaderboards.expansion,
                    modifier = Modifier.fillMaxSize(),
                )
        }
    }
}

/**
 * Renders [AchievementModeContent.hashMatchIndicator] - [HashMatchIndicator.Matched] uses
 * [Icons.Filled.CheckCircle] (a filled circle with a checkmark cut out) rather than a
 * hash-themed icon specifically so it visually reads as a sibling of
 * [HashMatchIndicator.TitleOnly]'s [Icons.Filled.Info] (same "filled circle, glyph cut out"
 * Material icon family), instead of two visually unrelated icons sitting side by side.
 */
@Composable
private fun HashMatchIndicatorIcon(
    indicator: HashMatchIndicator,
    onTapped: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (indicator) {
        HashMatchIndicator.None -> Unit
        HashMatchIndicator.Matched ->
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Matched by ROM hash - tap to view supported hashes",
                modifier = modifier.clickable(onClick = onTapped),
            )
        HashMatchIndicator.TitleOnly ->
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = "Matched by title, not verified by ROM hash - tap to view supported hashes",
                modifier = modifier.clickable(onClick = onTapped),
            )
    }
}

/**
 * Bundles [LeaderboardSortRow]'s selection state - same "bundle related params to stay under
 * detekt's LongParameterList limit" convention [AchievementListControls] uses.
 */
internal data class LeaderboardListControls(
    val sort: DropdownSelection<LeaderboardSortOrder>,
    val overlayOpacityPercent: Int,
)

/**
 * Backs each [LeaderboardRow]'s tap-to-expand entries section - single-expand accordion, same
 * shape as [AchievementExpansionState].
 */
internal data class LeaderboardExpansionState(
    val expanded: ExpandedLeaderboardEntries?,
    val onTap: (Long) -> Unit,
)

@Composable
internal fun LeaderboardsFetchBody(
    fetch: LeaderboardsFetchState,
    listControls: LeaderboardListControls,
    expansion: LeaderboardExpansionState,
    modifier: Modifier,
) {
    when (fetch) {
        LeaderboardsFetchState.Idle, LeaderboardsFetchState.Loading ->
            Box(modifier = modifier, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        is LeaderboardsFetchState.Loaded ->
            LeaderboardSummaryList(
                summary = fetch.summary,
                listControls = listControls,
                expansion = expansion,
                modifier = modifier,
            )
        LeaderboardsFetchState.NotFound ->
            RetroAchievementsMessage("This game's RetroAchievements entry couldn't be found.", modifier)
        is LeaderboardsFetchState.NetworkError ->
            RetroAchievementsMessage("Couldn't load leaderboards: ${fetch.message}", modifier)
    }
}

@Composable
internal fun LeaderboardSummaryList(
    summary: GameLeaderboardsSummary,
    listControls: LeaderboardListControls,
    expansion: LeaderboardExpansionState,
    modifier: Modifier,
) {
    if (summary.leaderboards.isEmpty()) {
        RetroAchievementsMessage("This game has no leaderboards.", modifier)
        return
    }

    val displayedLeaderboards = summary.leaderboards.sortedByLeaderboardOrder(listControls.sort.value)

    // Keyed on sort only - same "reset scroll on a real reordering, not on every recomposition"
    // reasoning as AchievementSummaryList's equivalent LaunchedEffect.
    val listState = rememberLazyListState()
    LaunchedEffect(listControls.sort.value) {
        listState.scrollToItem(0)
    }

    Column(modifier = modifier) {
        LeaderboardSortRow(sort = listControls.sort, modifier = Modifier.padding(horizontal = 16.dp))
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(displayedLeaderboards, key = { it.id }) { leaderboard ->
                LeaderboardRow(
                    leaderboard = leaderboard,
                    overlayOpacityPercent = listControls.overlayOpacityPercent,
                    expansion = expansion,
                )
            }
        }
    }
}

/**
 * Each leaderboard renders as its own translucent rounded "button" - same shape as [AchievementRow].
 * No per-leaderboard badge art exists on RetroAchievements, so a static icon stands in for
 * [AchievementRow]'s [coil3.compose.AsyncImage] badge. The right side shows two stacked lines -
 * the current #1 entry and the signed-in user's own entry - directly matching the "show the no 1
 * result and user on the right side of the panel" requirement. Tapping toggles a tap-to-expand
 * entries accordion below, same single-expand-at-a-time semantics as [AchievementRow]'s comments.
 */
@Composable
internal fun LeaderboardRow(
    leaderboard: LeaderboardSummary,
    overlayOpacityPercent: Int,
    expansion: LeaderboardExpansionState,
) {
    val expandedEntries = expansion.expanded.takeIf { it?.leaderboardId == leaderboard.id }
    val isExpanded = expandedEntries != null
    val tileModifier =
        Modifier
            .fillMaxWidth()
            .clip(LEADERBOARD_TILE_SHAPE)
            .background(themedTileColor().copy(alpha = overlayOpacityPercent / OVERLAY_PERCENT_DIVISOR))
            .clickable { expansion.onTap(leaderboard.id) }
            .animateContentSize()
            .padding(12.dp)

    Column(modifier = tileModifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Leaderboard,
                contentDescription = null,
                modifier = Modifier.size(LEADERBOARD_ICON_SIZE),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = leaderboard.title, style = MaterialTheme.typography.bodyLarge)
                Text(text = leaderboard.description, style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = leaderboard.topEntry.toTopEntryText(), style = MaterialTheme.typography.labelLarge)
                Text(
                    text = leaderboard.myEntry.toMyEntryText(),
                    style = MaterialTheme.typography.labelMedium,
                    color = LocalContentColor.current.copy(alpha = UNLOCK_DATE_ALPHA),
                )
            }
        }
        AnimatedVisibility(visible = isExpanded) {
            expandedEntries?.let { LeaderboardEntriesSection(it.entries) }
        }
    }
}

private fun LeaderboardTopEntry?.toTopEntryText(): String {
    return this?.let { "#1 ${it.user} · ${it.formattedScore}" } ?: "—"
}

private fun LeaderboardUserEntry?.toMyEntryText(): String {
    return this?.let { "You: #${it.rank} · ${it.formattedScore}" } ?: "Unranked"
}

@Composable
private fun LeaderboardEntriesSection(entries: LeaderboardEntriesFetchState) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        when (entries) {
            LeaderboardEntriesFetchState.Loading ->
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(ENTRIES_PROGRESS_SIZE))
                }
            is LeaderboardEntriesFetchState.Loaded ->
                if (entries.entries.isEmpty()) {
                    Text(
                        text = "No entries yet.",
                        style = MaterialTheme.typography.labelSmall,
                        color = LocalContentColor.current.copy(alpha = UNLOCK_DATE_ALPHA),
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        entries.entries.forEach { entry -> LeaderboardEntryRow(entry) }
                    }
                }
            is LeaderboardEntriesFetchState.NetworkError ->
                Text(
                    text = "Couldn't load entries: ${entries.message}",
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalContentColor.current.copy(alpha = UNLOCK_DATE_ALPHA),
                )
        }
    }
}

@Composable
private fun LeaderboardEntryRow(entry: LeaderboardEntry) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "#${entry.rank}", style = MaterialTheme.typography.bodyMedium)
        Text(text = entry.user, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(text = entry.formattedScore, style = MaterialTheme.typography.bodyMedium)
        entry.submittedAtMillis?.let { submittedAtMillis ->
            Text(
                text = formatEntryDate(submittedAtMillis),
                style = MaterialTheme.typography.labelSmall,
                color = LocalContentColor.current.copy(alpha = UNLOCK_DATE_ALPHA),
            )
        }
    }
}

private fun formatEntryDate(submittedAtMillis: Long): String {
    val date = Instant.ofEpochMilli(submittedAtMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    return date.format(ENTRY_DATE_FORMAT)
}

private val ENTRY_DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy")
private val LEADERBOARD_ICON_SIZE = 32.dp
private val ENTRIES_PROGRESS_SIZE = 20.dp
private const val UNLOCK_DATE_ALPHA = 0.6f
private val LEADERBOARD_TILE_SHAPE = RoundedCornerShape(16.dp)
private val HASH_MISMATCH_ICON_SIZE = 18.dp

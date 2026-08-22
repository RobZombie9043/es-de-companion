// One data class among several functions here - naming the file after it would be misleading.
@file:Suppress("MatchingDeclarationName")

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.esde.companion.domain.model.AchievementComment
import com.esde.companion.domain.model.AchievementDisplayField
import com.esde.companion.domain.model.AchievementFilterOption
import com.esde.companion.domain.model.AchievementItem
import com.esde.companion.domain.model.AchievementSortOrder
import com.esde.companion.domain.model.GameAchievementSummary
import com.esde.companion.domain.model.filteredByAchievementFilters
import com.esde.companion.domain.model.sortedByAchievementOrder
import com.esde.companion.domain.model.valueFor
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

// The data-driven achievement-summary body shared by RetroAchievementsScreen (the live game
// ES-DE is currently showing) and RetroAchievementsSystemGamesScreen's per-game drill-down
// page - both render the identical fetch-state/sort-filter/achievement-list body once a
// game's RA gameId is known; only how that gameId was identified (live resolution vs. an
// explicit tap) and the top bar differ between the two callers.

/**
 * Bundles the three dropdowns' selection state - see [AchievementSortFilterRow] - so each is
 * one [DropdownSelection] (value + its own setter, can't drift apart) rather than a value and a
 * setter passed as two separate fields, and so the functions below stay well under detekt's
 * parameter-count limit despite there now being three independent controls.
 * [playtimeStatsHardcoreMode] backs [GamePlaytimeStatsRow]'s Casual/Hardcore toggle - global
 * (not per-game) persisted state owned by the ViewModel, not local Compose state, so switching
 * games (or screens) doesn't reset it - see `OnboardingRepository.observePlaytimeStatsHardcoreModeEnabled`'s kdoc.
 */
internal data class AchievementListControls(
    val sort: DropdownSelection<AchievementSortOrder>,
    val filter: DropdownSelection<Set<AchievementFilterOption>>,
    val display: DropdownSelection<AchievementDisplayField>,
    val overlayOpacityPercent: Int,
    val playtimeStatsHardcoreMode: DropdownSelection<Boolean>,
)

/**
 * Backs each [AchievementRow]'s tap-to-expand comments section - single-expand accordion, so
 * [expanded] is the one achievement (if any) currently showing its comments, paired atomically
 * with its fetch state (see [ExpandedAchievementComments]'s kdoc for why that pairing matters).
 * Bundled into one param for the same detekt parameter-count reason [AchievementListControls] is.
 */
internal data class AchievementExpansionState(
    val expanded: ExpandedAchievementComments?,
    val onTap: (Long) -> Unit,
)

@Composable
internal fun RetroAchievementsFetchBody(
    fetch: RetroAchievementsFetchState,
    listControls: AchievementListControls,
    expansion: AchievementExpansionState,
    modifier: Modifier,
) {
    when (fetch) {
        RetroAchievementsFetchState.Idle, RetroAchievementsFetchState.Loading ->
            Box(modifier = modifier, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        is RetroAchievementsFetchState.Loaded ->
            AchievementSummaryList(
                summary = fetch.summary,
                listControls = listControls,
                expansion = expansion,
                modifier = modifier,
            )
        RetroAchievementsFetchState.NotFound ->
            RetroAchievementsMessage("This game's RetroAchievements entry couldn't be found.", modifier)
        is RetroAchievementsFetchState.NetworkError ->
            RetroAchievementsMessage("Couldn't load achievements: ${fetch.message}", modifier)
    }
}

@Composable
internal fun RetroAchievementsMessage(
    text: String,
    modifier: Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(32.dp),
        )
    }
}

@Composable
internal fun AchievementSummaryList(
    summary: GameAchievementSummary,
    listControls: AchievementListControls,
    expansion: AchievementExpansionState,
    modifier: Modifier,
) {
    if (summary.achievements.isEmpty()) {
        RetroAchievementsMessage("This game has no achievements.", modifier)
        return
    }

    val displayedAchievements =
        summary.achievements
            .filteredByAchievementFilters(listControls.filter.value)
            .sortedByAchievementOrder(listControls.sort.value)

    // Keyed only on sort+filter - deliberately not on the Display selection (it changes what's
    // printed on each row, not which achievements show or their order, so resetting scroll
    // there would yank the list away from whatever the user is currently comparing) and not on
    // the whole listControls object (its DropdownSelection fields carry function references -
    // e.g. bound method references like viewModel::onSortOrderChanged - that construct a new
    // instance per composition, so keying on the container would re-fire this on nearly every
    // recomposition and fight the user's own scrolling).
    val listState = rememberLazyListState()
    LaunchedEffect(listControls.sort.value, listControls.filter.value) {
        listState.scrollToItem(0)
    }

    Column(modifier = modifier) {
        AchievementStatsHeader(summary)
        GamePlaytimeStatsRow(stats = summary.playtimeStats, hardcoreMode = listControls.playtimeStatsHardcoreMode)
        AchievementSortFilterRow(
            sort = listControls.sort,
            filter = listControls.filter,
            display = listControls.display,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        if (displayedAchievements.isEmpty()) {
            RetroAchievementsMessage("No achievements match this filter.", Modifier.fillMaxSize())
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(displayedAchievements, key = { it.id }) { achievement ->
                    AchievementRow(
                        achievement = achievement,
                        overlayOpacityPercent = listControls.overlayOpacityPercent,
                        displayField = listControls.display.value,
                        totalPlayers = summary.totalPlayers,
                        expansion = expansion,
                    )
                }
            }
        }
    }
}

/**
 * The game title itself (plus the hash-match-confidence icon) now lives in the shared title row
 * next to the Achievements/Leaderboards toggle - see [RetroAchievementsModeBody]'s kdoc - so this
 * only renders the unlock/points/completion stats line beneath it.
 */
@Composable
internal fun AchievementStatsHeader(summary: GameAchievementSummary) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 8.dp),
    ) {
        val unlockedCount = summary.achievements.count { it.unlocked }
        val completionPercent = summary.completionPercent.roundToInt()
        val statsText =
            "$unlockedCount / ${summary.achievements.size} Achievements  -  " +
                "${summary.earnedPoints} / ${summary.totalPoints} points - $completionPercent%"
        Text(text = statsText, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * Each achievement renders as its own translucent rounded "button" - the same
 * black-in-dark/white-in-light overlay-surface convention [com.esde.companion.ui.CornerFab]/
 * the App Dock/music panel use, at the shared Overlay Opacity value - rather than a plain
 * row of text, so the list reads consistently with the rest of the app's floating chrome
 * against the full-bleed background image (see [RetroAchievementsScreen]'s kdoc).
 *
 * Tapping the tile toggles its wall-comments section below the existing content ([expansion] -
 * single-expand accordion, see [AchievementExpansionState]'s kdoc). The row is keyed by
 * [AchievementItem.id] at the `LazyColumn` call site, so toggling one row's expansion never
 * remounts a sibling row - see CLAUDE.md's `CrossfadeAsyncImage` gotcha for why identity
 * stability matters here. `animateContentSize()` smoothly grows/shrinks the tile as the
 * comments section's own loading/loaded/error states change height.
 */
@Composable
internal fun AchievementRow(
    achievement: AchievementItem,
    overlayOpacityPercent: Int,
    displayField: AchievementDisplayField,
    totalPlayers: Int,
    expansion: AchievementExpansionState,
) {
    val contentAlpha = if (achievement.unlocked) 1f else UNEARNED_ALPHA
    val expandedComments = expansion.expanded.takeIf { it?.achievementId == achievement.id }
    val isExpanded = expandedComments != null
    val tileModifier =
        Modifier
            .fillMaxWidth()
            .alpha(contentAlpha)
            .clip(ACHIEVEMENT_TILE_SHAPE)
            .background(themedTileColor().copy(alpha = overlayOpacityPercent / OVERLAY_PERCENT_DIVISOR))
            .clickable { expansion.onTap(achievement.id) }
            .animateContentSize()
            .padding(12.dp)

    Column(modifier = tileModifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = achievement.badgeUrl,
                contentDescription = null,
                modifier = Modifier.size(ACHIEVEMENT_BADGE_SIZE),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = achievement.title, style = MaterialTheme.typography.bodyLarge)
                Text(text = achievement.description, style = MaterialTheme.typography.bodySmall)
                achievement.unlockedAt?.let { unlockedAt ->
                    Text(
                        text = "Unlocked ${formatUnlockDate(unlockedAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = LocalContentColor.current.copy(alpha = UNLOCK_DATE_ALPHA),
                    )
                }
            }
            Text(text = displayField.valueFor(achievement, totalPlayers), style = MaterialTheme.typography.labelLarge)
        }
        AnimatedVisibility(visible = isExpanded) {
            expandedComments?.let { AchievementCommentsSection(it.comments) }
        }
    }
}

@Composable
private fun AchievementCommentsSection(comments: CommentsFetchState) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        when (comments) {
            CommentsFetchState.Loading ->
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(COMMENTS_PROGRESS_SIZE))
                }
            is CommentsFetchState.Loaded ->
                if (comments.comments.isEmpty()) {
                    Text(
                        text = "No comments yet.",
                        style = MaterialTheme.typography.labelSmall,
                        color = LocalContentColor.current.copy(alpha = UNLOCK_DATE_ALPHA),
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        comments.comments.forEach { comment -> AchievementCommentRow(comment) }
                    }
                }
            is CommentsFetchState.NetworkError ->
                Text(
                    text = "Couldn't load comments: ${comments.message}",
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalContentColor.current.copy(alpha = UNLOCK_DATE_ALPHA),
                )
        }
    }
}

@Composable
private fun AchievementCommentRow(comment: AchievementComment) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = comment.user, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            comment.submittedAtMillis?.let { submittedAtMillis ->
                Text(
                    text = formatCommentDate(submittedAtMillis),
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalContentColor.current.copy(alpha = UNLOCK_DATE_ALPHA),
                )
            }
        }
        Text(text = comment.text, style = MaterialTheme.typography.bodySmall)
    }
}

private fun formatUnlockDate(unlockedAtMillis: Long): String {
    val date = Instant.ofEpochMilli(unlockedAtMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    return date.format(UNLOCK_DATE_FORMAT)
}

private fun formatCommentDate(submittedAtMillis: Long): String {
    val date = Instant.ofEpochMilli(submittedAtMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    return date.format(UNLOCK_DATE_FORMAT)
}

private val ACHIEVEMENT_BADGE_SIZE = 48.dp
private val COMMENTS_PROGRESS_SIZE = 20.dp
private val UNLOCK_DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy")
private const val UNLOCK_DATE_ALPHA = 0.6f
private val ACHIEVEMENT_TILE_SHAPE = RoundedCornerShape(16.dp)
private const val UNEARNED_ALPHA = 0.5f

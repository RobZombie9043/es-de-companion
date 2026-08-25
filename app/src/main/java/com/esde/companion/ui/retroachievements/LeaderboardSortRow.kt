package com.esde.companion.ui.retroachievements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.esde.companion.domain.model.LeaderboardSortOrder

/**
 * The sort control above the leaderboard list - same [FlowRow]/[SingleSelectDropdownChip] shape
 * as [AchievementSortFilterRow], but with no filter/display chip (neither applies to
 * leaderboards - see [LeaderboardSortOrder]'s kdoc).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LeaderboardSortRow(
    sort: DropdownSelection<LeaderboardSortOrder>,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SingleSelectDropdownChip(
            title = "Sort Order",
            options = LeaderboardSortOrder.entries.map { DropdownOption(it, it.displayLabel(), it.icon()) },
            selection = sort,
        )
    }
}

private fun LeaderboardSortOrder.displayLabel(): String =
    when (this) {
        LeaderboardSortOrder.DisplayOrderFirst -> "Display Order (First)"
        LeaderboardSortOrder.DisplayOrderLast -> "Display Order (Last)"
        LeaderboardSortOrder.TitleAToZ -> "Title (A - Z)"
        LeaderboardSortOrder.TitleZToA -> "Title (Z - A)"
        LeaderboardSortOrder.MyRankBest -> "My Rank (Best)"
        LeaderboardSortOrder.MyRankWorst -> "My Rank (Worst)"
    }

private fun LeaderboardSortOrder.icon(): ImageVector =
    when (this) {
        LeaderboardSortOrder.DisplayOrderFirst -> Icons.AutoMirrored.Filled.List
        LeaderboardSortOrder.DisplayOrderLast -> Icons.AutoMirrored.Filled.List
        LeaderboardSortOrder.TitleAToZ -> Icons.Filled.SortByAlpha
        LeaderboardSortOrder.TitleZToA -> Icons.Filled.SortByAlpha
        LeaderboardSortOrder.MyRankBest -> Icons.Filled.EmojiEvents
        LeaderboardSortOrder.MyRankWorst -> Icons.Filled.EmojiEvents
    }

// Multiple top-level declarations (a data class plus several functions) here - naming the file
// after any single one of them would be misleading.
@file:Suppress("MatchingDeclarationName")

package com.esde.companion.ui.retroachievements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cottage
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.HighlightOff
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.esde.companion.domain.model.HasAchievementsFilter
import com.esde.companion.domain.model.ProgressFilter
import com.esde.companion.domain.model.RetroGameType
import com.esde.companion.domain.model.SystemGameFilters
import com.esde.companion.domain.model.SystemGameSortOrder

/**
 * Bundles the system-wide games browser's four dropdown selections - see
 * [AchievementListControls]'s kdoc for why this bundling exists (keeping the functions below
 * under detekt's parameter-count limit despite four independent controls).
 */
internal data class SystemGamesFilterControls(
    val sort: DropdownSelection<SystemGameSortOrder>,
    val gameTypes: DropdownSelection<Set<RetroGameType>>,
    val hasAchievements: DropdownSelection<HasAchievementsFilter>,
    val progress: DropdownSelection<ProgressFilter>,
)

/** Builds each field's [DropdownSelection] from one [SystemGameFilters] value + setter pair. */
internal fun systemGamesFilterControls(
    filters: SystemGameFilters,
    onFiltersChanged: (SystemGameFilters) -> Unit,
): SystemGamesFilterControls {
    val hasAchievementsSelection =
        DropdownSelection(filters.hasAchievements) { onFiltersChanged(filters.copy(hasAchievements = it)) }
    return SystemGamesFilterControls(
        sort = DropdownSelection(filters.sortOrder) { onFiltersChanged(filters.copy(sortOrder = it)) },
        gameTypes = DropdownSelection(filters.gameTypes) { onFiltersChanged(filters.copy(gameTypes = it)) },
        hasAchievements = hasAchievementsSelection,
        progress = DropdownSelection(filters.progress) { onFiltersChanged(filters.copy(progress = it)) },
    )
}

/**
 * Forced into its own file - [RetroAchievementsSystemGamesScreen] is already at detekt's
 * 10-function-per-file ceiling.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SystemGamesSortFilterRow(
    controls: SystemGamesFilterControls,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SingleSelectDropdownChip(
            title = "Sort Order",
            options = SystemGameSortOrder.entries.map { DropdownOption(it, it.displayLabel(), it.icon()) },
            selection = controls.sort,
        )
        MultiSelectDropdownChip(
            title = "Game Type",
            options = RetroGameType.entries.map { DropdownOption(it, it.displayLabel(), it.icon()) },
            selection = controls.gameTypes,
        )
        SingleSelectDropdownChip(
            title = "Has Achievements",
            options = HasAchievementsFilter.entries.map { DropdownOption(it, it.displayLabel(), it.icon()) },
            selection = controls.hasAchievements,
        )
        SingleSelectDropdownChip(
            title = "Progress",
            options = ProgressFilter.entries.map { DropdownOption(it, it.displayLabel(), it.icon()) },
            selection = controls.progress,
        )
    }
}

private fun SystemGameSortOrder.displayLabel(): String =
    when (this) {
        SystemGameSortOrder.AchievementsMost -> "Achievements (Most)"
        SystemGameSortOrder.AchievementsLeast -> "Achievements (Least)"
        SystemGameSortOrder.PointsMost -> "Points (Most)"
        SystemGameSortOrder.PointsLeast -> "Points (Least)"
        SystemGameSortOrder.ProgressMost -> "Progress (Most)"
        SystemGameSortOrder.ProgressLeast -> "Progress (Least)"
        SystemGameSortOrder.TitleAToZ -> "Title (A - Z)"
        SystemGameSortOrder.TitleZToA -> "Title (Z - A)"
    }

private fun SystemGameSortOrder.icon(): ImageVector =
    when (this) {
        SystemGameSortOrder.AchievementsMost -> Icons.Filled.EmojiEvents
        SystemGameSortOrder.AchievementsLeast -> Icons.Filled.EmojiEvents
        SystemGameSortOrder.PointsMost -> Icons.Filled.Star
        SystemGameSortOrder.PointsLeast -> Icons.Filled.Star
        SystemGameSortOrder.ProgressMost -> Icons.Filled.DonutLarge
        SystemGameSortOrder.ProgressLeast -> Icons.Filled.DonutLarge
        SystemGameSortOrder.TitleAToZ -> Icons.Filled.SortByAlpha
        SystemGameSortOrder.TitleZToA -> Icons.Filled.SortByAlpha
    }

private fun RetroGameType.displayLabel(): String =
    when (this) {
        RetroGameType.Retail -> "Retail"
        RetroGameType.Hack -> "Hack"
        RetroGameType.Homebrew -> "Homebrew"
        RetroGameType.Prototype -> "Prototype"
        RetroGameType.Unlicensed -> "Unlicensed"
        RetroGameType.Demo -> "Demo"
    }

private fun RetroGameType.icon(): ImageVector =
    when (this) {
        RetroGameType.Retail -> Icons.Filled.Storefront
        RetroGameType.Hack -> Icons.Filled.Extension
        RetroGameType.Homebrew -> Icons.Filled.Cottage
        RetroGameType.Prototype -> Icons.Filled.Science
        RetroGameType.Unlicensed -> Icons.Filled.Block
        RetroGameType.Demo -> Icons.Filled.PlayCircleOutline
    }

private fun HasAchievementsFilter.displayLabel(): String =
    when (this) {
        HasAchievementsFilter.Yes -> "Yes"
        HasAchievementsFilter.No -> "No"
        HasAchievementsFilter.Both -> "Both"
    }

private fun HasAchievementsFilter.icon(): ImageVector =
    when (this) {
        HasAchievementsFilter.Yes -> Icons.Filled.CheckCircle
        HasAchievementsFilter.No -> Icons.Filled.HighlightOff
        HasAchievementsFilter.Both -> Icons.Filled.AllInclusive
    }

private fun ProgressFilter.displayLabel(): String =
    when (this) {
        ProgressFilter.AllGames -> "All Games"
        ProgressFilter.None -> "None"
        ProgressFilter.Some -> "Some"
        ProgressFilter.Beaten -> "Completed"
        ProgressFilter.Mastered -> "Mastered"
    }

private fun ProgressFilter.icon(): ImageVector =
    when (this) {
        ProgressFilter.AllGames -> Icons.Filled.Apps
        ProgressFilter.None -> Icons.Filled.RadioButtonUnchecked
        ProgressFilter.Some -> Icons.Filled.Timelapse
        ProgressFilter.Beaten -> Icons.Filled.CheckCircle
        ProgressFilter.Mastered -> Icons.Filled.EmojiEvents
    }

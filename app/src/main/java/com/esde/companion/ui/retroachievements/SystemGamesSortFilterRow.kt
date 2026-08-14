// Multiple top-level declarations (a data class plus several functions) here - naming the file
// after any single one of them would be misleading.
@file:Suppress("MatchingDeclarationName")

package com.esde.companion.ui.retroachievements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
            options = SystemGameSortOrder.entries.map { DropdownOption(it, it.displayLabel()) },
            selection = controls.sort,
        )
        MultiSelectDropdownChip(
            title = "Game Type",
            options = RetroGameType.entries.map { DropdownOption(it, it.displayLabel()) },
            selection = controls.gameTypes,
        )
        SingleSelectDropdownChip(
            title = "Has Achievements",
            options = HasAchievementsFilter.entries.map { DropdownOption(it, it.displayLabel()) },
            selection = controls.hasAchievements,
        )
        SingleSelectDropdownChip(
            title = "Progress",
            options = ProgressFilter.entries.map { DropdownOption(it, it.displayLabel()) },
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

private fun RetroGameType.displayLabel(): String =
    when (this) {
        RetroGameType.Retail -> "Retail"
        RetroGameType.Hack -> "Hack"
        RetroGameType.Homebrew -> "Homebrew"
        RetroGameType.Prototype -> "Prototype"
        RetroGameType.Unlicensed -> "Unlicensed"
        RetroGameType.Demo -> "Demo"
    }

private fun HasAchievementsFilter.displayLabel(): String =
    when (this) {
        HasAchievementsFilter.Yes -> "Yes"
        HasAchievementsFilter.No -> "No"
        HasAchievementsFilter.Both -> "Both"
    }

private fun ProgressFilter.displayLabel(): String =
    when (this) {
        ProgressFilter.AllGames -> "All Games"
        ProgressFilter.None -> "None"
        ProgressFilter.Some -> "Some"
        ProgressFilter.Beaten -> "Completed"
        ProgressFilter.Mastered -> "Mastered"
    }

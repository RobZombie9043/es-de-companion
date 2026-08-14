package com.esde.companion.ui.retroachievements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.esde.companion.domain.model.AchievementDisplayField
import com.esde.companion.domain.model.AchievementFilterOption
import com.esde.companion.domain.model.AchievementSortOrder

/**
 * The sort/filter/display controls above the achievement list - same options RetroAchievements'
 * own website offers (see [AchievementSortOrder]/[AchievementFilterOption]), presented as small
 * dropdown chips rather than a settings-style panel, since this is a transient view preference
 * for the current visit, not something worth a dedicated settings screen. A [FlowRow] rather
 * than a plain `Row` since three chips (four on the system games browser's own row, see
 * `SystemGamesSortFilterRow`) don't reliably fit one line on a phone-width screen - excess
 * chips wrap to a second line instead of being clipped or scrolled off undiscoverably.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AchievementSortFilterRow(
    sort: DropdownSelection<AchievementSortOrder>,
    filter: DropdownSelection<Set<AchievementFilterOption>>,
    display: DropdownSelection<AchievementDisplayField>,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SingleSelectDropdownChip(
            title = "Sort Order",
            options = AchievementSortOrder.entries.map { DropdownOption(it, it.displayLabel()) },
            selection = sort,
        )
        MultiSelectDropdownChip(
            title = "Filter",
            options = AchievementFilterOption.entries.map { DropdownOption(it, it.displayLabel()) },
            selection = filter,
        )
        SingleSelectDropdownChip(
            title = "Display",
            options = AchievementDisplayField.entries.map { DropdownOption(it, it.displayLabel()) },
            selection = display,
        )
    }
}

private fun AchievementSortOrder.displayLabel(): String =
    when (this) {
        AchievementSortOrder.UnlockedFirst -> "Unlocked First"
        AchievementSortOrder.DisplayOrderFirst -> "Display Order (First)"
        AchievementSortOrder.DisplayOrderLast -> "Display Order (Last)"
        AchievementSortOrder.WonByMost -> "Won By (Most)"
        AchievementSortOrder.WonByLeast -> "Won By (Least)"
        AchievementSortOrder.PointsMost -> "Points (Most)"
        AchievementSortOrder.PointsLeast -> "Points (Least)"
        AchievementSortOrder.TitleAToZ -> "Title (A - Z)"
        AchievementSortOrder.TitleZToA -> "Title (Z - A)"
        AchievementSortOrder.TypeAscending -> "Type (Asc)"
        AchievementSortOrder.TypeDescending -> "Type (Desc)"
    }

private fun AchievementFilterOption.displayLabel(): String =
    when (this) {
        AchievementFilterOption.MissableOnly -> "Missable Only"
        AchievementFilterOption.LockedOnly -> "Locked Only"
    }

private fun AchievementDisplayField.displayLabel(): String =
    when (this) {
        AchievementDisplayField.UnlockRate -> "Unlock Rate"
        AchievementDisplayField.Points -> "Points"
        AchievementDisplayField.TotalUnlocks -> "Total Unlocks"
        AchievementDisplayField.HardcoreUnlocks -> "Hardcore Unlocks"
    }

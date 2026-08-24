package com.esde.companion.ui.retroachievements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
            options = AchievementSortOrder.entries.map { DropdownOption(it, it.displayLabel(), it.icon()) },
            selection = sort,
        )
        MultiSelectDropdownChip(
            title = "Filter",
            options = AchievementFilterOption.entries.map { DropdownOption(it, it.displayLabel(), it.icon()) },
            selection = filter,
        )
        SingleSelectDropdownChip(
            title = "Display",
            options = AchievementDisplayField.entries.map { DropdownOption(it, it.displayLabel(), it.icon()) },
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

private fun AchievementSortOrder.icon(): ImageVector =
    when (this) {
        AchievementSortOrder.UnlockedFirst -> Icons.Filled.LockOpen
        AchievementSortOrder.DisplayOrderFirst -> Icons.AutoMirrored.Filled.List
        AchievementSortOrder.DisplayOrderLast -> Icons.AutoMirrored.Filled.List
        AchievementSortOrder.WonByMost -> Icons.Filled.People
        AchievementSortOrder.WonByLeast -> Icons.Filled.People
        AchievementSortOrder.PointsMost -> Icons.Filled.Star
        AchievementSortOrder.PointsLeast -> Icons.Filled.Star
        AchievementSortOrder.TitleAToZ -> Icons.Filled.SortByAlpha
        AchievementSortOrder.TitleZToA -> Icons.Filled.SortByAlpha
        AchievementSortOrder.TypeAscending -> Icons.Filled.Category
        AchievementSortOrder.TypeDescending -> Icons.Filled.Category
    }

private fun AchievementFilterOption.displayLabel(): String =
    when (this) {
        AchievementFilterOption.MissableOnly -> "Missable Only"
        AchievementFilterOption.LockedOnly -> "Locked Only"
    }

private fun AchievementFilterOption.icon(): ImageVector =
    when (this) {
        AchievementFilterOption.MissableOnly -> Icons.Filled.Warning
        AchievementFilterOption.LockedOnly -> Icons.Filled.Lock
    }

private fun AchievementDisplayField.displayLabel(): String =
    when (this) {
        AchievementDisplayField.UnlockRate -> "Unlock Rate"
        AchievementDisplayField.Points -> "Points"
        AchievementDisplayField.TotalUnlocks -> "Total Unlocks"
        AchievementDisplayField.HardcoreUnlocks -> "Hardcore Unlocks"
    }

private fun AchievementDisplayField.icon(): ImageVector =
    when (this) {
        AchievementDisplayField.UnlockRate -> Icons.Filled.Percent
        AchievementDisplayField.Points -> Icons.Filled.Star
        AchievementDisplayField.TotalUnlocks -> Icons.Filled.People
        AchievementDisplayField.HardcoreUnlocks -> Icons.Filled.FitnessCenter
    }

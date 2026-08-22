package com.esde.companion.ui.retroachievements

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * The Achievements/Leaderboards chip toggle - deliberately small (icon + count only, no label
 * text) and content-width rather than [androidx.compose.material3.SingleChoiceSegmentedButtonRow]'s
 * full-width/min-touch-target sizing, so it can sit right-aligned in the same row as the game
 * title (see [RetroAchievementsModeBody]) instead of occupying its own full-width row. Each chip
 * shows an icon and the corresponding fetch's item count - `null` (its own fetch hasn't resolved
 * yet) renders as a dash rather than blocking the toggle itself from being usable.
 */
@Composable
internal fun RetroAchievementsModeToggleRow(
    mode: RetroAchievementsMode,
    achievementsCount: Int?,
    leaderboardsCount: Int?,
    onModeChanged: (RetroAchievementsMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ModeChip(
            selected = mode == RetroAchievementsMode.Achievements,
            icon = Icons.Filled.EmojiEvents,
            count = achievementsCount,
            onClick = { onModeChanged(RetroAchievementsMode.Achievements) },
        )
        ModeChip(
            selected = mode == RetroAchievementsMode.Leaderboards,
            icon = Icons.Filled.Leaderboard,
            count = leaderboardsCount,
            onClick = { onModeChanged(RetroAchievementsMode.Leaderboards) },
        )
    }
}

@Composable
private fun ModeChip(
    selected: Boolean,
    icon: ImageVector,
    count: Int?,
    onClick: () -> Unit,
) {
    val backgroundColor =
        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else LocalContentColor.current
    val chipModifier =
        Modifier
            .clip(CHIP_SHAPE)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    Row(
        modifier = chipModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(14.dp))
        Text(text = count?.toString() ?: "—", style = MaterialTheme.typography.labelSmall, color = contentColor)
    }
}

private val CHIP_SHAPE = RoundedCornerShape(8.dp)

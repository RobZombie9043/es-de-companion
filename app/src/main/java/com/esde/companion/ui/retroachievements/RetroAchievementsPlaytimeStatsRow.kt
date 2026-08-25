package com.esde.companion.ui.retroachievements

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.esde.companion.domain.model.GamePlaytimeStats

/**
 * Mirrors RA's own game-page "Playtime Stats" widget (community-wide median times, not the
 * signed-in user's own playtime - see [GamePlaytimeStats]'s kdoc). Rendered directly below
 * [AchievementStatsHeader] in [AchievementSummaryList]. Tapping toggles [hardcoreMode] between
 * the softcore ("Casual") and hardcore medians - a ViewModel-owned, DataStore-persisted global
 * preference (see `OnboardingRepository.observePlaytimeStatsHardcoreModeEnabled`'s kdoc), not
 * local Compose state, so the choice survives switching games/screens rather than resetting
 * every time this composable remounts. Renders nothing at all if [stats] is null (the
 * progression fetch failed or hasn't landed yet - see
 * `RetroClientRetroAchievementsApi.getGameInfoAndUserProgress`'s graceful-degradation kdoc); a
 * milestone nobody's reached yet renders as "-" for just that number rather than hiding the
 * whole line.
 */
@Composable
internal fun GamePlaytimeStatsRow(
    stats: GamePlaytimeStats?,
    hardcoreMode: DropdownSelection<Boolean>,
) {
    if (stats == null) return
    val isHardcore = hardcoreMode.value
    val beatSeconds = if (isHardcore) stats.beatHardcoreSeconds else stats.beatSeconds
    val secondMilestoneLabel = if (isHardcore) "Mastered" else "Completed"
    val secondMilestoneSeconds = if (isHardcore) stats.masteredSeconds else stats.completedSeconds
    val modeLabel = if (isHardcore) "Hardcore" else "Casual"
    val statsText =
        "Beat the game ${formatMedianDuration(beatSeconds)}  -  " +
            "$secondMilestoneLabel ${formatMedianDuration(secondMilestoneSeconds)} - $modeLabel"
    val rowModifier =
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            .clickable { hardcoreMode.onValueChanged(!isHardcore) }
    Text(text = statsText, style = MaterialTheme.typography.bodySmall, modifier = rowModifier)
}

private fun formatMedianDuration(totalSeconds: Int?): String {
    if (totalSeconds == null) return "-"
    val hours = totalSeconds / SECONDS_PER_HOUR
    val minutes = (totalSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
    return "${hours}h ${minutes}m"
}

private const val SECONDS_PER_HOUR = 3600
private const val SECONDS_PER_MINUTE = 60

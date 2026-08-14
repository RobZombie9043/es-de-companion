package com.esde.companion.domain.model

import java.util.Locale

private const val UNLOCK_RATE_PERCENT_MULTIPLIER = 100f

/**
 * What each achievement row shows on its right side - a view preference independent of
 * sorting/filtering. [UnlockRate] is the default since it's the one figure RA's own website
 * leads with, but it isn't a field RA's API returns directly (see [valueFor]) - everything else
 * here is a straight passthrough of an existing [AchievementItem] field.
 */
enum class AchievementDisplayField {
    UnlockRate,
    Points,
    TotalUnlocks,
    HardcoreUnlocks,
}

/**
 * [totalPlayers] is the achievement's *game's* total player count (see
 * [GameAchievementSummary.totalPlayers]), not something carried per-achievement - RA's API has
 * no per-achievement unlock-rate field, so [UnlockRate] is computed here as
 * `numAwarded / totalPlayers`.
 */
fun AchievementDisplayField.valueFor(
    achievement: AchievementItem,
    totalPlayers: Int,
): String =
    when (this) {
        AchievementDisplayField.UnlockRate -> unlockRateText(achievement.numAwarded, totalPlayers)
        AchievementDisplayField.Points -> "${achievement.points}"
        AchievementDisplayField.TotalUnlocks -> "${achievement.numAwarded}"
        AchievementDisplayField.HardcoreUnlocks -> "${achievement.numAwardedHardcore}"
    }

private fun unlockRateText(
    numAwarded: Int,
    totalPlayers: Int,
): String {
    if (totalPlayers <= 0) return "—"
    val rate = numAwarded * UNLOCK_RATE_PERCENT_MULTIPLIER / totalPlayers
    return String.format(Locale.US, "%.2f%%", rate)
}

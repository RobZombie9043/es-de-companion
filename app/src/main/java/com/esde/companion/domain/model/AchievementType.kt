package com.esde.companion.domain.model

/**
 * RetroAchievements' achievement `type` classification. `null` on [AchievementItem] means a
 * standard achievement (RA's API returns an empty/absent type string for these, not a
 * distinct value of its own).
 */
enum class AchievementType {
    Missable,
    Progression,
    WinCondition,
}

/**
 * Maps RA's raw API `type` string (e.g. `"missable"`, `"win_condition"`) onto
 * [AchievementType], or null for a standard achievement.
 */
fun String?.toAchievementType(): AchievementType? =
    when (this?.lowercase()) {
        "missable" -> AchievementType.Missable
        "progression" -> AchievementType.Progression
        "win_condition" -> AchievementType.WinCondition
        else -> null
    }

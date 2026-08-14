package com.esde.companion.domain.model

/**
 * A single RetroAchievements achievement and its unlock state for the signed-in user.
 * [displayOrder]/[numAwarded]/[type] back the sort/filter options on the achievement list (see
 * [AchievementSortOrder]/[AchievementFilterOption]) - not just display metadata.
 * [numAwardedHardcore] is a distinct count from [numAwarded] (RA tracks hardcore unlocks
 * separately from all unlocks) and, together with [numAwarded], backs the row's selectable
 * [AchievementDisplayField].
 */
data class AchievementItem(
    val id: Long,
    val title: String,
    val description: String,
    val points: Int,
    val badgeUrl: String?,
    val unlocked: Boolean,
    val unlockedAt: Long?,
    val displayOrder: Int = 0,
    val numAwarded: Int = 0,
    val numAwardedHardcore: Int = 0,
    val type: AchievementType? = null,
)

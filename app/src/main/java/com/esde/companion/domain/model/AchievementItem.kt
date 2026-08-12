package com.esde.companion.domain.model

/** A single RetroAchievements achievement and its unlock state for the signed-in user. */
data class AchievementItem(
    val id: Long,
    val title: String,
    val description: String,
    val points: Int,
    val badgeUrl: String?,
    val unlocked: Boolean,
    val unlockedAt: Long?,
)

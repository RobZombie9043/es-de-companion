package com.esde.companion.domain.model

/** A resolved RetroAchievements game's achievement summary for the signed-in user. */
data class GameAchievementSummary(
    val gameId: Long,
    val gameTitle: String,
    val totalPoints: Int,
    val earnedPoints: Int,
    val completionPercent: Float,
    val achievements: List<AchievementItem>,
)

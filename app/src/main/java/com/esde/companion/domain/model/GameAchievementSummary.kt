package com.esde.companion.domain.model

/**
 * A resolved RetroAchievements game's achievement summary for the signed-in user.
 * [totalPlayers] is the game's total distinct-player count (not per-user) - RA has no
 * per-achievement unlock-rate field, so this is threaded down to each achievement row to
 * compute [AchievementDisplayField.UnlockRate] client-side.
 */
data class GameAchievementSummary(
    val gameId: Long,
    val gameTitle: String,
    val totalPoints: Int,
    val earnedPoints: Int,
    val completionPercent: Float,
    val achievements: List<AchievementItem>,
    val totalPlayers: Int = 0,
    val playtimeStats: GamePlaytimeStats? = null,
)

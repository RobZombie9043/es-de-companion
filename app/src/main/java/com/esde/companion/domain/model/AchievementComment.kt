package com.esde.companion.domain.model

/**
 * One RetroAchievements achievement-wall comment. [submittedAtMillis] is `null` if the API's
 * timestamp couldn't be parsed.
 */
data class AchievementComment(
    val user: String,
    val submittedAtMillis: Long?,
    val text: String,
)

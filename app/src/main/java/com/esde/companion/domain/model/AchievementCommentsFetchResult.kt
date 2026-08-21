package com.esde.companion.domain.model

/**
 * Outcome of fetching an achievement's wall comments from RetroAchievements. A network/API
 * failure here is a routine, expected outcome, represented as a value rather than a thrown
 * exception, per CLAUDE.md - same pattern as [AchievementSummaryFetchResult]. There is no
 * "not found" variant - an achievement with no comments is just `Success(emptyList())`.
 */
sealed class AchievementCommentsFetchResult {
    data class Success(val comments: List<AchievementComment>) : AchievementCommentsFetchResult()

    data class NetworkError(val message: String) : AchievementCommentsFetchResult()
}

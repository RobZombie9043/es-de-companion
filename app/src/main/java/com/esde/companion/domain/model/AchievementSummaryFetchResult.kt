package com.esde.companion.domain.model

/**
 * Outcome of fetching a resolved game's achievement summary from RetroAchievements. A
 * network/API failure here is a routine, expected outcome (offline, rate-limited, RA
 * outage) - represented as a value rather than a thrown exception, per CLAUDE.md, the same
 * pattern [ReleaseFetchResult] uses for the update checker.
 */
sealed class AchievementSummaryFetchResult {
    data class Success(val summary: GameAchievementSummary) : AchievementSummaryFetchResult()

    /** The gameId does not exist on RetroAchievements (e.g. a stale manual override). */
    data object NotFound : AchievementSummaryFetchResult()

    data class NetworkError(val message: String) : AchievementSummaryFetchResult()
}

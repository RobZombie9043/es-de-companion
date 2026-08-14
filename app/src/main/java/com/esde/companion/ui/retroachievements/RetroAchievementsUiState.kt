package com.esde.companion.ui.retroachievements

import com.esde.companion.domain.model.AchievementSummaryFetchResult
import com.esde.companion.domain.model.GameAchievementSummary
import com.esde.companion.domain.model.MatchMethod

/**
 * Whether/how the current game was identified on RetroAchievements - kept separate from
 * [RetroAchievementsFetchState] so a network failure fetching achievements never gets
 * confused with "wrong/no game identified" (see ResolveRetroAchievementsGameUseCase's kdoc).
 * The RetroAchievements FAB (MainActivity's fabSlotContent) is only shown once this is
 * neither [NotSignedIn] nor [NoGame] - both of those mean there is nothing this feature
 * could show right now.
 */
sealed class RetroAchievementsResolutionState {
    data object NotSignedIn : RetroAchievementsResolutionState()

    data object NoGame : RetroAchievementsResolutionState()

    data object UnsupportedSystem : RetroAchievementsResolutionState()

    data object NoMatch : RetroAchievementsResolutionState()

    data class Found(val method: MatchMethod) : RetroAchievementsResolutionState()
}

/**
 * Outcome of fetching the resolved game's achievement summary - only meaningful once
 * [RetroAchievementsResolutionState.Found].
 */
sealed class RetroAchievementsFetchState {
    data object Idle : RetroAchievementsFetchState()

    data object Loading : RetroAchievementsFetchState()

    data class Loaded(val summary: GameAchievementSummary) : RetroAchievementsFetchState()

    data object NotFound : RetroAchievementsFetchState()

    data class NetworkError(val message: String) : RetroAchievementsFetchState()
}

/** Shared by [RetroAchievementsViewModel] and [RetroAchievementsSystemGamesViewModel] - both
 * fetch a [GameAchievementSummary] for an already-identified gameId and map the outcome into
 * [RetroAchievementsFetchState] identically. */
internal fun AchievementSummaryFetchResult.toFetchState(): RetroAchievementsFetchState =
    when (this) {
        is AchievementSummaryFetchResult.Success -> RetroAchievementsFetchState.Loaded(summary)
        AchievementSummaryFetchResult.NotFound -> RetroAchievementsFetchState.NotFound
        is AchievementSummaryFetchResult.NetworkError -> RetroAchievementsFetchState.NetworkError(message)
    }

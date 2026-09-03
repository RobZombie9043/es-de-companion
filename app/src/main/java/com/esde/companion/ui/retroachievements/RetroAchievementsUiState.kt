package com.esde.companion.ui.retroachievements

import com.esde.companion.domain.model.AchievementComment
import com.esde.companion.domain.model.AchievementCommentsFetchResult
import com.esde.companion.domain.model.AchievementSummaryFetchResult
import com.esde.companion.domain.model.GameAchievementSummary
import com.esde.companion.domain.model.GameHashSupport
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

    /**
     * [isRefreshing] is true while [summary] is a stale cached value being shown immediately
     * (stale-while-revalidate) with a real fetch running in the background to replace it -
     * deliberately a flag on this same variant rather than a separate sealed state, so
     * `RetroAchievementsFetchBody`'s `AnimatedContent` (keyed on `it::class`) never retriggers
     * its crossfade going from a stale to a fresh [Loaded] value, only on a genuine transition
     * to/from a different variant.
     */
    data class Loaded(val summary: GameAchievementSummary, val isRefreshing: Boolean = false) :
        RetroAchievementsFetchState()

    data object NotFound : RetroAchievementsFetchState()

    data class NetworkError(val message: String) : RetroAchievementsFetchState()
}

/**
 * Backs the achievements screen's "Supported Hashes" kebab-menu dialog ([GetGameHashSupportUseCase]).
 * A tiny sealed state rather than a plain nullable, so the dialog can show a loading state
 * while [com.esde.companion.domain.usecase.GetGameHashSupportUseCase] fetches the console's
 * candidate list (which may not be warm in cache yet) instead of appearing to hang.
 */
sealed class HashSupportState {
    data object Hidden : HashSupportState()

    data object Loading : HashSupportState()

    data class Loaded(val support: GameHashSupport) : HashSupportState()
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

/**
 * Backs the tap-to-expand comments section under an expanded [com.esde.companion.domain.model.AchievementItem]
 * row - only meaningful while paired with an achievementId inside [ExpandedAchievementComments].
 */
sealed class CommentsFetchState {
    data object Loading : CommentsFetchState()

    data class Loaded(val comments: List<AchievementComment>) : CommentsFetchState()

    data class NetworkError(val message: String) : CommentsFetchState()
}

/**
 * Atomically pairs which achievement's comments section is expanded with that achievement's
 * fetch state, in a single [kotlinx.coroutines.flow.StateFlow] value - both ViewModels
 * originally exposed the achievementId and the fetch state as two separate `StateFlow`s, which
 * update at different times (the id flips synchronously on tap, the fetch state resolves
 * asynchronously one dispatch later). Compose could observe a torn combination of the two - the
 * newly-expanding row's id, but the previously-expanded achievement's (possibly long) comments
 * content - which visibly grew that row to fit the wrong content before shrinking back down
 * once the real state arrived. Bundling both into one value written in a single atomic
 * `StateFlow` update makes that torn read impossible: a row can never see an achievementId that
 * doesn't match the comments it's paired with.
 */
data class ExpandedAchievementComments(
    val achievementId: Long,
    val comments: CommentsFetchState,
)

/** Shared by both ViewModels' comments-fetch logic - see [CommentsFetchState]'s kdoc. */
internal fun AchievementCommentsFetchResult.toCommentsFetchState(): CommentsFetchState =
    when (this) {
        is AchievementCommentsFetchResult.Success -> CommentsFetchState.Loaded(comments)
        is AchievementCommentsFetchResult.NetworkError -> CommentsFetchState.NetworkError(message)
    }

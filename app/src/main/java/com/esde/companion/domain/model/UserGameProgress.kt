package com.esde.companion.domain.model

/**
 * The signed-in user's completion state for one game, from RA's per-user
 * `getUserCompletionProgress` endpoint (see `GetUserGameProgressUseCase`) - a single global,
 * cross-console dataset, not scoped to the system currently being browsed. [maxPossible] is the
 * game's total achievement count as RA reports it for this endpoint, which can differ slightly
 * from a `RetroAchievementsCandidateGame.numAchievements` fetched separately from `GetGameList`.
 */
data class UserGameProgress(
    val gameId: Long,
    val numAwarded: Int,
    val maxPossible: Int,
    val status: ProgressStatus,
)

/**
 * A game with no entry in `getUserCompletionProgress` at all (the user never touched it) is
 * [None] by absence, not by an explicit field - the endpoint simply omits untouched games.
 */
enum class ProgressStatus {
    None,
    Some,
    Beaten,
    Mastered,
}

/**
 * RA's `HighestAwardKind` is a free-form string (`"mastered"`, `"beaten-hardcore"`,
 * `"beaten-softcore"`, `"completed"`, or absent) - [highestAwardKind] must already be laundered
 * through an explicit nullable local by the caller (see `RetroClientRetroAchievementsApi`'s
 * mapper) rather than read directly, since Gson can populate the API client's `@NotNull`-typed
 * field with `null` via reflection, bypassing Kotlin's constructor null-checks entirely.
 *
 * Per confirmed product decision: "Mastered" requires the **hardcore** award - a softcore 100%
 * completion (`"completed"`) maps to [ProgressStatus.Beaten], not [ProgressStatus.Mastered].
 * An unrecognized/future award-kind string degrades to [ProgressStatus.Some]/[ProgressStatus.None]
 * rather than upward to [ProgressStatus.Beaten]/[ProgressStatus.Mastered] - a filter should never
 * over-claim a game is more complete than confirmed.
 */
internal fun progressStatusFor(
    highestAwardKind: String?,
    numAwarded: Int,
): ProgressStatus {
    val kind = highestAwardKind?.lowercase()
    return when {
        kind.isNullOrBlank() -> if (numAwarded > 0) ProgressStatus.Some else ProgressStatus.None
        kind.contains("master") -> ProgressStatus.Mastered
        kind.contains("beaten") -> ProgressStatus.Beaten
        kind.contains("complet") -> ProgressStatus.Beaten
        else -> if (numAwarded > 0) ProgressStatus.Some else ProgressStatus.None
    }
}

package com.esde.companion.domain.model

/**
 * The outcome of resolving an ES-DE game to a RetroAchievements game, produced by
 * `ResolveRetroAchievementsGameUseCase`. No fuzzy/edit-distance scoring is involved (see
 * CLAUDE.md's RetroAchievements section) - a title that isn't an exact or normalized match
 * is [NoMatch], resolved via a manual override rather than a guess.
 */
sealed class RetroAchievementsGameMatch {
    data class Found(val gameId: Long, val method: MatchMethod) : RetroAchievementsGameMatch()

    data object NoMatch : RetroAchievementsGameMatch()

    /** The ROM's system has no entry in [EsdeSystemToRaConsoleMapping]. */
    data object UnsupportedSystem : RetroAchievementsGameMatch()
}

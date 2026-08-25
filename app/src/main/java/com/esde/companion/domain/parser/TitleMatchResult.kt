package com.esde.companion.domain.parser

import com.esde.companion.domain.model.RetroAchievementsCandidateGame

/**
 * The result of [GameTitleMatcher.match]. Deliberately narrower than
 * [com.esde.companion.domain.model.RetroAchievementsGameMatch] - it has no case
 * corresponding to a manual override, so the compiler enforces that automatic matching can
 * never produce one.
 */
sealed class TitleMatchResult {
    data class Matched(val candidate: RetroAchievementsCandidateGame, val method: TitleMatchMethod) : TitleMatchResult()

    data object NoMatch : TitleMatchResult()
}

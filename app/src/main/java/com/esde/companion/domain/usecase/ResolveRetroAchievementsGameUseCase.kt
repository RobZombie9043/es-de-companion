package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.EsdeSystemToRaConsoleMapping
import com.esde.companion.domain.model.GameReference
import com.esde.companion.domain.model.MatchMethod
import com.esde.companion.domain.model.RetroAchievementsGameMatch
import com.esde.companion.domain.parser.GameTitleMatcher
import com.esde.companion.domain.parser.TitleMatchMethod
import com.esde.companion.domain.parser.TitleMatchResult
import com.esde.companion.domain.repository.GameMatchOverrideRepository
import com.esde.companion.domain.repository.RetroAchievementsRepository

/**
 * Identification only - no achievement fetch, see [GetGameAchievementSummaryUseCase] for
 * that, deliberately kept separate so a network failure fetching achievements never gets
 * confused with "wrong/no game identified". A stored override always wins over an automatic
 * title match when both are available.
 *
 * [gameName] is passed alongside [gameReference] rather than folded into it, since
 * [GameReference] only carries the identity ES-DE's own media lookup needs (system + rom
 * path) and has no display-title field for [GameTitleMatcher] to compare against.
 */
class ResolveRetroAchievementsGameUseCase(
    private val gameMatchOverrideRepository: GameMatchOverrideRepository,
    private val retroAchievementsRepository: RetroAchievementsRepository,
) {
    suspend operator fun invoke(
        gameReference: GameReference,
        gameName: String,
    ): RetroAchievementsGameMatch {
        val console =
            EsdeSystemToRaConsoleMapping.consoleFor(gameReference.systemShortName)
                ?: return RetroAchievementsGameMatch.UnsupportedSystem

        val override = gameMatchOverrideRepository.getOverride(gameReference)
        return if (override != null) {
            RetroAchievementsGameMatch.Found(override.raGameId, MatchMethod.ManualOverride)
        } else {
            val candidates = retroAchievementsRepository.getCandidateGames(console)
            when (val titleMatch = GameTitleMatcher.match(gameName, candidates)) {
                is TitleMatchResult.Matched ->
                    RetroAchievementsGameMatch.Found(titleMatch.candidate.gameId, titleMatch.method.toMatchMethod())
                TitleMatchResult.NoMatch -> RetroAchievementsGameMatch.NoMatch
            }
        }
    }

    private fun TitleMatchMethod.toMatchMethod(): MatchMethod =
        when (this) {
            TitleMatchMethod.ExactTitle -> MatchMethod.ExactTitle
            TitleMatchMethod.NormalizedTitle -> MatchMethod.NormalizedTitle
        }
}

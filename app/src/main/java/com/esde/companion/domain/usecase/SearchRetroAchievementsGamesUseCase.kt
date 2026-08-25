package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.EsdeSystemToRaConsoleMapping
import com.esde.companion.domain.model.RetroAchievementsCandidateGame
import com.esde.companion.domain.parser.GameTitleMatcher
import com.esde.companion.domain.repository.RetroAchievementsRepository

/**
 * Backs the manual "search and pick" correction screen. An unmapped [systemShortName]
 * returns an empty list rather than throwing, since the search screen should never be
 * reachable for a system [ResolveRetroAchievementsGameUseCase] already reported as
 * [com.esde.companion.domain.model.RetroAchievementsGameMatch.UnsupportedSystem].
 *
 * With a blank [query] - the initial state, before the user has typed anything - results
 * are [GameTitleMatcher.rankBySimilarity]-ranked against [gameName] rather than returned in
 * raw cache order, so the correction picker can surface the likely intended game at the top
 * without the user needing to type at all. Once [query] is non-blank, a typed search always
 * means "show me titles containing this text", so that overrides ranking with a plain
 * substring filter instead.
 */
class SearchRetroAchievementsGamesUseCase(
    private val retroAchievementsRepository: RetroAchievementsRepository,
) {
    suspend operator fun invoke(
        systemShortName: String,
        query: String,
        gameName: String,
    ): List<RetroAchievementsCandidateGame> {
        val console = EsdeSystemToRaConsoleMapping.consoleFor(systemShortName) ?: return emptyList()
        val candidates = retroAchievementsRepository.getCandidateGames(console)
        return if (query.isBlank()) {
            GameTitleMatcher.rankBySimilarity(gameName, candidates)
        } else {
            candidates.filter { it.title.contains(query, ignoreCase = true) }
        }
    }
}

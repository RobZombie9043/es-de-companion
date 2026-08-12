package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.EsdeSystemToRaConsoleMapping
import com.esde.companion.domain.model.RetroAchievementsCandidateGame
import com.esde.companion.domain.repository.RetroAchievementsRepository

/**
 * Backs the manual "search and pick" correction screen - filters the same cached
 * per-console game list [ResolveRetroAchievementsGameUseCase] draws its automatic guess
 * from, by a case-insensitive substring match against the typed query. An unmapped
 * [systemShortName] returns an empty list rather than throwing, since the search screen
 * should never be reachable for a system [ResolveRetroAchievementsGameUseCase] already
 * reported as [com.esde.companion.domain.model.RetroAchievementsGameMatch.UnsupportedSystem].
 */
class SearchRetroAchievementsGamesUseCase(
    private val retroAchievementsRepository: RetroAchievementsRepository,
) {
    suspend operator fun invoke(
        systemShortName: String,
        query: String,
    ): List<RetroAchievementsCandidateGame> {
        val console = EsdeSystemToRaConsoleMapping.consoleFor(systemShortName) ?: return emptyList()
        val candidates = retroAchievementsRepository.getCandidateGames(console)
        return if (query.isBlank()) {
            candidates
        } else {
            candidates.filter { it.title.contains(query, ignoreCase = true) }
        }
    }
}

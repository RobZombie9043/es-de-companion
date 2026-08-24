package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.EsdeSystemToRaConsoleMapping
import com.esde.companion.domain.model.RetroAchievementsCandidateGame
import com.esde.companion.domain.repository.RetroAchievementsRepository

/**
 * Backs the system-wide games browser (`RetroAchievementsSystemGamesScreen`) - the same
 * cached per-console game list [com.esde.companion.domain.parser.GameTitleMatcher] and the
 * manual correction picker draw from, alphabetically ordered for browsing rather than
 * ranked/filtered against any particular game. Returns `null` (not an empty list) for a
 * system with no [EsdeSystemToRaConsoleMapping] entry, so callers can distinguish
 * "unsupported system" from "supported, but RA genuinely has zero games logged for it".
 */
class GetRetroAchievementsSystemGamesUseCase(
    private val retroAchievementsRepository: RetroAchievementsRepository,
) {
    suspend operator fun invoke(systemShortName: String): List<RetroAchievementsCandidateGame>? {
        val console = EsdeSystemToRaConsoleMapping.consoleFor(systemShortName) ?: return null
        return retroAchievementsRepository.getCandidateGames(console).sortedBy { it.title.lowercase() }
    }
}

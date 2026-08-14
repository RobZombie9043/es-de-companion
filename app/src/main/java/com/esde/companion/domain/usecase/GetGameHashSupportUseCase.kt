package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.EsdeSystemToRaConsoleMapping
import com.esde.companion.domain.model.GameHashSupport
import com.esde.companion.domain.model.GameReference
import com.esde.companion.domain.repository.GameRomHashRepository
import com.esde.companion.domain.repository.RetroAchievementsRepository

/**
 * Backs the achievements screen's "Supported Hashes" kebab-menu entry - identification is
 * already done by the time this runs (see `ResolveRetroAchievementsGameUseCase`), so this
 * only re-derives the two pieces that dialog needs to display: the current ROM's own hash,
 * and the already-resolved [gameId]'s full set of RA-known hashes. An unmapped system - not
 * expected to happen in practice, since this is only offered once a game has actually
 * resolved to a [gameId] - returns no supported hashes rather than throwing, the same
 * empty-list-on-unmapped convention [SearchRetroAchievementsGamesUseCase] uses.
 */
class GetGameHashSupportUseCase(
    private val gameRomHashRepository: GameRomHashRepository,
    private val retroAchievementsRepository: RetroAchievementsRepository,
) {
    suspend operator fun invoke(
        gameReference: GameReference,
        gameId: Long,
    ): GameHashSupport {
        val currentHash =
            gameRomHashRepository.resolveRomHash(gameReference.systemShortName, gameReference.romPath).value
        val console = EsdeSystemToRaConsoleMapping.consoleFor(gameReference.systemShortName)
        val supportedHashes =
            console
                ?.let { retroAchievementsRepository.getCandidateGames(it) }
                ?.firstOrNull { candidate -> candidate.gameId == gameId }
                ?.hashes
                ?: emptyList()
        return GameHashSupport(currentHash, supportedHashes)
    }
}

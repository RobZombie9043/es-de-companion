package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.GameMedia
import com.esde.companion.domain.repository.GameMediaRepository

/**
 * The single entry point the UI layer should depend on to look up a game's media - kept
 * as its own use case, downstream of AppState and entirely separate from it (see
 * CLAUDE.md: media resolution "is a distinct concern from state parsing").
 *
 * Callers pass whatever game is currently relevant (from AppState.BrowsingGame,
 * .PlayingGame, or a Screensaver's currentGame) - this use case has no opinion about
 * which state the app is in.
 */
class ResolveGameMediaUseCase(
    private val gameMediaRepository: GameMediaRepository,
) {
    suspend operator fun invoke(systemShortName: String, romPath: String): GameMedia =
        gameMediaRepository.resolveMedia(systemShortName, romPath)
}
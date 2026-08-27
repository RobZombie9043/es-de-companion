package com.esde.companion.domain.usecase

import com.esde.companion.domain.parser.GamelistGameEntry
import com.esde.companion.domain.repository.GamelistLibraryRepository

class ListGamelistGamesUseCase(
    private val gamelistLibraryRepository: GamelistLibraryRepository,
) {
    suspend operator fun invoke(systemShortName: String): List<GamelistGameEntry> {
        return gamelistLibraryRepository.listGames(systemShortName)
    }
}

package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.GamelistSystemSummary
import com.esde.companion.domain.repository.GamelistLibraryRepository

class ListGamelistSystemsUseCase(
    private val gamelistLibraryRepository: GamelistLibraryRepository,
) {
    suspend operator fun invoke(): List<GamelistSystemSummary> = gamelistLibraryRepository.listSystems()
}

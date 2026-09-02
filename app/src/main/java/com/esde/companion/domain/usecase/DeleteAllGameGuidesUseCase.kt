package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.GameGuideLibraryRepository

class DeleteAllGameGuidesUseCase(
    private val gameGuideLibraryRepository: GameGuideLibraryRepository,
) {
    suspend operator fun invoke() = gameGuideLibraryRepository.deleteAllGuides()
}

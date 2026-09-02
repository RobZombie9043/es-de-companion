package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.GameGuideLibraryRepository

class DeleteGameGuideUseCase(
    private val gameGuideLibraryRepository: GameGuideLibraryRepository,
) {
    suspend operator fun invoke(guideId: String) = gameGuideLibraryRepository.deleteGuide(guideId)
}

package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.GameGuideLibraryRepository

class LoadGameGuideContentUseCase(
    private val gameGuideLibraryRepository: GameGuideLibraryRepository,
) {
    suspend operator fun invoke(guideId: String): List<String>? = gameGuideLibraryRepository.loadContent(guideId)
}

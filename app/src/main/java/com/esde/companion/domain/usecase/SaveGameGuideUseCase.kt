package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.DownloadedGameGuide
import com.esde.companion.domain.repository.GameGuideLibraryRepository

class SaveGameGuideUseCase(
    private val gameGuideLibraryRepository: GameGuideLibraryRepository,
) {
    suspend operator fun invoke(
        guide: DownloadedGameGuide,
        content: List<String>,
    ): Result<Unit> = gameGuideLibraryRepository.saveGuide(guide, content)
}

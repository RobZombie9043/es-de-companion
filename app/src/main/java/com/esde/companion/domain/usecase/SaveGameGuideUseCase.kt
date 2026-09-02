package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.DownloadedGameGuide
import com.esde.companion.domain.repository.GameGuideLibraryRepository
import com.esde.companion.domain.repository.GuidePageContent

class SaveGameGuideUseCase(
    private val gameGuideLibraryRepository: GameGuideLibraryRepository,
) {
    suspend operator fun invoke(
        guide: DownloadedGameGuide,
        pageContent: suspend (pageIndex: Int) -> GuidePageContent,
    ): Result<Unit> = gameGuideLibraryRepository.saveGuide(guide, pageContent)
}

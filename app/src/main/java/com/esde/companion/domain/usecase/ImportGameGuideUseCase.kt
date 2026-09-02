package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.DownloadedGameGuide
import com.esde.companion.domain.repository.GameGuideLibraryRepository

class ImportGameGuideUseCase(
    private val gameGuideLibraryRepository: GameGuideLibraryRepository,
) {
    suspend operator fun invoke(
        guide: DownloadedGameGuide,
        contentBytes: ByteArray,
        fileExtension: String,
    ): Result<Unit> = gameGuideLibraryRepository.saveImportedGuide(guide, contentBytes, fileExtension)
}

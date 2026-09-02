package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.GameGuideLibraryRepository

class ResolveGameGuideMediaDirectoryUseCase(
    private val gameGuideLibraryRepository: GameGuideLibraryRepository,
) {
    suspend operator fun invoke(guideId: String): String = gameGuideLibraryRepository.mediaDirectoryPath(guideId)
}

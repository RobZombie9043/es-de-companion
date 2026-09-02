package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.GameGuideReadingProgress
import com.esde.companion.domain.repository.GameGuideSettingsRepository

class SetGameGuideReadingProgressUseCase(
    private val gameGuideSettingsRepository: GameGuideSettingsRepository,
) {
    suspend operator fun invoke(progress: GameGuideReadingProgress) {
        gameGuideSettingsRepository.setReadingProgress(progress)
    }
}

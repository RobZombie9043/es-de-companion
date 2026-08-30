package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.GameGuideReadingProgress
import com.esde.companion.domain.repository.GameGuideSettingsRepository
import kotlinx.coroutines.flow.Flow

class ObserveGameGuideReadingProgressUseCase(
    private val gameGuideSettingsRepository: GameGuideSettingsRepository,
) {
    operator fun invoke(guideId: String): Flow<GameGuideReadingProgress?> {
        return gameGuideSettingsRepository.observeReadingProgress(guideId)
    }
}

package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.GameGuideSettingsRepository
import kotlinx.coroutines.flow.Flow

class ObserveManualFallbackOnNoGuideEnabledUseCase(
    private val gameGuideSettingsRepository: GameGuideSettingsRepository,
) {
    operator fun invoke(): Flow<Boolean> = gameGuideSettingsRepository.observeManualFallbackOnNoGuideEnabled()
}

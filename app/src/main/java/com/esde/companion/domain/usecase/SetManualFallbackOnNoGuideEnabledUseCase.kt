package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.GameGuideSettingsRepository

class SetManualFallbackOnNoGuideEnabledUseCase(
    private val gameGuideSettingsRepository: GameGuideSettingsRepository,
) {
    suspend operator fun invoke(enabled: Boolean) {
        gameGuideSettingsRepository.setManualFallbackOnNoGuideEnabled(enabled)
    }
}

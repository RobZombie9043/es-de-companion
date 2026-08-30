package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.GameGuideDisplayPreferences
import com.esde.companion.domain.repository.GameGuideSettingsRepository

class SetGameGuideDisplayPreferencesUseCase(
    private val gameGuideSettingsRepository: GameGuideSettingsRepository,
) {
    suspend operator fun invoke(preferences: GameGuideDisplayPreferences) {
        gameGuideSettingsRepository.setDisplayPreferences(preferences)
    }
}

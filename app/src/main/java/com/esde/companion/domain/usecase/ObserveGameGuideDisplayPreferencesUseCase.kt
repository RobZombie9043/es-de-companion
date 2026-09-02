package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.GameGuideDisplayPreferences
import com.esde.companion.domain.repository.GameGuideSettingsRepository
import kotlinx.coroutines.flow.Flow

class ObserveGameGuideDisplayPreferencesUseCase(
    private val gameGuideSettingsRepository: GameGuideSettingsRepository,
) {
    operator fun invoke(): Flow<GameGuideDisplayPreferences> = gameGuideSettingsRepository.observeDisplayPreferences()
}

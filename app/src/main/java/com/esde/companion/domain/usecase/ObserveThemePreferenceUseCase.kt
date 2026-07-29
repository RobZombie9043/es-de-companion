package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.ThemePreference
import com.esde.companion.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.Flow

class ObserveThemePreferenceUseCase(
    private val onboardingRepository: OnboardingRepository,
) {
    operator fun invoke(): Flow<ThemePreference> = onboardingRepository.observeThemePreference()
}
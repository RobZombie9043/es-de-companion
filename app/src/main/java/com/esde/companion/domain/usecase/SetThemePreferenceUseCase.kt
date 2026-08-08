package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.ThemePreference
import com.esde.companion.domain.repository.OnboardingRepository

class SetThemePreferenceUseCase(
    private val onboardingRepository: OnboardingRepository,
) {
    suspend operator fun invoke(preference: ThemePreference) = onboardingRepository.setThemePreference(preference)
}

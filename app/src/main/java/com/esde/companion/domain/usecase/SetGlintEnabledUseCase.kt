package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.OnboardingRepository

class SetGlintEnabledUseCase(
    private val onboardingRepository: OnboardingRepository,
) {
    suspend operator fun invoke(enabled: Boolean) =
        onboardingRepository.setGlintEnabled(enabled)
}

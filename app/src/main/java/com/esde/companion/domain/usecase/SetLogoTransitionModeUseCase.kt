package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.LogoTransitionMode
import com.esde.companion.domain.repository.OnboardingRepository

class SetLogoTransitionModeUseCase(
    private val onboardingRepository: OnboardingRepository,
) {
    suspend operator fun invoke(mode: LogoTransitionMode) =
        onboardingRepository.setLogoTransitionMode(mode)
}

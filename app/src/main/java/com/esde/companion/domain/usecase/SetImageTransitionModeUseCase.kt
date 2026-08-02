package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.ImageTransitionMode
import com.esde.companion.domain.repository.OnboardingRepository

class SetImageTransitionModeUseCase(
    private val onboardingRepository: OnboardingRepository,
) {
    suspend operator fun invoke(mode: ImageTransitionMode) =
        onboardingRepository.setImageTransitionMode(mode)
}

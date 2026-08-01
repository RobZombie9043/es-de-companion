package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.ScreenBehavior
import com.esde.companion.domain.repository.OnboardingRepository

class SetScreensaverBehaviorUseCase(
    private val onboardingRepository: OnboardingRepository,
) {
    suspend operator fun invoke(behavior: ScreenBehavior) =
        onboardingRepository.setScreensaverBehavior(behavior)
}
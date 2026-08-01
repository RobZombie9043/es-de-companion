package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.OnboardingRepository

class SetVideoDelaySecondsUseCase(
    private val onboardingRepository: OnboardingRepository,
) {
    suspend operator fun invoke(seconds: Int) = onboardingRepository.setVideoDelaySeconds(seconds)
}
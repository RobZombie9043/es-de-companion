package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.OnboardingRepository

class SetVideoPlaybackEnabledUseCase(
    private val onboardingRepository: OnboardingRepository,
) {
    suspend operator fun invoke(enabled: Boolean) = onboardingRepository.setVideoPlaybackEnabled(enabled)
}
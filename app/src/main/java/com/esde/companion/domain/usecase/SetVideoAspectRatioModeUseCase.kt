package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.VideoAspectRatioMode
import com.esde.companion.domain.repository.OnboardingRepository

class SetVideoAspectRatioModeUseCase(
    private val onboardingRepository: OnboardingRepository,
) {
    suspend operator fun invoke(mode: VideoAspectRatioMode) = onboardingRepository.setVideoAspectRatioMode(mode)
}

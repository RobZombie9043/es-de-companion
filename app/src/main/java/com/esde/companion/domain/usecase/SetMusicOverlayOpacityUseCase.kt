package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.OnboardingRepository

class SetMusicOverlayOpacityUseCase(
    private val onboardingRepository: OnboardingRepository,
) {
    suspend operator fun invoke(percent: Int) = onboardingRepository.setMusicOverlayOpacityPercent(percent)
}

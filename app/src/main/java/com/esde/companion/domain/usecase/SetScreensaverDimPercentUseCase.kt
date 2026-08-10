package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.OnboardingRepository

class SetScreensaverDimPercentUseCase(
    private val onboardingRepository: OnboardingRepository,
) {
    suspend operator fun invoke(percent: Int) = onboardingRepository.setScreensaverDimPercent(percent)
}

package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.OnboardingRepository

class SetMusicPlayWhileBrowsingSystemsUseCase(
    private val onboardingRepository: OnboardingRepository,
) {
    suspend operator fun invoke(enabled: Boolean) = onboardingRepository.setMusicPlayWhileBrowsingSystems(enabled)
}

package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.OnboardingRepository

class SetSettingsFabVisibleUseCase(
    private val onboardingRepository: OnboardingRepository,
) {
    suspend operator fun invoke(visible: Boolean) = onboardingRepository.setSettingsFabVisible(visible)
}

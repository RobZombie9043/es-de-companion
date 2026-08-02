package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.LogoTransitionMode
import com.esde.companion.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.Flow

class ObserveLogoTransitionModeUseCase(
    private val onboardingRepository: OnboardingRepository,
) {
    operator fun invoke(): Flow<LogoTransitionMode> = onboardingRepository.observeLogoTransitionMode()
}

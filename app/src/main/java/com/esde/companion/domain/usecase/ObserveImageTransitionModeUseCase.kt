package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.ImageTransitionMode
import com.esde.companion.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.Flow

class ObserveImageTransitionModeUseCase(
    private val onboardingRepository: OnboardingRepository,
) {
    operator fun invoke(): Flow<ImageTransitionMode> = onboardingRepository.observeImageTransitionMode()
}

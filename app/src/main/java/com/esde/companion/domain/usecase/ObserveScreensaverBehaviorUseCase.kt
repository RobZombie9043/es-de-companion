package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.ScreenBehavior
import com.esde.companion.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.Flow

class ObserveScreensaverBehaviorUseCase(
    private val onboardingRepository: OnboardingRepository,
) {
    operator fun invoke(): Flow<ScreenBehavior> = onboardingRepository.observeScreensaverBehavior()
}
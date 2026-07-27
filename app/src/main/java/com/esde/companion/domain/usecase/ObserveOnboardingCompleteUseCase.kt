package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.Flow

class ObserveOnboardingCompleteUseCase(
    private val onboardingRepository: OnboardingRepository,
) {
    operator fun invoke(): Flow<Boolean> = onboardingRepository.observeOnboardingComplete()
}
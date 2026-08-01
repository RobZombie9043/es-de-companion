package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.VideoAspectRatioMode
import com.esde.companion.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.Flow

class ObserveVideoAspectRatioModeUseCase(
    private val onboardingRepository: OnboardingRepository,
) {
    operator fun invoke(): Flow<VideoAspectRatioMode> = onboardingRepository.observeVideoAspectRatioMode()
}

package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.MusicDuckingMode
import com.esde.companion.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.Flow

class ObserveMusicDuckingModeUseCase(
    private val onboardingRepository: OnboardingRepository,
) {
    operator fun invoke(): Flow<MusicDuckingMode> = onboardingRepository.observeMusicDuckingMode()
}

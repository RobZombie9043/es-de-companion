package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.MusicDuckingMode
import com.esde.companion.domain.repository.OnboardingRepository

class SetMusicDuckingModeUseCase(
    private val onboardingRepository: OnboardingRepository,
) {
    suspend operator fun invoke(mode: MusicDuckingMode) = onboardingRepository.setMusicDuckingMode(mode)
}

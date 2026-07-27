package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.MediaFolderValidation
import com.esde.companion.domain.repository.OnboardingRepository

class ValidateEsdeMediaFolderUseCase(
    private val onboardingRepository: OnboardingRepository,
) {
    suspend operator fun invoke(path: String): MediaFolderValidation =
        onboardingRepository.validateMediaFolder(path)
}
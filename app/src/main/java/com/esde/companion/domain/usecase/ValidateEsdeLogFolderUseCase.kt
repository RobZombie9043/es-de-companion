package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.LogFolderValidation
import com.esde.companion.domain.repository.OnboardingRepository

class ValidateEsdeLogFolderUseCase(
    private val onboardingRepository: OnboardingRepository,
) {
    suspend operator fun invoke(path: String): LogFolderValidation =
        onboardingRepository.validateLogFolder(path)
}
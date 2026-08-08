package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.OnboardingRepository

/**
 * Persists both chosen folders and marks onboarding complete, in that order - if saving
 * either path fails, onboarding should not be marked complete. Also the method a Settings
 * screen uses when the user changes a folder later, minus the "mark complete" step for
 * a single-folder update (see [UpdateLogFolderUseCase]/[UpdateMediaFolderUseCase] if only
 * one folder is being changed rather than run during initial setup).
 */
class CompleteOnboardingUseCase(
    private val onboardingRepository: OnboardingRepository,
) {
    suspend operator fun invoke(
        logFolderPath: String,
        mediaFolderPath: String,
    ) {
        onboardingRepository.saveLogFolderPath(logFolderPath)
        onboardingRepository.saveMediaFolderPath(mediaFolderPath)
        onboardingRepository.markOnboardingComplete()
    }
}

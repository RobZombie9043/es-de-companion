package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.OnboardingRepository

class SetBluetoothPermissionRequestedUseCase(
    private val onboardingRepository: OnboardingRepository,
) {
    suspend operator fun invoke(requested: Boolean) {
        onboardingRepository.setBluetoothPermissionRequested(requested)
    }
}

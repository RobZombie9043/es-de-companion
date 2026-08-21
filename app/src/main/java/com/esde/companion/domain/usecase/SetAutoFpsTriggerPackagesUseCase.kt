package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.ThorSettingsRepository

class SetAutoFpsTriggerPackagesUseCase(
    private val thorSettingsRepository: ThorSettingsRepository,
) {
    suspend operator fun invoke(packages: Set<String>) = thorSettingsRepository.setAutoFpsTriggerPackages(packages)
}

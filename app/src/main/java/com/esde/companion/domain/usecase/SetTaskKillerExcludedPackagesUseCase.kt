package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.ThorSettingsRepository

class SetTaskKillerExcludedPackagesUseCase(
    private val thorSettingsRepository: ThorSettingsRepository,
) {
    suspend operator fun invoke(packages: Set<String>) = thorSettingsRepository.setTaskKillerExcludedPackages(packages)
}

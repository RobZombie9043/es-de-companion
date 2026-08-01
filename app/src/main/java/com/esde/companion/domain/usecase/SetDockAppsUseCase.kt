package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.DockSettingsRepository

class SetDockAppsUseCase(
    private val dockSettingsRepository: DockSettingsRepository,
) {
    suspend operator fun invoke(packageNames: List<String>) = dockSettingsRepository.setDockApps(packageNames)
}

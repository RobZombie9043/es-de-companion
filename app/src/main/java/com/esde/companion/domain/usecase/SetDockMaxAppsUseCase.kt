package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.DockSettingsRepository

class SetDockMaxAppsUseCase(
    private val dockSettingsRepository: DockSettingsRepository,
) {
    suspend operator fun invoke(maxApps: Int) = dockSettingsRepository.setDockMaxApps(maxApps)
}

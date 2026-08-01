package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.DockSettingsRepository

class SetDockEnabledUseCase(
    private val dockSettingsRepository: DockSettingsRepository,
) {
    suspend operator fun invoke(enabled: Boolean) = dockSettingsRepository.setDockEnabled(enabled)
}

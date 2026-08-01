package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.DockSettingsRepository

class SetDockOpacityUseCase(
    private val dockSettingsRepository: DockSettingsRepository,
) {
    suspend operator fun invoke(percent: Int) = dockSettingsRepository.setDockOpacityPercent(percent)
}

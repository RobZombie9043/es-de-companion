package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.DockSize
import com.esde.companion.domain.repository.DockSettingsRepository

class SetDockSizeUseCase(
    private val dockSettingsRepository: DockSettingsRepository,
) {
    suspend operator fun invoke(size: DockSize) = dockSettingsRepository.setDockSize(size)
}

package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.DockSize
import com.esde.companion.domain.repository.DockSettingsRepository
import kotlinx.coroutines.flow.Flow

class ObserveDockSizeUseCase(
    private val dockSettingsRepository: DockSettingsRepository,
) {
    operator fun invoke(): Flow<DockSize> = dockSettingsRepository.observeDockSize()
}

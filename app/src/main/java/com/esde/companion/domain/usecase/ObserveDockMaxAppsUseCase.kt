package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.DockSettingsRepository
import kotlinx.coroutines.flow.Flow

class ObserveDockMaxAppsUseCase(
    private val dockSettingsRepository: DockSettingsRepository,
) {
    operator fun invoke(): Flow<Int> = dockSettingsRepository.observeDockMaxApps()
}

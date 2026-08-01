package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.DockSettingsRepository
import kotlinx.coroutines.flow.Flow

class ObserveDockAppsUseCase(
    private val dockSettingsRepository: DockSettingsRepository,
) {
    operator fun invoke(): Flow<List<String>> = dockSettingsRepository.observeDockApps()
}

package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.InstalledApp
import com.esde.companion.domain.repository.InstalledAppsRepository
import kotlinx.coroutines.flow.Flow

class ObserveInstalledAppsUseCase(
    private val installedAppsRepository: InstalledAppsRepository,
) {
    operator fun invoke(): Flow<List<InstalledApp>> = installedAppsRepository.observeInstalledApps()
}
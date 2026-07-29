package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.AppDrawerSettingsRepository

class SetHiddenAppsUseCase(
    private val appDrawerSettingsRepository: AppDrawerSettingsRepository,
) {
    suspend operator fun invoke(packageNames: Set<String>) =
        appDrawerSettingsRepository.setHiddenApps(packageNames)
}
package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.AppDrawerSettingsRepository

class SetOtherScreenLaunchAppsUseCase(
    private val appDrawerSettingsRepository: AppDrawerSettingsRepository,
) {
    suspend operator fun invoke(packageNames: Set<String>) = appDrawerSettingsRepository.setOtherScreenLaunchApps(packageNames)
}

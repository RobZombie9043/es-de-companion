package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.AppDrawerSettingsRepository

class SetDrawerOpacityUseCase(
    private val appDrawerSettingsRepository: AppDrawerSettingsRepository,
) {
    suspend operator fun invoke(percent: Int) = appDrawerSettingsRepository.setDrawerOpacityPercent(percent)
}
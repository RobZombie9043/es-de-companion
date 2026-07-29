package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.AppDrawerSettingsRepository

class SetGridColumnsUseCase(
    private val appDrawerSettingsRepository: AppDrawerSettingsRepository,
) {
    suspend operator fun invoke(columns: Int) = appDrawerSettingsRepository.setGridColumns(columns)
}
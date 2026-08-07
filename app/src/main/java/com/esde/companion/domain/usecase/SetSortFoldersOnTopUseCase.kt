package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.AppDrawerSettingsRepository

class SetSortFoldersOnTopUseCase(
    private val appDrawerSettingsRepository: AppDrawerSettingsRepository,
) {
    suspend operator fun invoke(sortOnTop: Boolean) = appDrawerSettingsRepository.setSortFoldersOnTop(sortOnTop)
}

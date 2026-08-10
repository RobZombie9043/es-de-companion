package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.AppDrawerSettingsRepository

class SetShowSearchBarUseCase(
    private val appDrawerSettingsRepository: AppDrawerSettingsRepository,
) {
    suspend operator fun invoke(show: Boolean) = appDrawerSettingsRepository.setShowSearchBar(show)
}

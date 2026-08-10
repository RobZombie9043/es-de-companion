package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.AppDrawerSettingsRepository
import kotlinx.coroutines.flow.Flow

class ObserveShowSearchBarUseCase(
    private val appDrawerSettingsRepository: AppDrawerSettingsRepository,
) {
    operator fun invoke(): Flow<Boolean> = appDrawerSettingsRepository.observeShowSearchBar()
}

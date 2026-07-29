package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.AppDrawerSettingsRepository
import kotlinx.coroutines.flow.Flow

class ObserveGridColumnsUseCase(
    private val appDrawerSettingsRepository: AppDrawerSettingsRepository,
) {
    operator fun invoke(): Flow<Int> = appDrawerSettingsRepository.observeGridColumns()
}
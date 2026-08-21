package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.ThorSettingsRepository
import kotlinx.coroutines.flow.Flow

class ObserveVolumeSyncEnabledUseCase(
    private val thorSettingsRepository: ThorSettingsRepository,
) {
    operator fun invoke(): Flow<Boolean> = thorSettingsRepository.observeVolumeSyncEnabled()
}

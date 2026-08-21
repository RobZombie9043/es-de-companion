package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.VolumeSyncMode
import com.esde.companion.domain.repository.ThorSettingsRepository
import kotlinx.coroutines.flow.Flow

class ObserveVolumeSyncModeUseCase(
    private val thorSettingsRepository: ThorSettingsRepository,
) {
    operator fun invoke(): Flow<VolumeSyncMode> = thorSettingsRepository.observeVolumeSyncMode()
}

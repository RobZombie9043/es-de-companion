package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.VolumeSyncMode
import com.esde.companion.domain.repository.ThorSettingsRepository

class SetVolumeSyncModeUseCase(
    private val thorSettingsRepository: ThorSettingsRepository,
) {
    suspend operator fun invoke(mode: VolumeSyncMode) = thorSettingsRepository.setVolumeSyncMode(mode)
}

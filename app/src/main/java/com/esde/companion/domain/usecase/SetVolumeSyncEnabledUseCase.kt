package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.ThorSettingsRepository

class SetVolumeSyncEnabledUseCase(
    private val thorSettingsRepository: ThorSettingsRepository,
) {
    suspend operator fun invoke(enabled: Boolean) = thorSettingsRepository.setVolumeSyncEnabled(enabled)
}

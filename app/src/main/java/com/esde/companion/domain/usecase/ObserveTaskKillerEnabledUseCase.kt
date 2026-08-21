package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.ThorSettingsRepository
import kotlinx.coroutines.flow.Flow

class ObserveTaskKillerEnabledUseCase(
    private val thorSettingsRepository: ThorSettingsRepository,
) {
    operator fun invoke(): Flow<Boolean> = thorSettingsRepository.observeTaskKillerEnabled()
}

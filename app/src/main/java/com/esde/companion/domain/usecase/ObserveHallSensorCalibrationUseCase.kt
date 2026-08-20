package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.HallSensorCalibration
import com.esde.companion.domain.repository.ThorSettingsRepository
import kotlinx.coroutines.flow.Flow

class ObserveHallSensorCalibrationUseCase(
    private val thorSettingsRepository: ThorSettingsRepository,
) {
    operator fun invoke(): Flow<HallSensorCalibration> = thorSettingsRepository.observeHallSensorCalibration()
}

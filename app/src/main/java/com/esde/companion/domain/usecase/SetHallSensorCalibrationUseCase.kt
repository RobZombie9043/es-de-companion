package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.HallSensorCalibration
import com.esde.companion.domain.repository.ThorSettingsRepository

class SetHallSensorCalibrationUseCase(
    private val thorSettingsRepository: ThorSettingsRepository,
) {
    suspend operator fun invoke(calibration: HallSensorCalibration) {
        thorSettingsRepository.setHallSensorCalibration(calibration)
    }
}

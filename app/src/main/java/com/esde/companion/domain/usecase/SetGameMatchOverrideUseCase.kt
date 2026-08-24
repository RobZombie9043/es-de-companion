package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.GameMatchOverride
import com.esde.companion.domain.repository.GameMatchOverrideRepository

class SetGameMatchOverrideUseCase(
    private val gameMatchOverrideRepository: GameMatchOverrideRepository,
) {
    suspend operator fun invoke(override: GameMatchOverride) = gameMatchOverrideRepository.setOverride(override)
}

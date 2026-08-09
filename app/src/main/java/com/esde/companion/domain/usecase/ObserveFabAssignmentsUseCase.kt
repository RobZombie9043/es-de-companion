package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.FabAssignments
import com.esde.companion.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.Flow

class ObserveFabAssignmentsUseCase(
    private val onboardingRepository: OnboardingRepository,
) {
    operator fun invoke(): Flow<FabAssignments> = onboardingRepository.observeFabAssignments()
}

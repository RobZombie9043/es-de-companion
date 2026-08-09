package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.FabAssignments
import com.esde.companion.domain.model.FabPosition
import com.esde.companion.domain.model.FabSlot
import com.esde.companion.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.first

/**
 * Resolves and persists a single corner reassignment, returning the resulting
 * [FabAssignments] so callers (see SettingsViewModel) can update their own state from the
 * authoritative post-swap result rather than duplicating [FabAssignments.with]'s
 * Music-exclusivity logic client-side.
 */
class SetFabAssignmentUseCase(
    private val onboardingRepository: OnboardingRepository,
) {
    suspend operator fun invoke(
        position: FabPosition,
        slot: FabSlot,
    ): FabAssignments {
        val updated = onboardingRepository.observeFabAssignments().first().with(position, slot)
        onboardingRepository.setFabAssignments(updated)
        return updated
    }
}

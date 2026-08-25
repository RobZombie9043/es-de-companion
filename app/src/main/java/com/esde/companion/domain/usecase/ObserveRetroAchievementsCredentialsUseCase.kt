package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.RetroAchievementsCredentials
import com.esde.companion.domain.repository.RetroAchievementsCredentialsRepository
import kotlinx.coroutines.flow.Flow

class ObserveRetroAchievementsCredentialsUseCase(
    private val retroAchievementsCredentialsRepository: RetroAchievementsCredentialsRepository,
) {
    operator fun invoke(): Flow<RetroAchievementsCredentials?> {
        return retroAchievementsCredentialsRepository.observeCredentials()
    }
}

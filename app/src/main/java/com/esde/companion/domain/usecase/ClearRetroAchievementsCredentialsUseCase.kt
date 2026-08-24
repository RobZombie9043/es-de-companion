package com.esde.companion.domain.usecase

import com.esde.companion.domain.repository.RetroAchievementsCredentialsRepository

class ClearRetroAchievementsCredentialsUseCase(
    private val retroAchievementsCredentialsRepository: RetroAchievementsCredentialsRepository,
) {
    suspend operator fun invoke() = retroAchievementsCredentialsRepository.clearCredentials()
}

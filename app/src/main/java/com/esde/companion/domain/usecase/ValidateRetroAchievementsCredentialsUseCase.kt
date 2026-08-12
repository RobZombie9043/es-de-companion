package com.esde.companion.domain.usecase

import com.esde.companion.domain.model.RetroAchievementsAuthState
import com.esde.companion.domain.model.RetroAchievementsCredentials
import com.esde.companion.domain.repository.RetroAchievementsCredentialsRepository
import com.esde.companion.domain.repository.RetroAchievementsRepository

/**
 * Validates a username/Web API Key against the live RetroAchievements API and, only on
 * success, persists it via [RetroAchievementsCredentialsRepository] - there is no "saved
 * but unvalidated" state, so a failed attempt leaves any previously-stored (valid)
 * credentials untouched. This is the sole write path to the credentials repository; see
 * Settings UX notes in CLAUDE.md's RetroAchievements section.
 */
class ValidateRetroAchievementsCredentialsUseCase(
    private val retroAchievementsRepository: RetroAchievementsRepository,
    private val retroAchievementsCredentialsRepository: RetroAchievementsCredentialsRepository,
) {
    suspend operator fun invoke(credentials: RetroAchievementsCredentials): RetroAchievementsAuthState {
        val authState = retroAchievementsRepository.validateCredentials(credentials)
        if (authState is RetroAchievementsAuthState.SignedIn) {
            retroAchievementsCredentialsRepository.setCredentials(credentials)
        }
        return authState
    }
}

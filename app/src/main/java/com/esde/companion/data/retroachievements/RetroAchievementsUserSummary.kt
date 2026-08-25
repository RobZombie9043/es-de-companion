package com.esde.companion.data.retroachievements

/**
 * A minimal projection of api-kotlin's `GetUserSummary.Response` - just the fields
 * [RetroAchievementsRepositoryImpl.validateCredentials] needs to build a
 * [com.esde.companion.domain.model.RetroAchievementsAuthState.SignedIn]. Keeping this
 * narrow (rather than exposing the real ~30-field response DTO through [RetroAchievementsApi])
 * is what makes a hand-rolled fake of that interface trivial to write in tests.
 */
data class RetroAchievementsUserSummary(
    val username: String,
    val points: Int,
    val avatarUrl: String?,
)

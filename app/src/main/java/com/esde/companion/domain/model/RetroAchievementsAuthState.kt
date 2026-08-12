package com.esde.companion.domain.model

/**
 * Whether the app currently has valid RetroAchievements credentials. Deliberately has no
 * "Validating" case - a coroutine mid-validation is transient UI/operation state, not a
 * fact about authentication the domain layer should model. The Settings ViewModel tracks
 * that itself (e.g. a separate `isConnecting: Boolean` alongside this state).
 */
sealed class RetroAchievementsAuthState {
    data object SignedOut : RetroAchievementsAuthState()

    data class SignedIn(
        val username: String,
        val points: Int,
        val avatarUrl: String?,
    ) : RetroAchievementsAuthState()

    data class Error(val message: String) : RetroAchievementsAuthState()
}

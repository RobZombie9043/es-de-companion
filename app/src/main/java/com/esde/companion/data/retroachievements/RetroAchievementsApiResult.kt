package com.esde.companion.data.retroachievements

/**
 * Outcome of a single [RetroAchievementsApi] call. Isolates callers - and their unit tests
 * - from api-kotlin's own Retrofit-shaped `NetworkResponse`/`ErrorResponse` types, which is
 * the whole point of the [RetroAchievementsApi] seam (see its kdoc).
 */
sealed class RetroAchievementsApiResult<out T> {
    data class Success<T>(val data: T) : RetroAchievementsApiResult<T>()

    data class Error(val message: String) : RetroAchievementsApiResult<Nothing>()
}

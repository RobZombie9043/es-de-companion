package com.esde.companion.domain.model

/** Result of comparing the latest GitHub release against the currently running version. */
sealed class UpdateCheckResult {
    data class UpdateAvailable(val release: ReleaseInfo) : UpdateCheckResult()

    data object UpToDate : UpdateCheckResult()

    data class Failed(val reason: UpdateCheckFailureReason) : UpdateCheckResult()
}

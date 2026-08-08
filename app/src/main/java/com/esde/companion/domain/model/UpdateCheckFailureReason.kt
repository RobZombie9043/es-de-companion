package com.esde.companion.domain.model

/** Why [CheckForUpdateUseCase] couldn't determine an update result. */
sealed class UpdateCheckFailureReason {
    data object NoReleaseFound : UpdateCheckFailureReason()

    /** A release was found, but it has no `.apk` asset attached - nothing to install. */
    data object NoApkAsset : UpdateCheckFailureReason()

    data class Network(val message: String) : UpdateCheckFailureReason()
}

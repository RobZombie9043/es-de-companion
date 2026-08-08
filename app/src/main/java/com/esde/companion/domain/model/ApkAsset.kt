package com.esde.companion.domain.model

/** A downloadable `.apk` file attached to a GitHub release. */
data class ApkAsset(
    val downloadUrl: String,
    val fileName: String,
    val sizeBytes: Long,
)

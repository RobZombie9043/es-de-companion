package com.esde.companion.domain.model

/** Progress of an in-flight APK download - see [UpdateRepository.downloadApk]. */
sealed class DownloadState {
    data class Progress(val percent: Int) : DownloadState()

    data class Success(val filePath: String) : DownloadState()

    data class Failed(val message: String) : DownloadState()
}

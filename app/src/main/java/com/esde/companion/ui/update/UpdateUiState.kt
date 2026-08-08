package com.esde.companion.ui.update

import com.esde.companion.domain.model.DownloadState
import com.esde.companion.domain.model.ReleaseInfo
import com.esde.companion.domain.model.UpdateCheckResult

data class UpdateUiState(
    val isCheckingForUpdate: Boolean = false,
    // Only set by the manual "Check for Updates" tap - drives the Setup row's status
    // text. The silent startup check never touches this, so it doesn't claim "up to
    // date"/"check failed" on the Setup row before the user has ever opened Settings.
    val lastManualCheckResult: UpdateCheckResult? = null,
    val showUpdateDialog: Boolean = false,
    val availableRelease: ReleaseInfo? = null,
    val downloadState: DownloadState? = null,
    val showWhatsNewDialog: Boolean = false,
    val whatsNewRelease: ReleaseInfo? = null,
)

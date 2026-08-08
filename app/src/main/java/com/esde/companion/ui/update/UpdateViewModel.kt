package com.esde.companion.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esde.companion.domain.model.ReleaseFetchResult
import com.esde.companion.domain.model.UpdateCheckResult
import com.esde.companion.domain.usecase.CheckForUpdateUseCase
import com.esde.companion.domain.usecase.DownloadApkUseCase
import com.esde.companion.domain.usecase.FetchReleaseNotesForVersionUseCase
import com.esde.companion.domain.usecase.ObserveLastSeenChangelogVersionCodeUseCase
import com.esde.companion.domain.usecase.SetLastSeenChangelogVersionCodeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Drives both the update-checker flows: a silent check on every app start (auto-showing
 * a dialog if an update is found) plus a manual "Check for Updates" entry in Settings ->
 * Setup, and a one-time "what's new" prompt the first startup after the app's own
 * versionCode has increased since it was last recorded.
 *
 * Deliberately a standalone ViewModel rather than folded into [com.esde.companion.ui.settings.SettingsViewModel]:
 * its dialogs must render at MainActivity's top level regardless of whether the Settings
 * popup is even open, unlike everything else that ViewModel owns, and SettingsViewModel
 * is already the largest class in the codebase.
 *
 * Never holds a [android.content.Context] - matches every other ViewModel here being
 * plain [ViewModel], not `AndroidViewModel`. The actual install intent is launched by
 * the dialog composable itself, which already has `LocalContext.current`.
 */
class UpdateViewModel(
    private val checkForUpdateUseCase: CheckForUpdateUseCase,
    private val fetchReleaseNotesForVersionUseCase: FetchReleaseNotesForVersionUseCase,
    private val downloadApkUseCase: DownloadApkUseCase,
    private val observeLastSeenChangelogVersionCodeUseCase: ObserveLastSeenChangelogVersionCodeUseCase,
    private val setLastSeenChangelogVersionCodeUseCase: SetLastSeenChangelogVersionCodeUseCase,
    private val runningAppVersion: RunningAppVersion,
) : ViewModel() {
    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    /** One-shot, called from MainActivity's startup `LaunchedEffect(Unit)`. */
    fun runStartupChecks() {
        viewModelScope.launch {
            checkWhatsNewIfNeeded()
            checkForUpdatesSilently()
        }
    }

    fun checkForUpdatesManually() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCheckingForUpdate = true)
            val result = checkForUpdateUseCase(runningAppVersion.name)
            _uiState.value = _uiState.value.copy(isCheckingForUpdate = false, lastManualCheckResult = result)
            showUpdateDialogIfAvailable(result)
        }
    }

    fun onDownloadAndInstallClicked() {
        val asset = _uiState.value.availableRelease?.apkAsset ?: return
        viewModelScope.launch {
            downloadApkUseCase(asset).collect { state ->
                _uiState.value = _uiState.value.copy(downloadState = state)
            }
        }
    }

    fun onUpdateDialogDismissed() {
        _uiState.value = _uiState.value.copy(showUpdateDialog = false)
    }

    fun onWhatsNewDialogDismissed() {
        _uiState.value = _uiState.value.copy(showWhatsNewDialog = false)
    }

    private suspend fun checkForUpdatesSilently() {
        showUpdateDialogIfAvailable(checkForUpdateUseCase(runningAppVersion.name))
    }

    private fun showUpdateDialogIfAvailable(result: UpdateCheckResult) {
        if (result is UpdateCheckResult.UpdateAvailable) {
            _uiState.value = _uiState.value.copy(showUpdateDialog = true, availableRelease = result.release)
        }
    }

    private suspend fun checkWhatsNewIfNeeded() {
        val lastSeen = observeLastSeenChangelogVersionCodeUseCase().first()

        if (lastSeen == null) {
            // First-ever run: nothing to diff against, just seed the baseline.
            setLastSeenChangelogVersionCodeUseCase(runningAppVersion.code)
            return
        }

        if (runningAppVersion.code > lastSeen) {
            val result = fetchReleaseNotesForVersionUseCase(runningAppVersion.name)
            if (result is ReleaseFetchResult.Success) {
                _uiState.value = _uiState.value.copy(showWhatsNewDialog = true, whatsNewRelease = result.release)
            }
            // Recorded even when the notes fetch failed, so a persistent network issue
            // doesn't re-attempt (and potentially nag) on every single app start.
            setLastSeenChangelogVersionCodeUseCase(runningAppVersion.code)
        }
    }
}

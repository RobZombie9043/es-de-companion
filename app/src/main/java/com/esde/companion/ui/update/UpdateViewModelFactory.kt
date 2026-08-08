package com.esde.companion.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.esde.companion.AppContainer
import com.esde.companion.BuildConfig

class UpdateViewModelFactory(
    private val appContainer: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(UpdateViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return UpdateViewModel(
            checkForUpdateUseCase = appContainer.checkForUpdateUseCase,
            fetchReleaseNotesForVersionUseCase = appContainer.fetchReleaseNotesForVersionUseCase,
            downloadApkUseCase = appContainer.downloadApkUseCase,
            observeLastSeenChangelogVersionCodeUseCase = appContainer.observeLastSeenChangelogVersionCodeUseCase,
            setLastSeenChangelogVersionCodeUseCase = appContainer.setLastSeenChangelogVersionCodeUseCase,
            runningAppVersion = RunningAppVersion(name = BuildConfig.VERSION_NAME, code = BuildConfig.VERSION_CODE),
        ) as T
    }
}

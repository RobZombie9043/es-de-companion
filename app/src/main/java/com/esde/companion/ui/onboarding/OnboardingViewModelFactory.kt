package com.esde.companion.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.esde.companion.AppContainer

class OnboardingViewModelFactory(
    private val appContainer: AppContainer,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(OnboardingViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return OnboardingViewModel(
            onboardingRepository = appContainer.onboardingRepository,
            validateLogFolderUseCase = appContainer.validateEsdeLogFolderUseCase,
            validateMediaFolderUseCase = appContainer.validateEsdeMediaFolderUseCase,
            completeOnboardingUseCase = appContainer.completeOnboardingUseCase,
            readEsdeMediaDirectoryUseCase = appContainer.readEsdeMediaDirectoryUseCase,
            readEsdeEventScriptSettingsUseCase = appContainer.readEsdeEventScriptSettingsUseCase,
            observeEsdeEventScriptSettingsUseCase = appContainer.observeEsdeEventScriptSettingsUseCase,
            findLegacyScriptFilesUseCase = appContainer.findLegacyScriptFilesUseCase,
            deleteLegacyScriptFilesUseCase = appContainer.deleteLegacyScriptFilesUseCase,
            observeConnectionStateUseCase = appContainer.observeConnectionStateUseCase,
        ) as T
    }
}
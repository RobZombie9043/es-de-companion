package com.esde.companion.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.esde.companion.AppContainer

/**
 * [initialLogFolderPath]/[initialMediaFolderPath] are whatever the caller already knows was
 * previously confirmed and persisted (or null on a genuinely fresh install) - see
 * MainActivity's OnboardingStartupInfo, fetched before this factory is constructed so
 * OnboardingViewModel can seed its initial state from them.
 */
class OnboardingViewModelFactory(
    private val appContainer: AppContainer,
    private val initialLogFolderPath: String? = null,
    private val initialMediaFolderPath: String? = null,
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
            observeEsdeLogActivityUseCase = appContainer.observeEsdeLogActivityUseCase,
            savedLogFolderPath = initialLogFolderPath,
            savedMediaFolderPath = initialMediaFolderPath,
        ) as T
    }
}

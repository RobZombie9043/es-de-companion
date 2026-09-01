package com.esde.companion.ui.gameguides

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.esde.companion.AppContainer

class GameGuidesViewModelFactory(
    private val appContainer: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(GameGuidesViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        val useCases =
            GameGuidesUseCases(
                observeGameGuides = appContainer.observeGameGuidesUseCase,
                saveGameGuide = appContainer.saveGameGuideUseCase,
                importGameGuide = appContainer.importGameGuideUseCase,
                loadGameGuideContent = appContainer.loadGameGuideContentUseCase,
                loadGameGuidePage = appContainer.loadGameGuidePageUseCase,
                loadGameGuideBinaryPath = appContainer.loadGameGuideBinaryPathUseCase,
                deleteGameGuide = appContainer.deleteGameGuideUseCase,
                observeDisplayPreferences = appContainer.observeGameGuideDisplayPreferencesUseCase,
                setDisplayPreferences = appContainer.setGameGuideDisplayPreferencesUseCase,
                observeReadingProgress = appContainer.observeGameGuideReadingProgressUseCase,
                setReadingProgress = appContainer.setGameGuideReadingProgressUseCase,
                resolveGameMedia = appContainer.resolveGameMediaUseCase,
                resolveGameGuideMediaDirectory = appContainer.resolveGameGuideMediaDirectoryUseCase,
            )
        val observeUpdateOnScreensaver = appContainer.observeUpdateGameGuidesOnScreensaverEnabledUseCase
        return GameGuidesViewModel(
            observeConnectionState = appContainer.observeConnectionStateUseCase,
            observeUpdateGameGuidesOnScreensaverEnabled = observeUpdateOnScreensaver,
            useCases = useCases,
        ) as T
    }
}

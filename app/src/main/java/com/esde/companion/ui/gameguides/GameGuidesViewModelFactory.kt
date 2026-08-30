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
                loadGameGuideContent = appContainer.loadGameGuideContentUseCase,
                deleteGameGuide = appContainer.deleteGameGuideUseCase,
                observeDisplayPreferences = appContainer.observeGameGuideDisplayPreferencesUseCase,
                setDisplayPreferences = appContainer.setGameGuideDisplayPreferencesUseCase,
                observeReadingProgress = appContainer.observeGameGuideReadingProgressUseCase,
                setReadingProgress = appContainer.setGameGuideReadingProgressUseCase,
            )
        return GameGuidesViewModel(
            observeConnectionState = appContainer.observeConnectionStateUseCase,
            useCases = useCases,
        ) as T
    }
}

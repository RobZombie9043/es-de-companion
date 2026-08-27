package com.esde.companion.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.esde.companion.AppContainer

class GameLaunchOverrideViewModelFactory(
    private val appContainer: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(GameLaunchOverrideViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return GameLaunchOverrideViewModel(
            listGamelistSystems = appContainer.listGamelistSystemsUseCase,
            listGamelistGames = appContainer.listGamelistGamesUseCase,
            observeInstalledApps = appContainer.observeInstalledAppsUseCase,
            observeGameLaunchSystemDefaults = appContainer.observeGameLaunchSystemDefaultsUseCase,
            setGameLaunchSystemDefault = appContainer.setGameLaunchSystemDefaultUseCase,
            observeGameLaunchOverrides = appContainer.observeGameLaunchOverridesUseCase,
            setGameLaunchOverride = appContainer.setGameLaunchOverrideUseCase,
            clearGameLaunchOverride = appContainer.clearGameLaunchOverrideUseCase,
        ) as T
    }
}

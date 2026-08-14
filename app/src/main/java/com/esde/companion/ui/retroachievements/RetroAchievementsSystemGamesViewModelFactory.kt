package com.esde.companion.ui.retroachievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.esde.companion.AppContainer

class RetroAchievementsSystemGamesViewModelFactory(
    private val appContainer: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(RetroAchievementsSystemGamesViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        val observeUpdateOnScreensaver = appContainer.observeUpdateAchievementsOnScreensaverEnabledUseCase
        return RetroAchievementsSystemGamesViewModel(
            observeConnectionState = appContainer.observeConnectionStateUseCase,
            observeCredentials = appContainer.observeRetroAchievementsCredentialsUseCase,
            observeUpdateAchievementsOnScreensaverEnabled = observeUpdateOnScreensaver,
            getSystemGames = appContainer.getRetroAchievementsSystemGamesUseCase,
            getAchievementSummary = appContainer.getGameAchievementSummaryUseCase,
            getUserGameProgress = appContainer.getUserGameProgressUseCase,
        ) as T
    }
}

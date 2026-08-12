package com.esde.companion.ui.retroachievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.esde.companion.AppContainer

class RetroAchievementsViewModelFactory(
    private val appContainer: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(RetroAchievementsViewModel::class.java)) {
            "Unknown ViewModel class: $modelClass"
        }
        return RetroAchievementsViewModel(
            observeConnectionState = appContainer.observeConnectionStateUseCase,
            observeCredentials = appContainer.observeRetroAchievementsCredentialsUseCase,
            resolveGame = appContainer.resolveRetroAchievementsGameUseCase,
            getAchievementSummary = appContainer.getGameAchievementSummaryUseCase,
        ) as T
    }
}

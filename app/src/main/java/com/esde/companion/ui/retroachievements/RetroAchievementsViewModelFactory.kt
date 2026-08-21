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
        val detailUseCases =
            RetroAchievementsDetailUseCases(
                getAchievementSummary = appContainer.getGameAchievementSummaryUseCase,
                getHashSupport = appContainer.getGameHashSupportUseCase,
            )
        val observeUpdateOnScreensaver = appContainer.observeUpdateAchievementsOnScreensaverEnabledUseCase
        return RetroAchievementsViewModel(
            observeConnectionState = appContainer.observeConnectionStateUseCase,
            observeCredentials = appContainer.observeRetroAchievementsCredentialsUseCase,
            observeUpdateAchievementsOnScreensaverEnabled = observeUpdateOnScreensaver,
            resolveGame = appContainer.resolveRetroAchievementsGameUseCase,
            detailUseCases = detailUseCases,
            searchGames = appContainer.searchRetroAchievementsGamesUseCase,
            setGameMatchOverride = appContainer.setGameMatchOverrideUseCase,
            getAchievementComments = appContainer.getAchievementCommentsUseCase,
        ) as T
    }
}

package com.esde.companion.ui.gameguides

import com.esde.companion.domain.usecase.DeleteGameGuideUseCase
import com.esde.companion.domain.usecase.LoadGameGuideContentUseCase
import com.esde.companion.domain.usecase.ObserveGameGuideDisplayPreferencesUseCase
import com.esde.companion.domain.usecase.ObserveGameGuideReadingProgressUseCase
import com.esde.companion.domain.usecase.ObserveGameGuidesUseCase
import com.esde.companion.domain.usecase.SaveGameGuideUseCase
import com.esde.companion.domain.usecase.SetGameGuideDisplayPreferencesUseCase
import com.esde.companion.domain.usecase.SetGameGuideReadingProgressUseCase

/** Bundles every Game Guides use case [GameGuidesViewModel] needs into one constructor
 * parameter - same reasoning as RetroAchievementsDetailUseCases/RetroAchievementsCaches. */
data class GameGuidesUseCases(
    val observeGameGuides: ObserveGameGuidesUseCase,
    val saveGameGuide: SaveGameGuideUseCase,
    val loadGameGuideContent: LoadGameGuideContentUseCase,
    val deleteGameGuide: DeleteGameGuideUseCase,
    val observeDisplayPreferences: ObserveGameGuideDisplayPreferencesUseCase,
    val setDisplayPreferences: SetGameGuideDisplayPreferencesUseCase,
    val observeReadingProgress: ObserveGameGuideReadingProgressUseCase,
    val setReadingProgress: SetGameGuideReadingProgressUseCase,
)

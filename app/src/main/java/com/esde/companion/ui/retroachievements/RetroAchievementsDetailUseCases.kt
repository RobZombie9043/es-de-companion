package com.esde.companion.ui.retroachievements

import com.esde.companion.domain.usecase.GetGameAchievementSummaryUseCase
import com.esde.companion.domain.usecase.GetGameHashSupportUseCase

/**
 * Bundles the two use cases [RetroAchievementsViewModel] calls once a game has already
 * resolved to an RA gameId - fetching its achievement summary and its hash-support detail -
 * into a single constructor parameter, the same "bundle related params to stay under
 * detekt's LongParameterList limit" convention `SelfHealConfig` uses (see CLAUDE.md).
 */
class RetroAchievementsDetailUseCases(
    val getAchievementSummary: GetGameAchievementSummaryUseCase,
    val getHashSupport: GetGameHashSupportUseCase,
)

package com.esde.companion.ui.retroachievements

import com.esde.companion.domain.usecase.GetGameAchievementSummaryUseCase
import com.esde.companion.domain.usecase.GetGameHashSupportUseCase
import com.esde.companion.domain.usecase.GetGameLeaderboardsUseCase
import com.esde.companion.domain.usecase.PeekGameAchievementSummaryUseCase
import com.esde.companion.domain.usecase.PeekGameLeaderboardsUseCase

/**
 * Bundles the use cases [RetroAchievementsViewModel] calls once a game has already resolved to
 * an RA gameId - fetching its achievement summary, its leaderboard list, and its hash-support
 * detail - into a single constructor parameter, the same "bundle related params to stay under
 * detekt's LongParameterList limit" convention `SelfHealConfig` uses (see CLAUDE.md).
 *
 * Scoped to callers that need these fetches together - today, only [RetroAchievementsViewModel].
 * [RetroAchievementsSystemGamesViewModel] never needs hash support (its game is explicitly
 * picked, not automatically resolved, so there's nothing to correct), so it takes
 * [GetGameAchievementSummaryUseCase]/[GetGameLeaderboardsUseCase]/[PeekGameAchievementSummaryUseCase]/
 * [PeekGameLeaderboardsUseCase] directly instead of depending on this bundle.
 */
class RetroAchievementsDetailUseCases(
    val getAchievementSummary: GetGameAchievementSummaryUseCase,
    val peekAchievementSummary: PeekGameAchievementSummaryUseCase,
    val getGameLeaderboards: GetGameLeaderboardsUseCase,
    val peekGameLeaderboards: PeekGameLeaderboardsUseCase,
    val getHashSupport: GetGameHashSupportUseCase,
)

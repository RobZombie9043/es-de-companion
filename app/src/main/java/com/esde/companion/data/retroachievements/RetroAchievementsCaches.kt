package com.esde.companion.data.retroachievements

/**
 * Bundles every RetroAchievements cache into one constructor parameter for
 * [RetroAchievementsRepositoryImpl] - the same "bundle related params to stay under detekt's
 * LongParameterList limit" convention `SelfHealConfig`/`BackupRepositories` use (see CLAUDE.md).
 */
data class RetroAchievementsCaches(
    val gameList: GameListCache,
    val userProgress: UserProgressCache,
    val achievementSummary: AchievementSummaryCache,
    val achievementComments: AchievementCommentsCache,
    val gameLeaderboards: GameLeaderboardsCache,
    val leaderboardEntries: LeaderboardEntriesCache,
)

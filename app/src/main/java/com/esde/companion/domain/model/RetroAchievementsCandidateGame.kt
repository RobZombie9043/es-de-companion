package com.esde.companion.domain.model

/**
 * One entry from RetroAchievements' game list for a console, as returned by `GetGameList`
 * and cached per-console (see `GameListCache`, data layer). Backs both the automatic title
 * matcher ([com.esde.companion.domain.parser.GameTitleMatcher]) and the manual search picker
 * - both draw from the same cached data.
 */
data class RetroAchievementsCandidateGame(
    val gameId: Long,
    val title: String,
    val iconUrl: String?,
)

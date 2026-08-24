package com.esde.companion.domain.model

/** Whether a game must have any achievements at all to appear in the system browser. */
enum class HasAchievementsFilter {
    Yes,
    No,
    Both,
}

/** A game's completion status, per [ProgressStatus] - [AllGames] shows every status. */
enum class ProgressFilter {
    AllGames,
    None,
    Some,
    Beaten,
    Mastered,
}

/**
 * The system-wide games browser's combined sort/filter selection.
 *
 * [gameTypes] is **OR within the facet**: any game carrying at least one selected tag matches -
 * unlike [AchievementFilterOption]'s two independent facets, which AND together, this is one
 * facet with several possible values, so requiring all of them simultaneously would usually
 * match nothing (most games carry at most one tag). An empty set means the facet is skipped
 * entirely (show every game type), not "match none".
 */
data class SystemGameFilters(
    val sortOrder: SystemGameSortOrder = SystemGameSortOrder.TitleAToZ,
    val gameTypes: Set<RetroGameType> = emptySet(),
    val hasAchievements: HasAchievementsFilter = HasAchievementsFilter.Yes,
    val progress: ProgressFilter = ProgressFilter.AllGames,
)

/**
 * Pure filter (sorting is separate - see [sortedBySystemGameOrder]). [gameTypesByGameId] and
 * [progressByGameId] are precomputed by the caller once per game list, not recomputed per game
 * per call, since a large system's list runs to thousands of entries.
 */
fun List<RetroAchievementsCandidateGame>.filteredBySystemGameFilters(
    filters: SystemGameFilters,
    gameTypesByGameId: Map<Long, Set<RetroGameType>>,
    progressByGameId: Map<Long, UserGameProgress>,
): List<RetroAchievementsCandidateGame> =
    filter { game -> matchesGameType(game, filters.gameTypes, gameTypesByGameId) }
        .filter { game -> matchesHasAchievements(game, filters.hasAchievements) }
        .filter { game -> matchesProgress(game, filters.progress, progressByGameId) }

private fun matchesGameType(
    game: RetroAchievementsCandidateGame,
    selectedTypes: Set<RetroGameType>,
    gameTypesByGameId: Map<Long, Set<RetroGameType>>,
): Boolean {
    if (selectedTypes.isEmpty()) return true
    val types = gameTypesByGameId[game.gameId].orEmpty()
    return types.any { it in selectedTypes }
}

private fun matchesHasAchievements(
    game: RetroAchievementsCandidateGame,
    filter: HasAchievementsFilter,
): Boolean =
    when (filter) {
        HasAchievementsFilter.Both -> true
        HasAchievementsFilter.Yes -> game.numAchievements > 0
        HasAchievementsFilter.No -> game.numAchievements == 0
    }

private fun matchesProgress(
    game: RetroAchievementsCandidateGame,
    filter: ProgressFilter,
    progressByGameId: Map<Long, UserGameProgress>,
): Boolean {
    if (filter == ProgressFilter.AllGames) return true
    val status = progressByGameId[game.gameId]?.status ?: ProgressStatus.None
    return when (filter) {
        ProgressFilter.AllGames -> true
        ProgressFilter.None -> status == ProgressStatus.None
        ProgressFilter.Some -> status == ProgressStatus.Some
        ProgressFilter.Beaten -> status == ProgressStatus.Beaten
        ProgressFilter.Mastered -> status == ProgressStatus.Mastered
    }
}

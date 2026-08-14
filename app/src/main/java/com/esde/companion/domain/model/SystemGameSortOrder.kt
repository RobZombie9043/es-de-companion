package com.esde.companion.domain.model

private const val PROGRESS_PERCENT_MULTIPLIER = 100f

/**
 * Sort options for the system-wide games browser. No Players option - `GetGameList` (the bulk
 * per-system call this browser is built on) carries no player-count field at all, confirmed by
 * direct inspection of the real response shape; a per-game player count only exists on the
 * per-game detail endpoint used by [AchievementSortOrder]'s game view, one call per game, which
 * isn't viable across a system's full game list.
 */
enum class SystemGameSortOrder {
    AchievementsMost,
    AchievementsLeast,
    PointsMost,
    PointsLeast,
    ProgressMost,
    ProgressLeast,
    TitleAToZ,
    TitleZToA,
}

/**
 * Pure sort - every Most/Least comparator tie-breaks by lowercased title, since ties on
 * achievement/point/progress counts are extremely common across a system's full game list and
 * would otherwise sort nondeterministically between recompositions. Progress percent is looked
 * up from [progressByGameId]; a game absent from it (no recorded progress) sorts as 0%.
 */
fun List<RetroAchievementsCandidateGame>.sortedBySystemGameOrder(
    order: SystemGameSortOrder,
    progressByGameId: Map<Long, UserGameProgress>,
): List<RetroAchievementsCandidateGame> {
    fun progressPercent(game: RetroAchievementsCandidateGame): Float {
        val progress = progressByGameId[game.gameId]
        return if (progress == null || progress.maxPossible <= 0) {
            0f
        } else {
            progress.numAwarded * PROGRESS_PERCENT_MULTIPLIER / progress.maxPossible
        }
    }

    val titleTieBreak: (RetroAchievementsCandidateGame) -> String = { it.title.lowercase() }
    return when (order) {
        SystemGameSortOrder.AchievementsMost ->
            sortedWith(compareByDescending<RetroAchievementsCandidateGame> { it.numAchievements }.thenBy(titleTieBreak))
        SystemGameSortOrder.AchievementsLeast ->
            sortedWith(compareBy<RetroAchievementsCandidateGame> { it.numAchievements }.thenBy(titleTieBreak))
        SystemGameSortOrder.PointsMost ->
            sortedWith(compareByDescending<RetroAchievementsCandidateGame> { it.totalPoints }.thenBy(titleTieBreak))
        SystemGameSortOrder.PointsLeast ->
            sortedWith(compareBy<RetroAchievementsCandidateGame> { it.totalPoints }.thenBy(titleTieBreak))
        SystemGameSortOrder.ProgressMost ->
            sortedWith(compareByDescending<RetroAchievementsCandidateGame>(::progressPercent).thenBy(titleTieBreak))
        SystemGameSortOrder.ProgressLeast ->
            sortedWith(compareBy<RetroAchievementsCandidateGame>(::progressPercent).thenBy(titleTieBreak))
        SystemGameSortOrder.TitleAToZ -> sortedBy(titleTieBreak)
        SystemGameSortOrder.TitleZToA -> sortedByDescending(titleTieBreak)
    }
}

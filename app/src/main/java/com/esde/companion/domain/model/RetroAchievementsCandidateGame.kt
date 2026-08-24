package com.esde.companion.domain.model

/**
 * One entry from RetroAchievements' game list for a console, as returned by `GetGameList`
 * and cached per-console (see `GameListCache`, data layer). Backs the automatic title
 * matcher ([com.esde.companion.domain.parser.GameTitleMatcher]), the automatic ROM-hash
 * matcher ([com.esde.companion.domain.parser.GameHashMatcher]), the manual search picker,
 * and the system-wide games browser (`RetroAchievementsSystemGamesScreen`) - all four draw
 * from the same cached data. [numAchievements]/[totalPoints] come straight from `GetGameList`
 * itself (RA's own summary counts for the game), not a per-game achievement-summary fetch -
 * that's what lets the system browser show "suggested details" for hundreds of games at once
 * without hundreds of extra network calls. [hashes] is RA's set of known ROM hashes for this
 * game, populated only because `GetGameList` is called with `shouldRetrieveGameHashes = 1`
 * (see `RetroClientRetroAchievementsApi`) - an empty list is normal (an older disk-cached
 * entry from before this field existed, or a game RA simply has no hashes logged for), not
 * an error.
 */
data class RetroAchievementsCandidateGame(
    val gameId: Long,
    val title: String,
    val iconUrl: String?,
    val numAchievements: Int = 0,
    val totalPoints: Int = 0,
    val hashes: List<String> = emptyList(),
)

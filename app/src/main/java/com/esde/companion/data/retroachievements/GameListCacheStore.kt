package com.esde.companion.data.retroachievements

import com.esde.companion.domain.model.RetroAchievementsCandidateGame

/** A cached [RetroAchievementsCandidateGame] list for one console, and when it was fetched. */
data class CachedGameList(
    val fetchedAtMillis: Long,
    val games: List<RetroAchievementsCandidateGame>,
)

/**
 * Persists [GameListCache]'s per-console cache. A seam purely for testability - same
 * reasoning as [RetroAchievementsApi] - so [GameListCache]'s TTL/stale-fallback logic can be
 * unit-tested against a hand-rolled fake instead of a real DataStore, which needs a real
 * Android [android.content.Context] to construct.
 */
interface GameListCacheStore {
    suspend fun read(consoleId: Long): CachedGameList?

    suspend fun write(
        consoleId: Long,
        cached: CachedGameList,
    )
}

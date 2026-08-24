package com.esde.companion.data.retroachievements

import com.esde.companion.domain.model.UserGameProgress

/** A cached completion-progress snapshot for one user, keyed by gameId, and when it was fetched. */
data class CachedUserProgress(
    val fetchedAtMillis: Long,
    val progressByGameId: Map<Long, UserGameProgress>,
)

/**
 * Persists [UserProgressCache]'s per-username cache. A seam purely for testability - same
 * reasoning as [GameListCacheStore] - so [UserProgressCache]'s TTL/stale-fallback/pagination
 * logic can be unit-tested against a hand-rolled fake instead of a real DataStore, which needs
 * a real Android [android.content.Context] to construct.
 */
interface UserProgressCacheStore {
    suspend fun read(username: String): CachedUserProgress?

    suspend fun write(
        username: String,
        cached: CachedUserProgress,
    )
}

package com.esde.companion.domain.repository

import com.esde.companion.domain.model.GameMatchOverride
import com.esde.companion.domain.model.GameReference
import kotlinx.coroutines.flow.Flow

/**
 * Persisted per-ROM RetroAchievements game-match corrections. Not a secret - plain
 * DataStore, same pattern as any other small settings repository (unlike credentials).
 * [observeAllOverrides] backs Backup & Restore's export, since it's user-curated library
 * data rather than sensitive; [getOverride] is the one-shot lookup
 * `ResolveRetroAchievementsGameUseCase` uses each time the current game changes.
 */
interface GameMatchOverrideRepository {
    suspend fun setOverride(override: GameMatchOverride)

    suspend fun clearOverride(gameReference: GameReference)

    suspend fun getOverride(gameReference: GameReference): GameMatchOverride?

    fun observeAllOverrides(): Flow<List<GameMatchOverride>>
}

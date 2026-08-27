package com.esde.companion.domain.repository

import com.esde.companion.domain.model.GameLaunchDisplayTarget
import com.esde.companion.domain.model.GameLaunchOverride
import kotlinx.coroutines.flow.Flow

/**
 * Persisted Game Launch Override settings - a system-wide default app per system short name, and
 * per-game overrides that take precedence over it (see `resolveGameLaunchPackage`). Plain,
 * non-secret DataStore, same pattern as `GameMatchOverrideRepository`; both flows back Backup &
 * Restore's export via [BackupRepositories], since this is user-curated preference data rather
 * than anything sensitive.
 */
interface GameLaunchAppRepository {
    fun observeSystemDefaults(): Flow<Map<String, String>>

    /** [packageName] null clears the system default (falls back to "launch nothing"). */
    suspend fun setSystemDefault(
        systemShortName: String,
        packageName: String?,
    )

    fun observeGameOverrides(): Flow<List<GameLaunchOverride>>

    /** [packageName] present-but-null persists an explicit "never launch anything for this
     * game" entry - distinct from [clearGameOverride], which removes the entry entirely so the
     * game falls back to inheriting the system default. */
    suspend fun setGameOverride(
        systemShortName: String,
        relativeRomPath: String,
        packageName: String?,
    )

    /** Removes any override for this game, so it goes back to inheriting the system default. */
    suspend fun clearGameOverride(
        systemShortName: String,
        relativeRomPath: String,
    )

    /** Global (not per-system/per-game) setting for which display a launched app appears on -
     * see [GameLaunchDisplayTarget]'s kdoc. */
    fun observeLaunchDisplayTarget(): Flow<GameLaunchDisplayTarget>

    suspend fun setLaunchDisplayTarget(target: GameLaunchDisplayTarget)
}

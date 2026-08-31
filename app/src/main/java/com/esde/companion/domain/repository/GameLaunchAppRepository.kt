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
@Suppress("TooManyFunctions")
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

    /** Global (not per-system/per-game) setting: whether to close the app a Game Launch
     * Override started once the game that triggered it ends - see
     * [com.esde.companion.data.gamelist.GameLaunchOverrideCoordinator]'s kdoc for what "close"
     * and "ends" mean here. Off by default. */
    fun observeCloseAppOnGameEnd(): Flow<Boolean>

    suspend fun setCloseAppOnGameEnd(enabled: Boolean)

    /** Master on/off switch for the whole Game Launch Override feature - off suppresses every
     * launch regardless of configured system defaults/game overrides. Defaults true: this
     * feature ran unconditionally before this switch existed (an unconfigured system/game
     * already resolved to "launch nothing"), so anyone with overrides already configured keeps
     * getting them until they explicitly turn this off. */
    fun observeEnabled(): Flow<Boolean>

    suspend fun setEnabled(enabled: Boolean)
}

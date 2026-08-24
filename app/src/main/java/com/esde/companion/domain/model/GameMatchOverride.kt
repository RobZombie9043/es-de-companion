package com.esde.companion.domain.model

/**
 * A user-entered correction for which RetroAchievements game a specific ROM corresponds to,
 * keyed the same way [GameReference] is. Always takes precedence over an automatic title
 * match - see `ResolveRetroAchievementsGameUseCase`. Persisted as a plain, non-secret
 * DataStore list (see CLAUDE.md) and included in Backup & Restore, since it's user-curated
 * library data rather than sensitive.
 */
data class GameMatchOverride(
    val systemShortName: String,
    val romPath: String,
    val raGameId: Long,
)

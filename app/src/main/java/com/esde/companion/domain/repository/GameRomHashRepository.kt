package com.esde.companion.domain.repository

import com.esde.companion.domain.model.GameRomHash

/**
 * Source of a game's ROM hash, parsed from ES-DE's gamelist.xml for the given system - used
 * by [com.esde.companion.domain.usecase.ResolveRetroAchievementsGameUseCase] as the primary,
 * most reliable RetroAchievements match signal, ahead of ES-DE's own display title. How the
 * file is located and parsed (and any caching) is entirely a data-layer concern - see
 * `FileGameRomHashRepository`.
 */
interface GameRomHashRepository {
    suspend fun resolveRomHash(
        systemShortName: String,
        romPath: String,
    ): GameRomHash
}

package com.esde.companion.data.gamelist

import com.esde.companion.data.debug.DebugFileLogger
import com.esde.companion.domain.model.GameRomHash
import com.esde.companion.domain.repository.GameRomHashRepository

/**
 * Wraps a [GameRomHashRepository] to log every ROM-hash resolution outcome via
 * [debugFileLogger] - RetroAchievements matching's data source, same pattern as
 * [LoggingGameDescriptionRepository]. Logged under the "Cheevo" tag (not "Media" - this
 * feeds RetroAchievements matching, not the media widgets' FOUND/NOT FOUND log), the same
 * tag `ResolveRetroAchievementsGameUseCase`'s own resolution-outcome logging uses, so
 * extraction and matching show up together in the debug log. This is also the practical way
 * to check whether hash extraction is working at all before ES-DE ships real hash data -
 * point the app at a hand-edited gamelist.xml and watch this log.
 */
class LoggingGameRomHashRepository(
    private val inner: GameRomHashRepository,
    private val debugFileLogger: DebugFileLogger,
) : GameRomHashRepository {
    override suspend fun resolveRomHash(
        systemShortName: String,
        romPath: String,
    ): GameRomHash {
        val result = inner.resolveRomHash(systemShortName, romPath)
        val status = if (result.value != null) "FOUND" else "NOT FOUND"
        val location = result.gamelistPath ?: "(no gamelist.xml found for $systemShortName)"
        debugFileLogger.logInfo("Cheevo", "ROM Hash $status $systemShortName $location")
        return result
    }
}

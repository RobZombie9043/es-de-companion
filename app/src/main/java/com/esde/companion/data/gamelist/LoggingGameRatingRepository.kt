package com.esde.companion.data.gamelist

import com.esde.companion.data.debug.DebugFileLogger
import com.esde.companion.domain.model.GameRating
import com.esde.companion.domain.repository.GameRatingRepository

/**
 * Wraps a [GameRatingRepository] to log every rating resolution outcome via
 * [debugFileLogger] - the Rating widget's data source, same pattern as
 * LoggingGameDescriptionRepository for game descriptions.
 */
class LoggingGameRatingRepository(
    private val inner: GameRatingRepository,
    private val debugFileLogger: DebugFileLogger,
) : GameRatingRepository {
    override suspend fun resolveRating(
        systemShortName: String,
        romPath: String,
    ): GameRating {
        val result = inner.resolveRating(systemShortName, romPath)
        val status = if (result.value != null) "FOUND" else "NOT FOUND"
        val location = result.gamelistPath ?: "(no gamelist.xml found for $systemShortName)"
        debugFileLogger.logInfo("Media", "Game Rating $status $systemShortName $location")
        return result
    }
}

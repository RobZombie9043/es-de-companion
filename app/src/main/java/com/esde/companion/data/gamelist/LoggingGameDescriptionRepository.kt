package com.esde.companion.data.gamelist

import com.esde.companion.data.debug.DebugFileLogger
import com.esde.companion.domain.model.GameDescription
import com.esde.companion.domain.repository.GameDescriptionRepository

/**
 * Wraps a [GameDescriptionRepository] to log every description resolution outcome via
 * [debugFileLogger] - the Description widget's data source, same pattern as
 * LoggingGameMediaRepository for game media.
 */
class LoggingGameDescriptionRepository(
    private val inner: GameDescriptionRepository,
    private val debugFileLogger: DebugFileLogger,
) : GameDescriptionRepository {
    override suspend fun resolveDescription(
        systemShortName: String,
        romPath: String,
    ): GameDescription {
        val result = inner.resolveDescription(systemShortName, romPath)
        val status = if (result.text != null) "FOUND" else "NOT FOUND"
        debugFileLogger.logInfo("Desc", "$status $systemShortName $romPath")
        return result
    }
}

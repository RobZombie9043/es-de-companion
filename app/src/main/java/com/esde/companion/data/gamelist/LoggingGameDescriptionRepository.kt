package com.esde.companion.data.gamelist

import com.esde.companion.data.debug.DebugFileLogger
import com.esde.companion.domain.model.GameDescription
import com.esde.companion.domain.repository.GameDescriptionRepository

/**
 * Wraps a [GameDescriptionRepository] to log every description resolution outcome via
 * [debugFileLogger] - the Description widget's data source, same pattern as
 * LoggingGameMediaRepository for game media. Logged under the "Media" tag (not a dedicated
 * one) with "Game Description" standing in for a [MediaType][com.esde.companion.domain.model.MediaType]
 * name, so it reads as one more entry in the same FOUND/NOT FOUND media log rather than a
 * separate category.
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
        // The gamelist.xml path, not romPath, is what's actually useful to check by hand
        // here - romPath only tells you which game, not where its description came from.
        val location = result.gamelistPath ?: "(no gamelist.xml found for $systemShortName)"
        debugFileLogger.logInfo("Media", "Game Description $status $systemShortName $location")
        return result
    }
}

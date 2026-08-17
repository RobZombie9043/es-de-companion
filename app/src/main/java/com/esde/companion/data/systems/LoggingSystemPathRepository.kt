package com.esde.companion.data.systems

import com.esde.companion.data.debug.DebugFileLogger
import com.esde.companion.domain.repository.SystemPathRepository

/**
 * Wraps a [SystemPathRepository] to log every resolution outcome via [debugFileLogger] -
 * same pattern as LoggingGameMediaRepository/LoggingGameDescriptionRepository. Logged
 * under the "Media" tag (not a dedicated one), since this is the last-resort tier of the
 * same media-resolution fallback chain those two already report on - see
 * GameMediaPathResolver's kdoc.
 */
class LoggingSystemPathRepository(
    private val inner: SystemPathRepository,
    private val debugFileLogger: DebugFileLogger,
) : SystemPathRepository {
    override suspend fun resolveSystemPath(systemShortName: String): String? {
        val result = inner.resolveSystemPath(systemShortName)
        val status = if (result != null) "FOUND $result" else "NOT FOUND"
        debugFileLogger.logInfo("Media", "System Path (custom_systems/es_systems.xml) $status $systemShortName")
        return result
    }
}

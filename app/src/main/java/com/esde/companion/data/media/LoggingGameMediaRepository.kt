package com.esde.companion.data.media

import com.esde.companion.data.debug.DebugFileLogger
import com.esde.companion.domain.model.GameMedia
import com.esde.companion.domain.model.MediaType
import com.esde.companion.domain.repository.GameMediaRepository

/**
 * Wraps a [GameMediaRepository] to log every media resolution outcome via
 * [debugFileLogger] - lookup in, found/not-found per requested type out. Logging is
 * scoped to exactly the [MediaType]s the caller asked for (see
 * [DebugFileLogger.logMediaResolution]'s kdoc), not every type on disk.
 */
class LoggingGameMediaRepository(
    private val inner: GameMediaRepository,
    private val debugFileLogger: DebugFileLogger,
) : GameMediaRepository {
    override suspend fun resolveMedia(
        systemShortName: String,
        systemPath: String?,
        romPath: String,
        mediaTypes: Set<MediaType>,
    ): GameMedia {
        val result = inner.resolveMedia(systemShortName, systemPath, romPath, mediaTypes)
        debugFileLogger.logMediaResolution(systemShortName, mediaTypes, result)
        return result
    }
}

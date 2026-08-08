package com.esde.companion.domain.repository

import com.esde.companion.domain.model.GameMedia
import com.esde.companion.domain.model.MediaType

/**
 * Source of truth for a single game's resolved media files. Domain only depends on this
 * interface - how media is actually located on disk (extension probing, the configured
 * media root folder) is entirely a data-layer concern.
 *
 * [mediaTypes] scopes the lookup to exactly what the caller needs (e.g. the GameMedia
 * widgets currently placed on a canvas, or the single type a video/manual overlay needs)
 * rather than probing every [MediaType] on every call regardless of relevance.
 */
interface GameMediaRepository {
    suspend fun resolveMedia(
        systemShortName: String,
        romPath: String,
        mediaTypes: Set<MediaType>,
    ): GameMedia
}

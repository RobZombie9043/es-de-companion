package com.esde.companion.domain.repository

import com.esde.companion.domain.model.GameMedia

/**
 * Source of truth for a single game's resolved media files. Domain only depends on this
 * interface - how media is actually located on disk (extension probing, the configured
 * media root folder) is entirely a data-layer concern.
 */
interface GameMediaRepository {
    suspend fun resolveMedia(
        systemShortName: String,
        romPath: String,
    ): GameMedia
}

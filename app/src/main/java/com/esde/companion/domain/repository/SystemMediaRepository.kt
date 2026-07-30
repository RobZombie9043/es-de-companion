package com.esde.companion.domain.repository

import com.esde.companion.domain.model.MediaType

/**
 * Source of a representative image for a whole system, as opposed to [GameMediaRepository]
 * which resolves media for one specific game - "pick a random image of [mediaType] from
 * among this system's games." How the random pick is made (scanning the media folder) is
 * entirely a data-layer concern.
 */
interface SystemMediaRepository {
    suspend fun randomMedia(systemShortName: String, mediaType: MediaType): String?
}
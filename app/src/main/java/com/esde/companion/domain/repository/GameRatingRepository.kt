package com.esde.companion.domain.repository

import com.esde.companion.domain.model.GameRating

/**
 * Source of a game's rating, parsed from ES-DE's gamelist.xml for the given system. How
 * the file is located and parsed (and any caching) is entirely a data-layer concern - see
 * FileGameRatingRepository. Same shape as GameDescriptionRepository.
 */
interface GameRatingRepository {
    suspend fun resolveRating(
        systemShortName: String,
        romPath: String,
    ): GameRating
}

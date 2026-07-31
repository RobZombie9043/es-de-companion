package com.esde.companion.domain.repository

import com.esde.companion.domain.model.GameDescription

/**
 * Source of a game's description text, parsed from ES-DE's gamelist.xml for the given
 * system. How the file is located and parsed (and any caching) is entirely a data-layer
 * concern - see FileGameDescriptionRepository.
 */
interface GameDescriptionRepository {
    suspend fun resolveDescription(systemShortName: String, romPath: String): GameDescription
}
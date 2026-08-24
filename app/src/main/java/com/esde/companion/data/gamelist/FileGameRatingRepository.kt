package com.esde.companion.data.gamelist

import com.esde.companion.domain.model.GameRating
import com.esde.companion.domain.parser.GameListParser
import com.esde.companion.domain.repository.GameRatingRepository

/**
 * Parses a game's <rating> out of whatever gamelist.xml content [reader] hands back - see
 * [GamelistFileReader] for how the file is located, cached, and kept reactive to the
 * configured ES-DE root. Mirrors [FileGameDescriptionRepository]'s shape exactly, sharing
 * the same reader instance (see `AppContainer`) rather than each maintaining its own file
 * cache.
 */
class FileGameRatingRepository(
    private val reader: GamelistFileReader,
) : GameRatingRepository {
    override suspend fun resolveRating(
        systemShortName: String,
        romPath: String,
    ): GameRating {
        val file = reader.read(systemShortName, romPath) ?: return GameRating(value = null)
        val value = GameListParser.findRating(file.content, romPath)
        return GameRating(value = value, gamelistPath = file.path)
    }
}

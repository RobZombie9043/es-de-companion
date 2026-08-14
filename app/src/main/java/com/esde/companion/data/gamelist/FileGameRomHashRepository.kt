package com.esde.companion.data.gamelist

import com.esde.companion.domain.model.GameRomHash
import com.esde.companion.domain.parser.GameListParser
import com.esde.companion.domain.repository.GameRomHashRepository

/**
 * Parses a game's ROM hash out of whatever gamelist.xml content [reader] hands back - see
 * [GamelistFileReader] for how the file is located, cached, and kept reactive to the
 * configured ES-DE root. Mirrors [FileGameDescriptionRepository]'s shape exactly, sharing
 * the same reader instance (see `AppContainer`) rather than each maintaining its own file
 * cache.
 */
class FileGameRomHashRepository(
    private val reader: GamelistFileReader,
) : GameRomHashRepository {
    override suspend fun resolveRomHash(
        systemShortName: String,
        romPath: String,
    ): GameRomHash {
        val file = reader.read(systemShortName, romPath) ?: return GameRomHash(value = null)
        val hash = GameListParser.findRomHash(file.content, romPath)
        return GameRomHash(value = hash, gamelistPath = file.path)
    }
}

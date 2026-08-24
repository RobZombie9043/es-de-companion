package com.esde.companion.data.gamelist

import com.esde.companion.domain.model.GameDescription
import com.esde.companion.domain.parser.GameListParser
import com.esde.companion.domain.repository.GameDescriptionRepository

/**
 * Parses a game's <desc> out of whatever gamelist.xml content [reader] hands back - see
 * [GamelistFileReader] for how the file is located, cached, and kept reactive to the
 * configured ES-DE root.
 */
class FileGameDescriptionRepository(
    private val reader: GamelistFileReader,
) : GameDescriptionRepository {
    override suspend fun resolveDescription(
        systemShortName: String,
        romPath: String,
    ): GameDescription {
        val file = reader.read(systemShortName, romPath) ?: return GameDescription(text = null)
        val text = GameListParser.findDescription(file.content, romPath)
        return GameDescription(text = text, gamelistPath = file.path)
    }
}

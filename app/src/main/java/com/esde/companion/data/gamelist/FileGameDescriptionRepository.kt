package com.esde.companion.data.gamelist

import com.esde.companion.domain.model.GameDescription
import com.esde.companion.domain.parser.GameListParser
import com.esde.companion.domain.repository.GameDescriptionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Parses a game's <desc> out of whatever gamelist.xml content [reader] hands back - see
 * [GamelistFileReader] for how the file is located, cached, and kept reactive to the
 * configured ES-DE root.
 *
 * [GameListParser.findDescription] does a full DOM parse of the *entire* gamelist.xml plus
 * a linear scan for the matching `<game>` - O(number of games in that system), confirmed
 * on-device to take long enough on large gamelists to visibly stutter a concurrently-
 * running widget-canvas animation. [reader]'s own `withContext(Dispatchers.IO)` only covers
 * the file read; without this repository's own `withContext(Dispatchers.Default)` around
 * the CPU-bound parse itself, execution resumes on whatever dispatcher called
 * [resolveDescription] (in practice, the main thread) the moment the read completes.
 */
class FileGameDescriptionRepository(
    private val reader: GamelistFileReader,
) : GameDescriptionRepository {
    override suspend fun resolveDescription(
        systemShortName: String,
        romPath: String,
    ): GameDescription {
        val file = reader.read(systemShortName, romPath) ?: return GameDescription(text = null)
        val text = withContext(Dispatchers.Default) { GameListParser.findDescription(file.content, romPath) }
        return GameDescription(text = text, gamelistPath = file.path)
    }
}
